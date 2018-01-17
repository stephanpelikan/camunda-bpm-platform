/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.bpm.engine.test.api.externaltask;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.HashMap;

import org.camunda.bpm.engine.externaltask.LockedExternalTask;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.impl.test.PluggableProcessEngineTestCase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * @author Stephan Pelikan
 */
public class ExternalTaskExpressionSupportTest extends PluggableProcessEngineTestCase {

  @Test
  @Deployment(resources = "org/camunda/bpm/engine/test/api/externaltask/ExternalTaskSupportTest.expressionGlobalProperty.bpmn20.xml")
  public void testExternalTaskSupportUsingGlobalVariable() {

    System.err.println("JAHA:");
    for (org.camunda.bpm.engine.repository.Deployment d : repositoryService.createDeploymentQuery().list()) {
       System.err.println("JUHU :" + d.getName());
    }
    // when
    HashMap<String, Object> globalVariables = new HashMap<String, Object>();
    globalVariables.put("testVariable", "global");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneExternalTaskProcessGlobal", globalVariables);

    // then
    List<LockedExternalTask> externalTasks = externalTaskService
        .fetchAndLock(1, "aWorker")
        .topic("global_externalTaskTopic", 5000L)
        .execute();

    Assert.assertEquals(1, externalTasks.size());
    Assert.assertEquals(processInstance.getId(), externalTasks.get(0).getProcessInstanceId());

    // and it is possible to complete the external task successfully and end the process instance
    externalTaskService.complete(externalTasks.get(0).getId(), "aWorker");

    Assert.assertEquals(0L, runtimeService.createProcessInstanceQuery().count());

    // when
    processInstance = runtimeService.startProcessInstanceByKey("oneExternalTaskProcessGlobal", globalVariables);

    // then
    externalTasks = externalTaskService
        .fetchAndLock(1, "aWorker")
        .topic("local_externalTaskTopic", 5000L)
        .execute();

    Assert.assertEquals(0, externalTasks.size());

    Assert.assertEquals(1L, runtimeService.createProcessInstanceQuery().count());

  }

  @Test
  @Deployment(resources = "org/camunda/bpm/engine/test/api/externaltask/ExternalTaskSupportTest.expressionLocalProperty.bpmn20.xml")
  public void testExternalTaskSupportUsingLocalVariable() {

    System.err.println("JAHA:");
    for (org.camunda.bpm.engine.repository.Deployment d : repositoryService.createDeploymentQuery().list()) {
       System.err.println("JUHU :" + d.getName());
    }
    // when
    HashMap<String, Object> globalVariables = new HashMap<String, Object>();
    globalVariables.put("testVariable", "global");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneExternalTaskProcessLocal", globalVariables);

    // then
    List<LockedExternalTask> externalTasks = externalTaskService
        .fetchAndLock(1, "aWorker")
        .topic("local_externalTaskTopic", 5000L)
        .execute();

    Assert.assertEquals(1, externalTasks.size());
    Assert.assertEquals(processInstance.getId(), externalTasks.get(0).getProcessInstanceId());

    // and it is possible to complete the external task successfully and end the process instance
    externalTaskService.complete(externalTasks.get(0).getId(), "aWorker");

    Assert.assertEquals(0L, runtimeService.createProcessInstanceQuery().count());

    // when
    processInstance = runtimeService.startProcessInstanceByKey("oneExternalTaskProcessLocal", globalVariables);

    // then
    externalTasks = externalTaskService
        .fetchAndLock(1, "aWorker")
        .topic("global_externalTaskTopic", 5000L)
        .execute();

    Assert.assertEquals(0, externalTasks.size());

    Assert.assertEquals(1L, runtimeService.createProcessInstanceQuery().count());

  }
}
