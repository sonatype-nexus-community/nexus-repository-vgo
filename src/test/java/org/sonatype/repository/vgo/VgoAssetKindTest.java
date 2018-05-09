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
package org.sonatype.repository.vgo;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.cache.CacheControllerHolder;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class VgoAssetKindTest
    extends TestSupport
{
  private VgoAssetKind VGO_PACKAGE;

  private VgoAssetKind VGO_MODULE;

  private VgoAssetKind VGO_INFO;

  @Before
  public void setUp() throws Exception {
    VGO_PACKAGE = VgoAssetKind.VGO_PACKAGE;
    VGO_MODULE = VgoAssetKind.VGO_MODULE;
    VGO_INFO = VgoAssetKind.VGO_INFO;
  }

  @Test
  public void getCacheType() {
    assertThat(VGO_PACKAGE.getCacheType(), is(equalTo(CacheControllerHolder.CONTENT)));
    assertThat(VGO_MODULE.getCacheType(), is(equalTo(CacheControllerHolder.METADATA)));
    assertThat(VGO_INFO.getCacheType(), is(equalTo(CacheControllerHolder.METADATA)));
  }
}