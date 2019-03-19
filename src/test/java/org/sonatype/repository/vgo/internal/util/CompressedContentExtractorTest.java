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
package org.sonatype.repository.vgo.internal.util;

import java.io.InputStream;

import com.google.common.base.Charsets;
import com.google.common.io.ByteSource;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class CompressedContentExtractorTest
{
  @Test
  public void canExtractFile() throws Exception {
    InputStream project = getClass().getResourceAsStream("sonatype.zip");
    InputStream goMod = getClass().getResourceAsStream("go.mod");

    InputStream result = CompressedContentExtractor.extractFile(project, "go.mod");

    String goModFromProject = new ByteSource()
    {
      @Override
      public InputStream openStream() {
        return result;
      }
    }.asCharSource(Charsets.UTF_8).read();

    String goModAsString = new ByteSource()
    {
      @Override
      public InputStream openStream() {
        return goMod;
      }
    }.asCharSource(Charsets.UTF_8).read();

    assertThat(goModFromProject, is(equalTo(goModAsString)));
  }

  @Test
  public void entryNotFound() {
    InputStream project = getClass().getResourceAsStream("sonatype.zip");

    InputStream response = CompressedContentExtractor.extractFile(project, "does_not_exist.txt");

    assertThat(response, is(nullValue()));
  }
}
