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
package org.apache.spark.deploy.k8s.submit

import java.io.File

import com.google.common.base.Charsets
import com.google.common.io.Files

import org.apache.spark.{SparkConf, SparkFunSuite}
import org.apache.spark.deploy.k8s._
import org.apache.spark.deploy.k8s.features._
import org.apache.spark.deploy.k8s.features.bindings.{JavaDriverFeatureStep, PythonDriverFeatureStep}
import org.apache.spark.util.Utils

class KubernetesDriverBuilderSuite extends SparkFunSuite {

  private val BASIC_STEP_TYPE = "basic"
  private val CREDENTIALS_STEP_TYPE = "credentials"
  private val SERVICE_STEP_TYPE = "service"
  private val LOCAL_DIRS_STEP_TYPE = "local-dirs"
  private val SECRETS_STEP_TYPE = "mount-secrets"
  private val MOUNT_LOCAL_FILES_STEP_TYPE = "mount-local-files"
  private val JAVA_STEP_TYPE = "java-bindings"
  private val PYSPARK_STEP_TYPE = "pyspark-bindings"
  private val ENV_SECRETS_STEP_TYPE = "env-secrets"
  private val MOUNT_VOLUMES_STEP_TYPE = "mount-volumes"

  private val basicFeatureStep = KubernetesFeaturesTestUtils.getMockConfigStepForStepType(
    BASIC_STEP_TYPE, classOf[BasicDriverFeatureStep])

  private val credentialsStep = KubernetesFeaturesTestUtils.getMockConfigStepForStepType(
    CREDENTIALS_STEP_TYPE, classOf[DriverKubernetesCredentialsFeatureStep])

  private val serviceStep = KubernetesFeaturesTestUtils.getMockConfigStepForStepType(
    SERVICE_STEP_TYPE, classOf[DriverServiceFeatureStep])

  private val localDirsStep = KubernetesFeaturesTestUtils.getMockConfigStepForStepType(
    LOCAL_DIRS_STEP_TYPE, classOf[LocalDirsFeatureStep])

  private val secretsStep = KubernetesFeaturesTestUtils.getMockConfigStepForStepType(
    SECRETS_STEP_TYPE, classOf[MountSecretsFeatureStep])

  private val mountLocalFilesStep = KubernetesFeaturesTestUtils.getMockConfigStepForStepType(
    MOUNT_LOCAL_FILES_STEP_TYPE, classOf[MountLocalFilesFeatureStep])

  private val javaStep = KubernetesFeaturesTestUtils.getMockConfigStepForStepType(
    JAVA_STEP_TYPE, classOf[JavaDriverFeatureStep])

  private val pythonStep = KubernetesFeaturesTestUtils.getMockConfigStepForStepType(
    PYSPARK_STEP_TYPE, classOf[PythonDriverFeatureStep])

  private val envSecretsStep = KubernetesFeaturesTestUtils.getMockConfigStepForStepType(
    ENV_SECRETS_STEP_TYPE, classOf[EnvSecretsFeatureStep])

  private val mountVolumesStep = KubernetesFeaturesTestUtils.getMockConfigStepForStepType(
    MOUNT_VOLUMES_STEP_TYPE, classOf[MountVolumesFeatureStep])

  private val builderUnderTest: KubernetesDriverBuilder =
    new KubernetesDriverBuilder(
      _ => basicFeatureStep,
      _ => credentialsStep,
      _ => serviceStep,
      _ => secretsStep,
      _ => envSecretsStep,
      _ => localDirsStep,
      _ => mountLocalFilesStep,
      _ => mountVolumesStep,
      _ => javaStep,
      _ => pythonStep)

  test("Apply fundamental steps all the time.") {
    val conf = KubernetesConf(
      new SparkConf(false),
      KubernetesDriverSpecificConf(
        Some(JavaMainAppResource("example.jar")),
        "test-app",
        "main",
        Seq.empty),
      "prefix",
      "appId",
      Some("secret"),
      Map.empty,
      Map.empty,
      Map.empty,
      Map.empty,
      Map.empty,
      Nil,
      Seq.empty[String])
    validateStepTypesApplied(
      builderUnderTest.buildFromFeatures(conf),
      BASIC_STEP_TYPE,
      CREDENTIALS_STEP_TYPE,
      SERVICE_STEP_TYPE,
      LOCAL_DIRS_STEP_TYPE,
      JAVA_STEP_TYPE)
  }

  test("Apply secrets step if secrets are present.") {
    val conf = KubernetesConf(
      new SparkConf(false),
      KubernetesDriverSpecificConf(
        None,
        "test-app",
        "main",
        Seq.empty),
      "prefix",
      "appId",
      Some("secret"),
      Map.empty,
      Map.empty,
      Map("secret" -> "secretMountPath"),
      Map("EnvName" -> "SecretName:secretKey"),
      Map.empty,
      Nil,
      Seq.empty[String])
    validateStepTypesApplied(
      builderUnderTest.buildFromFeatures(conf),
      BASIC_STEP_TYPE,
      CREDENTIALS_STEP_TYPE,
      SERVICE_STEP_TYPE,
      LOCAL_DIRS_STEP_TYPE,
      SECRETS_STEP_TYPE,
      ENV_SECRETS_STEP_TYPE,
      JAVA_STEP_TYPE)
  }

  test("Apply Java step if main resource is none.") {
    val conf = KubernetesConf(
      new SparkConf(false),
      KubernetesDriverSpecificConf(
        None,
        "test-app",
        "main",
        Seq.empty),
      "prefix",
      "appId",
      None,
      Map.empty,
      Map.empty,
      Map.empty,
      Map.empty,
      Map.empty,
      Nil,
      Seq.empty[String])
    validateStepTypesApplied(
      builderUnderTest.buildFromFeatures(conf),
      BASIC_STEP_TYPE,
      CREDENTIALS_STEP_TYPE,
      SERVICE_STEP_TYPE,
      LOCAL_DIRS_STEP_TYPE,
      JAVA_STEP_TYPE)
  }

  test("Apply Python step if main resource is python.") {
    val conf = KubernetesConf(
      new SparkConf(false),
      KubernetesDriverSpecificConf(
        Some(PythonMainAppResource("example.py")),
        "test-app",
        "main",
        Seq.empty),
      "prefix",
      "appId",
      None,
      Map.empty,
      Map.empty,
      Map.empty,
      Map.empty,
      Map.empty,
      Nil,
      Seq.empty[String])
    validateStepTypesApplied(
      builderUnderTest.buildFromFeatures(conf),
      BASIC_STEP_TYPE,
      CREDENTIALS_STEP_TYPE,
      SERVICE_STEP_TYPE,
      LOCAL_DIRS_STEP_TYPE,
      PYSPARK_STEP_TYPE)
  }

  test("Apply mounting small local files when present..") {
    val tempDir = Utils.createTempDir()
    val tempFile1 = new File(tempDir, "file1.txt")
    Files.write("a", tempFile1, Charsets.UTF_8)
    val tempFile2 = new File(tempDir, "file2.txt")
    Files.write("b", tempFile2, Charsets.UTF_8)
    val allFiles = Seq(
      tempFile1.getAbsolutePath,
      s"file://${tempFile2.getAbsolutePath}",
      "https://localhost:9000/file3.txt")
    val conf = KubernetesConf(
      new SparkConf(false),
      KubernetesDriverSpecificConf(
        None,
        "test-app",
        "main",
        Seq.empty),
      "prefix",
      "appId",
      Some("secret"),
      Map.empty,
      Map.empty,
      Map.empty,
      Map.empty,
      Map.empty,
      Seq.empty,
      allFiles)
    validateStepTypesApplied(
      builderUnderTest.buildFromFeatures(conf),
      BASIC_STEP_TYPE,
      CREDENTIALS_STEP_TYPE,
      SERVICE_STEP_TYPE,
      LOCAL_DIRS_STEP_TYPE,
      MOUNT_LOCAL_FILES_STEP_TYPE,
      JAVA_STEP_TYPE)
  }

  test("Apply volumes step if mounts are present.") {
    val volumeSpec = KubernetesVolumeSpec(
      "volume",
      "/tmp",
      false,
      KubernetesHostPathVolumeConf("/path"))
    val conf = KubernetesConf(
      new SparkConf(false),
      KubernetesDriverSpecificConf(
        None,
        "test-app",
        "main",
        Seq.empty),
      "prefix",
      "appId",
      Some("secret"),
      Map.empty,
      Map.empty,
      Map.empty,
      Map.empty,
      Map.empty,
      volumeSpec :: Nil,
      Seq.empty[String])
    validateStepTypesApplied(
      builderUnderTest.buildFromFeatures(conf),
      BASIC_STEP_TYPE,
      CREDENTIALS_STEP_TYPE,
      SERVICE_STEP_TYPE,
      LOCAL_DIRS_STEP_TYPE,
      MOUNT_VOLUMES_STEP_TYPE,
      JAVA_STEP_TYPE)
  }


  private def validateStepTypesApplied(resolvedSpec: KubernetesDriverSpec, stepTypes: String*)
    : Unit = {
    assert(resolvedSpec.systemProperties.size === stepTypes.size)
    stepTypes.foreach { stepType =>
      assert(resolvedSpec.pod.pod.getMetadata.getLabels.get(stepType) === stepType)
      assert(resolvedSpec.driverKubernetesResources.containsSlice(
        KubernetesFeaturesTestUtils.getSecretsForStepType(stepType)))
      assert(resolvedSpec.systemProperties(stepType) === stepType)
    }
  }
}
