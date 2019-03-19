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
package org.sonatype.repository.vgo.internal.proxy;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.proxy.ProxyFacet;
import org.sonatype.nexus.repository.proxy.ProxyFacetSupport;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchMetadata;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;
import org.sonatype.nexus.transaction.UnitOfWork;
import org.sonatype.repository.vgo.VgoAssetKind;
import org.sonatype.repository.vgo.internal.metadata.VgoAttributes;
import org.sonatype.repository.vgo.internal.util.VgoDataAccess;
import org.sonatype.repository.vgo.internal.util.VgoPathUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.repository.vgo.internal.util.VgoDataAccess.HASH_ALGORITHMS;

/**
 * Vgo {@link ProxyFacet} implementation.
 *
 * @since 0.0.1
 */
@Named
public class VgoProxyFacetImpl
    extends ProxyFacetSupport
{
  private final VgoPathUtils vgoPathUtils;

  private final VgoDataAccess vgoDataAccess;

  @Inject
  public VgoProxyFacetImpl(final VgoPathUtils vgoPathUtils,
                           final VgoDataAccess vgoDataAccess)
  {
    this.vgoPathUtils = checkNotNull(vgoPathUtils);
    this.vgoDataAccess = checkNotNull(vgoDataAccess);
  }

  // HACK: Workaround for known CGLIB issue, forces an Import-Package for org.sonatype.nexus.repository.config
  @Override
  protected void doValidate(final Configuration configuration) throws Exception {
    super.doValidate(configuration);
  }

  @Nullable
  @Override
  protected Content getCachedContent(final Context context) throws IOException {
    VgoAssetKind assetKind = context.getAttributes().require(VgoAssetKind.class);
    TokenMatcher.State matcherState = vgoPathUtils.matcherState(context);
    switch (assetKind) {
      case VGO_INFO:
      case VGO_MODULE:
      case VGO_PACKAGE:
        return getAsset(vgoPathUtils.assetPath(matcherState));
      case VGO_LIST:
        return getAsset(vgoPathUtils.listPath(matcherState));
      default:
        throw new IllegalStateException("Received an invalid VgoAssetKind of type: " + assetKind.name());
    }
  }

  @Override
  protected Content store(final Context context, final Content content) throws IOException {
    VgoAssetKind assetKind = context.getAttributes().require(VgoAssetKind.class);
    TokenMatcher.State matcherState = vgoPathUtils.matcherState(context);
    VgoAttributes vgoAttributes = vgoPathUtils.getAttributesFromMatcherState(matcherState);
    switch(assetKind) {
      case VGO_INFO:
      case VGO_MODULE:
      case VGO_PACKAGE:
        return putComponent(vgoAttributes, content, vgoPathUtils.assetPath(matcherState), assetKind);
      case VGO_LIST:
        return putComponent(vgoAttributes, content, vgoPathUtils.listPath(matcherState), assetKind);
      default:
        throw new IllegalStateException("Received an invalid VgoAssetKind of type: " + assetKind.name());
    }
  }

  @TransactionalTouchBlob
  protected Content getAsset(final String path) {
    StorageTx tx = UnitOfWork.currentTx();

    Asset asset = vgoDataAccess.findAsset(tx, tx.findBucket(getRepository()), path);
    if (asset == null) {
      return null;
    }

    return vgoDataAccess.toContent(asset, tx.requireBlob(asset.requireBlobRef()));
  }

  private Content putComponent(final VgoAttributes vgoAttributes,
                               final Content content,
                               final String assetPath,
                               final VgoAssetKind assetKind) throws IOException {
    StorageFacet storageFacet = facet(StorageFacet.class);
    try (TempBlob tempBlob = storageFacet.createTempBlob(content.openInputStream(), HASH_ALGORITHMS)) {
      return vgoDataAccess.doCreateOrSaveComponent(getRepository(),
          vgoAttributes,
          assetPath,
          tempBlob,
          content,
          assetKind);
    }
  }

  @Override
  protected void indicateVerified(final Context context, final Content content, final CacheInfo cacheInfo)
      throws IOException
  {
    setCacheInfo(content, cacheInfo);
  }

  @TransactionalTouchMetadata
  public void setCacheInfo(final Content content, final CacheInfo cacheInfo) throws IOException {
    StorageTx tx = UnitOfWork.currentTx();
    Asset asset = Content.findAsset(tx, tx.findBucket(getRepository()), content);
    if (asset == null) {
      log.debug(
          "Attempting to set cache info for non-existent vgo asset {}", content.getAttributes().require(Asset.class)
      );
      return;
    }
    log.debug("Updating cacheInfo of {} to {}", asset, cacheInfo);
    CacheInfo.applyToAsset(asset, cacheInfo);
    tx.saveAsset(asset);
  }

  @Override
  protected String getUrl(@Nonnull final Context context) {
    return context.getRequest().getPath().substring(1);
  }

}
