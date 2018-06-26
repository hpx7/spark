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

import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.File;

public class DockerBuildTask extends DefaultTask  {

  private File dockerFile;
  private File dockerBuildDirectory;
  private Property<String> imageName;

  @InputFile
  public File getDockerFile() {
    return dockerFile;
  }

  @InputDirectory
  public File getDockerBuildDirectory() {
    return dockerBuildDirectory;
  }

  @Input
  public Property<String> getImageName() {
    return imageName;
  }

  public void setDockerFile(File dockerFile) {
    this.dockerFile = dockerFile;
  }

  public void setDockerBuildDirectory(File dockerBuildDirectory) {
    this.dockerBuildDirectory = dockerBuildDirectory;
  }

  public void setImageName(Property<String> imageName) {
    this.imageName = imageName;
  }

  @TaskAction
  public void exec() {
    getProject().exec(execSpec -> execSpec.commandLine(
        "docker",
        "build",
        "-f",
        getDockerFile().getAbsolutePath(),
        "-t",
        getImageName().get(),
        getDockerBuildDirectory().getAbsolutePath()));
  }

}
