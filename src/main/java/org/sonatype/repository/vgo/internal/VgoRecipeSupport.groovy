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
package org.sonatype.repository.vgo.internal

import javax.inject.Inject
import javax.inject.Provider

import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.RecipeSupport
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.attributes.AttributesFacet
import org.sonatype.nexus.repository.cache.NegativeCacheFacet
import org.sonatype.nexus.repository.cache.NegativeCacheHandler
import org.sonatype.nexus.repository.http.PartialFetchHandler
import org.sonatype.nexus.repository.httpclient.HttpClientFacet
import org.sonatype.nexus.repository.purge.PurgeUnusedFacet
import org.sonatype.nexus.repository.search.SearchFacet
import org.sonatype.nexus.repository.security.SecurityHandler
import org.sonatype.nexus.repository.storage.DefaultComponentMaintenanceImpl
import org.sonatype.nexus.repository.storage.StorageFacet
import org.sonatype.nexus.repository.storage.UnitOfWorkHandler
import org.sonatype.nexus.repository.view.ConfigurableViewFacet
import org.sonatype.nexus.repository.view.Context
import org.sonatype.nexus.repository.view.Matcher
import org.sonatype.nexus.repository.view.handlers.BrowseUnsupportedHandler
import org.sonatype.nexus.repository.view.handlers.ConditionalRequestHandler
import org.sonatype.nexus.repository.view.handlers.ContentHeadersHandler
import org.sonatype.nexus.repository.view.handlers.ExceptionHandler
import org.sonatype.nexus.repository.view.handlers.HandlerContributor
import org.sonatype.nexus.repository.view.handlers.LastDownloadedHandler
import org.sonatype.nexus.repository.view.handlers.TimingHandler
import org.sonatype.nexus.repository.view.matchers.ActionMatcher
import org.sonatype.nexus.repository.view.matchers.logic.LogicMatchers
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher
import org.sonatype.repository.vgo.VgoAssetKind
import org.sonatype.repository.vgo.internal.security.VgoSecurityFacet

import static org.sonatype.nexus.repository.http.HttpMethods.GET
import static org.sonatype.nexus.repository.http.HttpMethods.HEAD
import static org.sonatype.nexus.repository.http.HttpMethods.PUT
import static org.sonatype.repository.vgo.VgoAssetKind.VGO_INFO
import static org.sonatype.repository.vgo.VgoAssetKind.VGO_LIST
import static org.sonatype.repository.vgo.VgoAssetKind.VGO_MODULE
import static org.sonatype.repository.vgo.VgoAssetKind.VGO_PACKAGE

/**
 * Support for Vgo recipes.
 */
abstract class VgoRecipeSupport
    extends RecipeSupport
{
  @Inject
  Provider<VgoSecurityFacet> securityFacet

  @Inject
  Provider<ConfigurableViewFacet> viewFacet

  @Inject
  Provider<StorageFacet> storageFacet

  @Inject
  Provider<SearchFacet> searchFacet

  @Inject
  Provider<AttributesFacet> attributesFacet

  @Inject
  ExceptionHandler exceptionHandler

  @Inject
  TimingHandler timingHandler

  @Inject
  SecurityHandler securityHandler

  @Inject
  PartialFetchHandler partialFetchHandler

  @Inject
  ConditionalRequestHandler conditionalRequestHandler

  @Inject
  ContentHeadersHandler contentHeadersHandler

  @Inject
  UnitOfWorkHandler unitOfWorkHandler

  @Inject
  BrowseUnsupportedHandler browseUnsupportedHandler

  @Inject
  HandlerContributor handlerContributor

  @Inject
  Provider<DefaultComponentMaintenanceImpl> componentMaintenanceFacet

  @Inject
  Provider<HttpClientFacet> httpClientFacet

  @Inject
  Provider<PurgeUnusedFacet> purgeUnusedFacet

  @Inject
  Provider<NegativeCacheFacet> negativeCacheFacet

  @Inject
  NegativeCacheHandler negativeCacheHandler

  @Inject
  LastDownloadedHandler lastDownloadedHandler

  protected VgoRecipeSupport(final Type type, final Format format) {
    super(type, format)
  }

  /**
   * Matcher for .zip files
   */
  static Matcher packageMatcher() {
    createMatcher(VGO_PACKAGE, 'zip')
  }

  /**
   * Matcher for .info files
   */
  static Matcher infoMatcher() {
    createMatcher(VGO_INFO, 'info')
  }

  /**
   * Matcher for .mod files
   */
  static Matcher moduleMatcher() {
    createMatcher(VGO_MODULE, 'mod')
  }

  static Matcher uploadMatcher() {
    LogicMatchers.and(
        new ActionMatcher(PUT),
        tokenMatcherForExtension('zip'),
        new Matcher() {
          @Override
          boolean matches(final Context context) {
            context.attributes.set(VgoAssetKind.class, VGO_PACKAGE)
            return true
          }
        }
    )
  }
  static Matcher listMatcher() {
    LogicMatchers.and(
        new ActionMatcher(GET, HEAD),
        new TokenMatcher("/{module:.+}/@v/list"),
        new Matcher() {
          @Override
          boolean matches(final Context context) {
            context.attributes.set(VgoAssetKind.class, VGO_LIST)
            return true
          }
        }
    )
  }

  static Matcher createMatcher(final VgoAssetKind assetKind, final String extension) {
    LogicMatchers.and(
        new ActionMatcher(GET, HEAD),
        tokenMatcherForExtension(extension),
        new Matcher() {
          @Override
          boolean matches(final Context context) {
            context.attributes.set(VgoAssetKind.class, assetKind)
            return true
          }
        }
    )
  }

  static TokenMatcher tokenMatcherForExtension(final String extension) {
    new TokenMatcher("/{module:.+}/@v/{version:.+}.{extension:${extension}}")
  }
}
