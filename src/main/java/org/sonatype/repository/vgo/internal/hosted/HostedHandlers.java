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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher.State;
import org.sonatype.repository.vgo.VgoAssetKind;
import org.sonatype.repository.vgo.internal.metadata.VgoAttributes;
import org.sonatype.repository.vgo.internal.util.VgoPathUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.http.HttpResponses.notFound;
import static org.sonatype.nexus.repository.http.HttpResponses.ok;

@Named
@Singleton
public class HostedHandlers
    extends ComponentSupport
{
  private VgoPathUtils pathUtils;

  @Inject
  public HostedHandlers(final VgoPathUtils pathUtils) { this.pathUtils = checkNotNull(pathUtils); }

  final Handler get = context -> {
    State state = context.getAttributes().require(TokenMatcher.State.class);
    String path = pathUtils.assetPath(state);
    VgoAttributes vgoAttributes = pathUtils.getAttributesFromMatcherState(state);

    VgoAssetKind assetKind = context.getAttributes().require(VgoAssetKind.class);
    Content content;
    switch (assetKind) {
      case VGO_INFO:
        content = context.getRepository().facet(VgoHostedFacet.class).getInfo(path, vgoAttributes);
        break;
      case VGO_PACKAGE:
        content = context.getRepository().facet(VgoHostedFacet.class).get(path);
        break;
      default:
        return notFound();
    }

    return (content != null) ? ok(content) : notFound();
  };

  final Handler upload = context -> {
    State state = context.getAttributes().require(TokenMatcher.State.class);
    String path = pathUtils.assetPath(state);
    VgoAttributes vgoAttributes = pathUtils.getAttributesFromMatcherState(state);

    VgoAssetKind assetKind = context.getAttributes().require(VgoAssetKind.class);
    context.getRepository().facet(VgoHostedFacet.class).upload(path, vgoAttributes, context.getRequest().getPayload(), assetKind);

    return ok();
  };
}
