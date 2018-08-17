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
package org.apache.spark.deploy.k8s.features

import io.fabric8.kubernetes.api.model.{HasMetadata, PodBuilder}
import org.apache.spark.deploy.k8s.{KubernetesConf, KubernetesRoleSpecificConf, SparkPod}

private[spark] class TemplateVolumeStep(
   conf: KubernetesConf[_ <: KubernetesRoleSpecificConf])
  extends KubernetesFeatureConfigStep {
  def configurePod(pod: SparkPod): SparkPod = {
    val podWithVolume = new PodBuilder(pod.pod)
      .editSpec()
      .addNewVolume()
      // TODO(osatici): create the volume for the executor podspec template here
      .endVolume()
      .endSpec()
      .build()
    SparkPod(podWithVolume, pod.container)
  }

  def getAdditionalPodSystemProperties(): Map[String, String] = {
    // TODO(osatici): remap executor podspec template config to point to the mount created above
  }

  def getAdditionalKubernetesResources(): Seq[HasMetadata] = Seq.empty
}
