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
package org.camunda.bpm.qa.performance.engine.bpmn;

import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.impl.ProcessEngineLogger;
import org.camunda.bpm.engine.impl.db.EnginePersistenceLogger;
import org.camunda.bpm.engine.repository.DeploymentBuilder;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.qa.performance.engine.bpmn.delegate.RandomSleepDelegate;
import org.camunda.bpm.qa.performance.engine.framework.PerfTest;
import org.camunda.bpm.qa.performance.engine.junit.ProcessEngineJobExecutorPerformanceTestCase;
import org.camunda.bpm.qa.performance.engine.steps.*;
import org.junit.Test;


/**
 * @author: Johannes Heinemann
 */
public class MultiInstancePerformanceTest extends ProcessEngineJobExecutorPerformanceTestCase {

  final protected String MI_CARDINALITY = "5000";


  @Test
  public void testBla() {

    BpmnModelInstance mainProcess = Bpmn.createExecutableProcess("mainprocess")
        .startEvent()
        .subProcess("subprocess")
          .embeddedSubProcess()
            .startEvent()
            .callActivity()
              .calledElement("calledProcess")
              .camundaAsyncAfter()
            .endEvent()
        .subProcessDone()
        .endEvent("end")
          .camundaExecutionListenerClass("end", "org.camunda.bpm.qa.performance.engine.steps.SignalTestRunListener")
        .done();

    mainProcess = ModifiableBpmnModelInstance.modify(mainProcess)
        .activityBuilder("subprocess")
        .multiInstance()
        .cardinality(MI_CARDINALITY)
        .parallel()
        .done();

    BpmnModelInstance subProcess = Bpmn.createExecutableProcess("calledProcess")
        .startEvent()
          .camundaAsyncBefore()
        .serviceTask()
          .camundaClass(RandomSleepDelegate.class.getName())
        .endEvent()
        .done();

    DeploymentBuilder deploymentbuilder = engine.getRepositoryService().createDeployment();
    deploymentbuilder.addModelInstance("mainProcess.bpmn", mainProcess);
    deploymentbuilder.addModelInstance("calledProcess.bpmn", subProcess);
    deploymentbuilder.deploy();

    performanceTest()
        .step(new StartProcessInstanceStep(engine, "mainprocess"))
        .step(new WaitStep())
        .step(new CountJobsStep(engine))
        .run();
  }
}
