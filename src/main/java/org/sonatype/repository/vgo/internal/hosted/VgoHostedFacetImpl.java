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

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.transaction.UnitOfWork;
import org.sonatype.repository.vgo.VgoAssetKind;
import org.sonatype.repository.vgo.internal.util.VgoDataAccess;

import com.google.common.base.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

@Named
public class VgoHostedFacetImpl
    extends FacetSupport
  implements VgoHostedFacet
{
  private final VgoDataAccess vgoDataAccess;

  @Override
  protected void doInit(final Configuration configuration) throws Exception {
    super.doInit(configuration);
  }

  @Inject
  public VgoHostedFacetImpl(final VgoDataAccess vgoDataAccess) {
    this.vgoDataAccess = checkNotNull(vgoDataAccess);
  }

  @Nullable
  @TransactionalTouchBlob
  @Override
  public Content get(final String path) {
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
  public void upload(final String path, final Payload payload, final VgoAssetKind assetKind) throws IOException {
    checkNotNull(path);
    checkNotNull(payload);

    if (assetKind != VgoAssetKind.VGO_PACKAGE) {
      throw new IllegalArgumentException("Unsupported AssetKind");
    }
  }

  @TransactionalStoreBlob
  protected Content storeModule(final String path,
                                final Supplier<InputStream> moduleContent,
                                final Payload payload) throws IOException
  {
    StorageTx tx = UnitOfWork.currentTx();

    Asset asset = createModuleAsset(path, tx, tx.findBucket(getRepository()), moduleContent.get());

    return vgoDataAccess.saveAsset(tx, asset, moduleContent, payload);
  }

  private Asset createModuleAsset(final String path,
                                  final StorageTx tx,
                                  final Bucket bucket,
                                  final InputStream inputStream) throws IOException
  {

  }
}
