/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.spark.deploy.kubernetes.docker.gradle;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.gradle.api.provider.Property;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

public final class GenerateDockerFileTaskSuite {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  private File destDockerFile;

  @Before
  public void before() throws IOException {
    File dockerFileDir = tempFolder.newFolder("docker");
    destDockerFile = new File(dockerFileDir, "Dockerfile");
  }

  @Test
  public void testGenerateDockerFile() throws IOException {
    GenerateDockerFileTask task = Mockito.mock(GenerateDockerFileTask.class);
    Mockito.when(task.getDestDockerFile()).thenReturn(destDockerFile);
    Property<String> baseImageProperty = Mockito.mock(Property.class);
    Mockito.when(baseImageProperty.get()).thenReturn("fabric8/java-centos-openjdk8-jdk:latest");
    Mockito.when(task.getBaseImage()).thenReturn(baseImageProperty);
    Mockito.doCallRealMethod().when(task).generateDockerFile();
    task.generateDockerFile();
    Assertions.assertThat(destDockerFile).isFile();
    List<String> writtenLines = Files.readAllLines(
        destDockerFile.toPath(), StandardCharsets.UTF_8);
    try (InputStream expectedDockerFileInput =
        getClass().getResourceAsStream("/ExpectedDockerfile");
        InputStreamReader expectedDockerFileReader =
            new InputStreamReader(expectedDockerFileInput, StandardCharsets.UTF_8);
         BufferedReader expectedDockerFileBuffered =
            new BufferedReader(expectedDockerFileReader)) {
      List<String> expectedFileLines = expectedDockerFileBuffered
          .lines()
          .collect(Collectors.toList());
      Assertions.assertThat(writtenLines).isEqualTo(expectedFileLines);
    }
  }

}
