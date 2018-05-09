package org.sonatype.repository.vgo;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.cache.CacheControllerHolder;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class AssetKindTest
    extends TestSupport
{
  private AssetKind VGO_PACKAGE;

  private AssetKind VGO_MODULE;

  private AssetKind VGO_INFO;

  @Before
  public void setUp() throws Exception {
    VGO_PACKAGE = AssetKind.VGO_PACKAGE;
    VGO_MODULE = AssetKind.VGO_MODULE;
    VGO_INFO = AssetKind.VGO_INFO;
  }

  @Test
  public void getCacheType() {
    assertThat(VGO_PACKAGE.getCacheType(), is(equalTo(CacheControllerHolder.CONTENT)));
    assertThat(VGO_MODULE.getCacheType(), is(equalTo(CacheControllerHolder.METADATA)));
    assertThat(VGO_INFO.getCacheType(), is(equalTo(CacheControllerHolder.METADATA)));
  }
}