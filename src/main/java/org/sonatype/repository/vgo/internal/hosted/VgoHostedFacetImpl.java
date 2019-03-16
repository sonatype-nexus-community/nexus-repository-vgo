/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2018-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.repository.vgo.internal.hosted;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.ContentTypes;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.BlobPayload;
import org.sonatype.nexus.repository.view.payloads.StreamPayload;
import org.sonatype.nexus.repository.view.payloads.StreamPayload.InputStreamSupplier;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;
import org.sonatype.repository.vgo.VgoAssetKind;
import org.sonatype.repository.vgo.internal.metadata.VgoAttributes;
import org.sonatype.repository.vgo.internal.metadata.VgoInfo;
import org.sonatype.repository.vgo.internal.util.CompressedContentExtractor;
import org.sonatype.repository.vgo.internal.util.VgoDataAccess;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.impl.io.EmptyInputStream;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.view.Payload.UNKNOWN_SIZE;
import static org.sonatype.repository.vgo.VgoAssetKind.VGO_MODULE;
import static org.sonatype.repository.vgo.internal.util.VgoDataAccess.HASH_ALGORITHMS;

@Named
public class VgoHostedFacetImpl
    extends FacetSupport
  implements VgoHostedFacet
{
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final VgoDataAccess vgoDataAccess;

  @Override
  protected void doInit(final Configuration configuration) throws Exception {
    super.doInit(configuration);
  }

  @Inject
  public VgoHostedFacetImpl(final VgoDataAccess vgoDataAccess) {
    this.vgoDataAccess = checkNotNull(vgoDataAccess);
  }

  @Override
  @Nullable
  @Transactional
  public Content getInfo(final String path,
                         final VgoAttributes vgoAttributes) {
    checkNotNull(path);
    String newPath = path.replaceAll("\\.info", "\\.zip");

    StorageTx tx = UnitOfWork.currentTx();

    Asset asset = vgoDataAccess.findAsset(tx, tx.findBucket(getRepository()), newPath);
    if (asset == null) {
      return null;
    }

    StreamPayload streamPayload = new StreamPayload(
        () -> doGetInfo(asset, vgoAttributes),
        UNKNOWN_SIZE,
        ContentTypes.APPLICATION_JSON);
    return new Content(streamPayload);
  }

  @Transactional
  @Override
  public Content getList(final String module) {
    checkNotNull(module);

    StorageTx tx = UnitOfWork.currentTx();

    Iterable<Asset> assetsForModule = vgoDataAccess.findPackageAssetsForModule(tx, getRepository(), module);

    String listOfVersions = StreamSupport.stream(assetsForModule.spliterator(), false)
        .map(asset -> asset.name())
        .map(name -> name.split("/@v/")[1])
        .map((name -> name.replaceAll(".zip", "")))
        .collect(Collectors.joining("\n"));

    return new Content(
      new StreamPayload(
          new InputStreamSupplier() {
            @Nonnull
            @Override
            public InputStream get() {
              return new ByteArrayInputStream(listOfVersions.getBytes());
            }
          },
          UNKNOWN_SIZE,
          ContentTypes.TEXT_PLAIN
      )
    );
  }

  private InputStream doGetInfo(final Asset asset, final VgoAttributes vgoAttributes) {
    VgoInfo vgoInfo = new VgoInfo(vgoAttributes.getVersion(), asset.blobCreated().toString());
    try {
      String info = MAPPER.writeValueAsString(vgoInfo);
      return new ByteArrayInputStream(info.getBytes());
    }
    catch (JsonProcessingException e) {
      log.warn(String.format("Unable to convert %s to json", vgoInfo.toString()));
    }
    return EmptyInputStream.INSTANCE;
  }

  @Nullable
  @TransactionalTouchBlob
  @Override
  public Content getPackage(final String path) {
    return doGet(path);
  }

  @Nullable
  @TransactionalTouchBlob
  @Override
  public Content getMod(final String path) {
    return doGet(path);
  }

  private Content doGet(final String path) {
    checkNotNull(path);
    StorageTx tx = UnitOfWork.currentTx();

    Asset asset = vgoDataAccess.findAsset(tx, tx.findBucket(getRepository()), path);
    if (asset == null) {
      return null;
    }

    if (asset.markAsDownloaded()) {
      tx.saveAsset(asset);
    }

    return vgoDataAccess.toContent(asset, tx.requireBlob(asset.requireBlobRef()));
  }

  @Override
  public void upload(final String path,
                     final VgoAttributes vgoAttributes,
                     final Payload payload,
                     final VgoAssetKind assetKind) throws IOException {
    checkNotNull(vgoAttributes);
    checkNotNull(payload);

    if (assetKind != VgoAssetKind.VGO_PACKAGE) {
      throw new IllegalArgumentException("Unsupported AssetKind");
    }

    storeContent(path, vgoAttributes, payload, assetKind);
  }

  private void storeContent(final String path,
                            final VgoAttributes vgoAttributes,
                            final Payload payload,
                            final VgoAssetKind assetKind) throws IOException
  {
    StorageFacet storageFacet = facet(StorageFacet.class);
    try (TempBlob tempBlob = storageFacet.createTempBlob(payload.openInputStream(), HASH_ALGORITHMS)) {
      vgoDataAccess.doCreateOrSaveComponent(getRepository(), vgoAttributes, path, tempBlob, payload, assetKind);
    }

    extractAndSaveMod(path, vgoAttributes);
  }

  private void extractAndSaveMod(final String path, final VgoAttributes vgoAttributes) {
    Payload content = getZip(path);

    if (content != null) {
      try (InputStream contentStream = content.openInputStream()){
        InputStream goModStream = CompressedContentExtractor.extractFile(contentStream, "go.mod");

        if (goModStream != null) {
          storeContent(path.replaceAll("\\.zip", "\\.mod"),
              vgoAttributes,
              new StreamPayload(new InputStreamSupplier()
              {
                @Nonnull
                @Override
                public InputStream get() {
                  return goModStream;
                }
              }, UNKNOWN_SIZE, ContentTypes.TEXT_PLAIN),
              VGO_MODULE);
        }
      }
      catch (IOException e) {
        log.warn(String.format("Unable to open content %s", path), e);
      }
    }
  }

  @TransactionalTouchBlob
  protected Payload getZip(final String path) {
    StorageTx tx = UnitOfWork.currentTx();

    Asset asset = vgoDataAccess.findAsset(tx, tx.findBucket(getRepository()), path);
    if (asset == null) {
      log.warn("Unable to find %s for extraction", path);
      return null;
    }
    return new BlobPayload(tx.requireBlob(asset.requireBlobRef()), asset.requireContentType());
  }
}
