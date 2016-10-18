/*
 * Copyright 2016 camunda services GmbH.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package org.camunda.bpm.engine.test.bpmn.event.conditional;

import java.util.List;
import java.util.Map;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.task.TaskQuery;
import org.camunda.bpm.engine.test.Deployment;
import static org.camunda.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;
import org.junit.Test;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import static org.camunda.bpm.engine.test.bpmn.event.conditional.AbstractConditionalEventTestCase.CONDITIONAL_VAR_EVENT_UPDATE;
import org.camunda.bpm.engine.variable.Variables;
import static org.camunda.bpm.engine.test.bpmn.event.conditional.AbstractConditionalEventTestCase.CONDITIONAL_EVENT_PROCESS_KEY;
import static org.camunda.bpm.engine.test.bpmn.event.conditional.BoundaryConditionalEventTest.TASK_WITH_CONDITION_ID;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaExecutionListener;
import org.junit.Ignore;

/**
 *
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
public class EventSubProcessStartConditionalEventTest extends AbstractConditionalEventTestCase {

  protected static final String TASK_AFTER_SERVICE_TASK = "afterServiceTask";

  @Test
  @Deployment
  public void testTrueCondition() {
    //given process with event sub process conditional start event
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());
    assertEquals(1, conditionEventSubscriptionQuery.list().size());

    //when variable is set on task with condition
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1);

    //then execution is at user task after conditional start event
    task = taskQuery.singleResult();
    assertEquals(TASK_AFTER_CONDITION, task.getName());
    assertEquals(0, conditionEventSubscriptionQuery.list().size());
  }

  @Test
  @Deployment
  public void testFalseCondition() {
    //given process with event sub process conditional start event
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when variable is set on task with condition
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1);

    //then execution stays at user task
    task = taskQuery.singleResult();
    assertEquals(TASK_BEFORE_CONDITION, task.getName());
  }

  @Test
  @Deployment
  public void testVariableCondition() {
    //given process with event sub process conditional start event
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when variable is set on task with condition
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1);

    //then execution is at user task after conditional start event
    task = taskQuery.singleResult();
    assertEquals(TASK_AFTER_CONDITION, task.getName());
  }

  @Test
  @Deployment(resources ={ "org/camunda/bpm/engine/test/bpmn/event/conditional/EventSubProcessStartConditionalEventTest.testVariableCondition.bpmn20.xml"})
  public void testWrongVariableCondition() {
    //given process with event sub process conditional start event
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());
    assertEquals(1, conditionEventSubscriptionQuery.list().size());

    //when variable is set on task contawith condition
    taskService.setVariable(task.getId(), VARIABLE_NAME+1, 1);

    //then execution stays at user task
    task = taskQuery.singleResult();
    assertEquals(TASK_BEFORE_CONDITION, task.getName());
    assertEquals(1, conditionEventSubscriptionQuery.list().size());
  }

  @Test
  @Deployment
  public void testNonInterruptingVariableCondition() {
    //given process with event sub process conditional start event
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());
    assertEquals(1, conditionEventSubscriptionQuery.list().size());

    //when variable is set on task with condition
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1);

    //then execution is at user task after conditional start event
    final List<Task> tasks = taskQuery.list();
    assertEquals(2, tasks.size());
    assertEquals(1, conditionEventSubscriptionQuery.list().size());
  }


  @Test
  @Deployment
  public void testSubProcessVariableCondition() {
    //given process with event sub process conditional start event and user task in sub process
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when local variable is set on task with condition
    taskService.setVariableLocal(task.getId(), VARIABLE_NAME, 1);

    //then execution stays at user task
    task = taskQuery.singleResult();
    assertEquals(TASK_BEFORE_CONDITION, task.getName());
  }

  @Test
  @Deployment(resources ={ "org/camunda/bpm/engine/test/bpmn/event/conditional/EventSubProcessStartConditionalEventTest.testSubProcessVariableCondition.bpmn20.xml"})
  public void testSubProcessSetVariableOnTaskCondition() {
    //given process with event sub process conditional start event and user task in sub process
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when variable is set on task, variable is propagated to process instance
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1);

    //then execution is at user task after conditional start event
    task = taskQuery.singleResult();
    assertEquals(TASK_AFTER_CONDITION, task.getName());
  }

  @Test
  @Deployment(resources ={ "org/camunda/bpm/engine/test/bpmn/event/conditional/EventSubProcessStartConditionalEventTest.testSubProcessVariableCondition.bpmn20.xml"})
  public void testSubProcessSetVariableOnExecutionCondition() {
    //given process with event sub process conditional start event and user task in sub process
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when local variable is set on task execution
    runtimeService.setVariableLocal(task.getExecutionId(), VARIABLE_NAME, 1);

    //then execution stays at user task
    task = taskQuery.singleResult();
    assertEquals(TASK_BEFORE_CONDITION, task.getName());
  }


  protected void deployEventSubProcessWithVariableIsSetInDelegationCode(BpmnModelInstance model, boolean isInterrupting) {
    deployEventSubProcessWithVariableIsSetInDelegationCode(model, CONDITIONAL_EVENT_PROCESS_KEY, isInterrupting);
  }

  protected void deployEventSubProcessWithVariableIsSetInDelegationCode(BpmnModelInstance model, String parentId, boolean isInterrupting) {

    final BpmnModelInstance modelInstance = modify(model)
            .addSubProcessTo(parentId)
            .triggerByEvent()
            .embeddedSubProcess()
            .startEvent()
            .interrupting(isInterrupting)
            .conditionalEventDefinition(CONDITIONAL_EVENT)
            .condition(CONDITION_EXPR)
            .conditionalEventDefinitionDone()
            .userTask()
            .name(TASK_AFTER_CONDITION)
            .endEvent().done();

    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());
  }

  @Test
  public void testSetVariableInDelegate() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent().userTask().name(TASK_BEFORE_CONDITION)
                                                  .serviceTask()
                                                    .camundaClass(SetVariableDelegate.class.getName())
                                                  .endEvent().done();
    deployEventSubProcessWithVariableIsSetInDelegationCode(modelInstance, true);

    // given process with event sub process conditional start event and service task with delegate class which sets a variable
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());
    assertEquals(1, conditionEventSubscriptionQuery.list().size());

    //when task is completed
    taskService.complete(task.getId());

    //then service task with delegated code is called and variable is set
    //-> conditional event is triggered and execution stays is user task after condition
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_AFTER_CONDITION, task.getName());
    assertEquals(0, conditionEventSubscriptionQuery.list().size());
  }


  @Test
  public void testNonInterruptingSetVariableInDelegate() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent().userTask().name(TASK_BEFORE_CONDITION)
                                                  .serviceTask()
                                                    .camundaClass(SetVariableDelegate.class.getName())
                                                  .userTask()
                                                  .endEvent().done();
    deployEventSubProcessWithVariableIsSetInDelegationCode(modelInstance, false);

    // given process with event sub process conditional start event and service task with delegate class which sets a variable
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());
    assertEquals(1, conditionEventSubscriptionQuery.list().size());

    //when task before service task is completed
    taskService.complete(task.getId());

    //then service task with delegated code is called and variable is set
    //-> non interrupting conditional event is triggered
    //execution stays at user task after condition and after service task
    List<Task> tasks = taskQuery.list();
    assertEquals(2, tasks.size());
    assertEquals(1, conditionEventSubscriptionQuery.list().size());
  }

  @Test
  public void testSetVariableInInputMapping() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent().userTask().name(TASK_BEFORE_CONDITION)
                                                  .serviceTask(TASK_WITH_CONDITION_ID)
                                                    .camundaInputParameter(VARIABLE_NAME, "1")
                                                    .camundaExpression(TRUE_CONDITION)
                                                  .userTask().name(TASK_AFTER_SERVICE_TASK)
                                                  .endEvent().done();
    deployEventSubProcessWithVariableIsSetInDelegationCode(modelInstance, true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    //then service task with input mapping is called and variable is set
    //-> interrupting conditional event is not triggered
    //since variable is only localy
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_AFTER_SERVICE_TASK, task.getName());
  }

  @Test
  public void testNonInterruptingSetVariableInInputMapping() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent().userTask().name(TASK_BEFORE_CONDITION)
                                                  .serviceTask(TASK_WITH_CONDITION_ID)
                                                    .camundaInputParameter(VARIABLE_NAME, "1")
                                                    .camundaExpression(TRUE_CONDITION)
                                                  .userTask().name(TASK_AFTER_SERVICE_TASK)
                                                  .endEvent().done();
    deployEventSubProcessWithVariableIsSetInDelegationCode(modelInstance, false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task before service task is completed
    taskService.complete(task.getId());

    //then service task with input mapping is called and variable is set
    //-> non interrupting conditional event is not triggered
    //since variable is only localy
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_AFTER_SERVICE_TASK, task.getName());
  }

  @Test
  public void testSetVariableInExpression() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent().userTask().name(TASK_BEFORE_CONDITION)
                                                  .serviceTask(TASK_WITH_CONDITION_ID)
                                                    .camundaExpression("${execution.setVariable(\"variable\", 1)}")
                                                  .userTask().name(TASK_AFTER_SERVICE_TASK)
                                                  .endEvent().done();
    deployEventSubProcessWithVariableIsSetInDelegationCode(modelInstance, true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());


    //then service task with expression is called and variable is set
    //-> interrupting conditional event is triggered
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_AFTER_CONDITION, task.getName());
  }

  @Test
  public void testNonInterruptingSetVariableInExpression() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent().userTask().name(TASK_BEFORE_CONDITION)
                                                  .serviceTask(TASK_WITH_CONDITION_ID)
                                                    .camundaExpression("${execution.setVariable(\"variable\", 1)}")
                                                  .userTask().name(TASK_AFTER_SERVICE_TASK)
                                                  .endEvent().done();
    deployEventSubProcessWithVariableIsSetInDelegationCode(modelInstance, false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task before service task is completed
    taskService.complete(task.getId());

    //then service task with expression is called and variable is set
    //-> non interrupting conditional event is triggered
    List<Task> tasks = taskQuery.list();
    assertEquals(2, tasks.size());
    assertEquals(1, conditionEventSubscriptionQuery.list().size());
  }

  @Test
  public void testSetVariableInInputMappingOfSubProcess() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent().userTask().name(TASK_BEFORE_CONDITION)
                                                  .subProcess(SUBPROCESS_ID)
                                                    .camundaInputParameter(VARIABLE_NAME, "1")
                                                    .embeddedSubProcess()
                                                    .startEvent("startSubProcess")
                                                    .userTask().name(TASK_IN_SUB_PROCESS)
                                                    .endEvent()
                                                  .subProcessDone()
                                                  .endEvent()
                                                  .done();
    deployEventSubProcessWithVariableIsSetInDelegationCode(modelInstance, SUBPROCESS_ID, true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    //then input mapping from sub process sets variable
    //-> interrupting conditional event is triggered
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_AFTER_CONDITION, task.getName());
  }

  @Test
  public void testNonInterruptingSetVariableInInputMappingOfSubProcess() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent().userTask().name(TASK_BEFORE_CONDITION)
                                                  .subProcess(SUBPROCESS_ID)
                                                    .camundaInputParameter(VARIABLE_NAME, "1")
                                                    .embeddedSubProcess()
                                                    .startEvent()
                                                    .userTask().name(TASK_IN_SUB_PROCESS)
                                                    .endEvent()
                                                  .subProcessDone()
                                                  .endEvent()
                                                  .done();
    deployEventSubProcessWithVariableIsSetInDelegationCode(modelInstance, SUBPROCESS_ID, false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task before service task is completed
    taskService.complete(task.getId());

    //then input mapping from sub process sets variable
    //-> non interrupting conditional event is triggered
    List<Task> tasks = taskQuery.list();
    assertEquals(2, tasks.size());
    assertEquals(1, conditionEventSubscriptionQuery.list().size());
  }

  @Test
  public void testSetVariableInOutputMapping() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                    .camundaOutputParameter(VARIABLE_NAME, "1")
                                                  .userTask().name("afterOutputMapping")
                                                  .endEvent()
                                                  .done();
    deployEventSubProcessWithVariableIsSetInDelegationCode(modelInstance, true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    //then input mapping from sub process sets variable
    //-> interrupting conditional event is triggered
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_AFTER_CONDITION, task.getName());
  }

  @Test
  public void testNonInterruptingSetVariableInOutputMapping() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                    .camundaOutputParameter(VARIABLE_NAME, "1")
                                                  .userTask().name("afterOutputMapping")
                                                  .endEvent()
                                                  .done();
    deployEventSubProcessWithVariableIsSetInDelegationCode(modelInstance, false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task before service task is completed
    taskService.complete(task.getId());

    //then input mapping from sub process sets variable
    //-> non interrupting conditional event is triggered
    List<Task> tasks = taskQuery.list();
    assertEquals(2, tasks.size());
    assertEquals(1, conditionEventSubscriptionQuery.list().size());
  }

  @Test
  public void testSetVariableInOutputMappingOfCallActivity() {
    final BpmnModelInstance delegatedInstance = Bpmn.createExecutableProcess("delegatedProcess")
                                                     .startEvent()
                                                     .serviceTask()
                                                     .camundaExpression(EXPR_SET_VARIABLE)
                                                     .endEvent()
                                                     .done();

    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, delegatedInstance).deploy());

    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                  .callActivity(TASK_WITH_CONDITION_ID)
                                                    .calledElement("delegatedProcess")
                                                    .camundaOutputParameter(VARIABLE_NAME, "1")
                                                  .userTask().name("afterOutputMapping")
                                                  .endEvent()
                                                  .done();
    deployEventSubProcessWithVariableIsSetInDelegationCode(modelInstance, true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    //then input mapping from sub process sets variable
    //-> interrupting conditional event is triggered
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_AFTER_CONDITION, task.getName());
  }

  @Test
  public void testNonInterruptingSetVariableInOutputMappingOfCallActivity() {
    final BpmnModelInstance delegatedInstance = Bpmn.createExecutableProcess("delegatedProcess")
                                                     .startEvent()
                                                     .serviceTask()
                                                     .camundaExpression(EXPR_SET_VARIABLE)
                                                     .endEvent()
                                                     .done();

    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, delegatedInstance).deploy());

    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                  .callActivity(TASK_WITH_CONDITION_ID)
                                                    .calledElement("delegatedProcess")
                                                    .camundaOutputParameter(VARIABLE_NAME, "1")
                                                  .userTask().name("afterOutputMapping")
                                                  .endEvent()
                                                  .done();
    deployEventSubProcessWithVariableIsSetInDelegationCode(modelInstance, false);


    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task before service task is completed
    taskService.complete(task.getId());

    //then input mapping from sub process sets variable
    //-> non interrupting conditional event is triggered
    List<Task> tasks = taskQuery.list();
    assertEquals(2, tasks.size());
    assertEquals(1, conditionEventSubscriptionQuery.list().size());
  }


  @Test
  public void testSetVariableInStartListener() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                  .userTask(TASK_WITH_CONDITION_ID)
                                                    .camundaExecutionListenerExpression("start", "${execution.setVariable(\"variable\", 1)}")
                                                  .endEvent()
                                                  .done();
    deployEventSubProcessWithVariableIsSetInDelegationCode(modelInstance, true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    //then start listener sets variable
    //conditional event is triggered
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_AFTER_CONDITION, task.getName());
  }

  @Test
  public void testNonInterruptingSetVariableInStartListener() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                  .userTask(TASK_WITH_CONDITION_ID)
                                                    .camundaExecutionListenerExpression("start", "${execution.setVariable(\"variable\", 1)}")
                                                  .endEvent()
                                                  .done();
    deployEventSubProcessWithVariableIsSetInDelegationCode(modelInstance, false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    //then start listener sets variable
    //non interrupting boundary event is triggered
    List<Task> tasks = taskQuery.list();
    assertEquals(2, tasks.size());
    assertEquals(1, conditionEventSubscriptionQuery.list().size());
  }

  @Test
  public void testSetVariableInTakeListener() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                  .sequenceFlowId(FLOW_ID)
                                                  .userTask().name("task")
                                                  .endEvent()
                                                  .done();
    CamundaExecutionListener listener = modelInstance.newInstance(CamundaExecutionListener.class);
    listener.setCamundaEvent(ExecutionListener.EVENTNAME_TAKE);
    listener.setCamundaExpression(EXPR_SET_VARIABLE);
    modelInstance.<SequenceFlow>getModelElementById(FLOW_ID).builder().addExtensionElement(listener);
    deployEventSubProcessWithVariableIsSetInDelegationCode(modelInstance, true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    //then take listener sets variable
    //conditional event is triggered
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_AFTER_CONDITION, task.getName());
  }

  @Test
  public void testNonInterruptingSetVariableInTakeListener() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                  .sequenceFlowId(FLOW_ID)
                                                  .userTask().name("task")
                                                  .endEvent()
                                                  .done();
    CamundaExecutionListener listener = modelInstance.newInstance(CamundaExecutionListener.class);
    listener.setCamundaEvent(ExecutionListener.EVENTNAME_TAKE);
    listener.setCamundaExpression(EXPR_SET_VARIABLE);
    modelInstance.<SequenceFlow>getModelElementById(FLOW_ID).builder().addExtensionElement(listener);
    deployEventSubProcessWithVariableIsSetInDelegationCode(modelInstance, false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    //then take listener sets variable
    //non interrupting boundary event is not triggered
    List<Task> tasks = taskQuery.list();
    assertEquals(2, tasks.size());
    assertEquals(1, conditionEventSubscriptionQuery.list().size());
  }

  @Test
  public void testSetVariableInEndListener() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                    .camundaExecutionListenerExpression(ExecutionListener.EVENTNAME_END, "${execution.setVariable(\"variable\", 1)}")
                                                  .userTask(TASK_WITH_CONDITION_ID)
                                                  .endEvent()
                                                  .done();
    deployEventSubProcessWithVariableIsSetInDelegationCode(modelInstance, true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    //then start listener sets variable
    //conditional event is triggered
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_AFTER_CONDITION, task.getName());
  }

  @Test
  public void testNonInterruptingSetVariableInEndListener() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                    .camundaExecutionListenerExpression(ExecutionListener.EVENTNAME_END, "${execution.setVariable(\"variable\", 1)}")
                                                  .userTask(TASK_WITH_CONDITION_ID)
                                                  .endEvent()
                                                  .done();
    deployEventSubProcessWithVariableIsSetInDelegationCode(modelInstance, false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    //then start listener sets variable
    //non interrupting boundary event is triggered
    List<Task> tasks = taskQuery.list();
    assertEquals(2, tasks.size());
    assertEquals(1, conditionEventSubscriptionQuery.list().size());
  }

  @Test
  public void testSetVariableInCallActivity() {
    final BpmnModelInstance delegatedInstance = Bpmn.createExecutableProcess("delegatedProcess")
                                                     .startEvent()
                                                     .serviceTask()
                                                     .camundaExpression(EXPR_SET_VARIABLE)
                                                     .endEvent()
                                                     .done();

    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, delegatedInstance).deploy());

    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                  .callActivity(TASK_WITH_CONDITION_ID)
                                                    .calledElement("delegatedProcess")
                                                  .userTask().name(TASK_AFTER_SERVICE_TASK)
                                                  .endEvent()
                                                  .done();
    deployEventSubProcessWithVariableIsSetInDelegationCode(modelInstance, true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    //then service task in call activity sets variable
    //conditional event is not triggered
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_AFTER_SERVICE_TASK, task.getName());
  }

  @Test
  public void testNonInterruptingSetVariableInCallActivity() {
    final BpmnModelInstance delegatedInstance = Bpmn.createExecutableProcess("delegatedProcess")
                                                     .startEvent()
                                                     .serviceTask()
                                                     .camundaExpression(EXPR_SET_VARIABLE)
                                                     .endEvent()
                                                     .done();

    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, delegatedInstance).deploy());

    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                  .callActivity(TASK_WITH_CONDITION_ID)
                                                    .calledElement("delegatedProcess")
                                                  .userTask().name(TASK_AFTER_SERVICE_TASK)
                                                  .endEvent()
                                                  .done();
    deployEventSubProcessWithVariableIsSetInDelegationCode(modelInstance, false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    //then service task in call activity sets variable
    //conditional event is not triggered
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_AFTER_SERVICE_TASK, task.getName());
  }


  @Test
  public void testSetVariableInSubProcessInDelegatedCode() {

    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                  .subProcess(SUBPROCESS_ID)
                                                  .embeddedSubProcess()
                                                    .startEvent()
                                                    .serviceTask()
                                                    .camundaExpression(EXPR_SET_VARIABLE)
                                                    .userTask().name(TASK_AFTER_SERVICE_TASK)
                                                    .endEvent()
                                                  .subProcessDone()
                                                  .endEvent()
                                                  .done();
    deployEventSubProcessWithVariableIsSetInDelegationCode(modelInstance, SUBPROCESS_ID, true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    //then service task in sub process sets variable
    //conditional event is triggered
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_AFTER_CONDITION, task.getName());
  }

  @Test
  public void testNonInterruptingSetVariableInSubProcessInDelegatedCode() {

    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                  .subProcess(SUBPROCESS_ID)
                                                  .embeddedSubProcess()
                                                    .startEvent()
                                                    .serviceTask()
                                                    .camundaExpression(EXPR_SET_VARIABLE)
                                                    .userTask().name(TASK_AFTER_SERVICE_TASK)
                                                    .endEvent()
                                                  .subProcessDone()
                                                  .endEvent()
                                                  .done();
    deployEventSubProcessWithVariableIsSetInDelegationCode(modelInstance, SUBPROCESS_ID, false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    //then service task in sub process sets variable
    //conditional event is triggered
    List<Task> tasks = taskQuery.list();
    assertEquals(2, tasks.size());
    assertEquals(1, conditionEventSubscriptionQuery.list().size());
  }


  @Test
  public void testSetVariableInSubProcessInDelegatedCodeConditionOnPI() {

    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                  .subProcess(SUBPROCESS_ID)
                                                  .embeddedSubProcess()
                                                    .startEvent()
                                                    .serviceTask()
                                                    .camundaExpression(EXPR_SET_VARIABLE)
                                                    .userTask().name(TASK_AFTER_SERVICE_TASK)
                                                    .endEvent()
                                                  .subProcessDone()
                                                  .endEvent()
                                                  .done();
    deployEventSubProcessWithVariableIsSetInDelegationCode(modelInstance, true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    //then service task in sub process sets variable
    //conditional event is triggered
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_AFTER_CONDITION, task.getName());
  }

  @Test
  public void testNonInterruptingSetVariableInSubProcessInDelegatedCodeConditionOnPI() {

    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                  .subProcess(SUBPROCESS_ID)
                                                  .embeddedSubProcess()
                                                    .startEvent()
                                                    .serviceTask()
                                                    .camundaExpression(EXPR_SET_VARIABLE)
                                                    .userTask().name(TASK_AFTER_SERVICE_TASK)
                                                    .endEvent()
                                                  .subProcessDone()
                                                  .endEvent()
                                                  .done();
    deployEventSubProcessWithVariableIsSetInDelegationCode(modelInstance, false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    //then service task in sub process sets variable
    //conditional event is triggered
    List<Task> tasks = taskQuery.list();
    assertEquals(2, tasks.size());
    assertEquals(1, conditionEventSubscriptionQuery.list().size());
  }

  // variable name /////////////////////////////////////////////////////////////

  @Test
  @Deployment(resources ={ "org/camunda/bpm/engine/test/bpmn/event/conditional/EventSubProcessStartConditionalEventTest.testSubProcessVariableCondition.bpmn20.xml"})
  public void testSubProcessSetVariableOnProcessInstanceCondition() {
    //given process with event sub process conditional start event and user task in sub process
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when variable is set on process instance
    runtimeService.setVariable(procInst.getId(), VARIABLE_NAME, 1);

    //then execution is at user task after conditional start event
    task = taskQuery.singleResult();
    assertEquals(TASK_AFTER_CONDITION, task.getName());
  }

  @Test
  public void testVariableConditionWithVariableName() {

    //given process with event sub process conditional start event and defined variable name
    deployEventSubProcessWithVarName(true);

    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);

    //when variable with name `variable1` is set on execution
    taskService.setVariable(task.getId(), VARIABLE_NAME+1, 1);

    //then nothing happens
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());
    assertEquals(1, conditionEventSubscriptionQuery.list().size());

    //when variable with name `variable` is set on execution
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1);

    //then execution is at user task after conditional start event
    task = taskQuery.singleResult();
    assertEquals(TASK_AFTER_CONDITION, task.getName());
    assertEquals(0, conditionEventSubscriptionQuery.list().size());
  }


  @Test
  public void testVariableConditionWithVariableNameAndEvent() {

    //given process with event sub process conditional start event and defined variable name
    deployEventSubProcessWithVarNameAndEvents(true, CONDITIONAL_VAR_EVENT_UPDATE);

    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);

    //when variable with name `variable` is set on execution
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1);

    //then nothing happens
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());
    assertEquals(1, conditionEventSubscriptionQuery.list().size());

    //when variable with name `variable` is updated
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1);

    //then execution is at user task after conditional start event
    task = taskQuery.singleResult();
    assertEquals(TASK_AFTER_CONDITION, task.getName());
    assertEquals(0, conditionEventSubscriptionQuery.list().size());
  }

  @Test
  public void testNonInterruptingVariableConditionWithVariableName() {

    //given process with event sub process non interrupting conditional start event and defined variable name
    deployEventSubProcessWithVarName(false);

    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);

    //when variable with name `variable1` is set on execution
    taskService.setVariable(task.getId(), VARIABLE_NAME+1, 1);

    //then nothing happens
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());
    assertEquals(1, conditionEventSubscriptionQuery.list().size());

    //when variable with name `variable` is set, updated and deleted
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1); //create
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1); //update
    taskService.removeVariable(task.getId(), VARIABLE_NAME); //delete

    //then execution is for three times at user task after conditional start event
    List<Task> tasks = taskQuery.taskName(TASK_AFTER_CONDITION).list();
    assertEquals(3, tasks.size());
    assertEquals(1, conditionEventSubscriptionQuery.list().size());
  }

  @Test
  public void testNonInterruptingVariableConditionWithVariableNameAndEvents() {

    //given process with event sub process non interrupting conditional start event and defined variable name and events
    deployEventSubProcessWithVarNameAndEvents(false, CONDITIONAL_VAR_EVENTS);

    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);

    //when variable with name `variable` is set, updated and deleted
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1); //create
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1); //update
    taskService.removeVariable(task.getId(), VARIABLE_NAME); //delete

    //then execution is for two times at user task after conditional start event
    List<Task> tasks = taskQuery.taskName(TASK_AFTER_CONDITION).list();
    assertEquals(2, tasks.size());
    assertEquals(1, conditionEventSubscriptionQuery.list().size());
  }

  @Test
  public void testVariableConditionWithVariableEvent() {

    //given process with event sub process conditional start event and defined variable event
    deployEventSubProcessWithVarEvent(true, CONDITIONAL_VAR_EVENT_UPDATE);

    Map<String, Object> variables = Variables.createVariables();
    variables.put(VARIABLE_NAME+1, 0);
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY, variables);
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);

    //when variable with name `variable` is set on execution
    runtimeService.setVariable(procInst.getId(), VARIABLE_NAME, 1);

    //then nothing happens
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());
    assertEquals(1, conditionEventSubscriptionQuery.list().size());

    //when variable with name `variable1` is updated
    runtimeService.setVariable(procInst.getId(), VARIABLE_NAME+1, 1);

    //then execution is at user task after conditional intermediate event
    task = taskQuery.singleResult();
    assertEquals(TASK_AFTER_CONDITION, task.getName());
    assertEquals(0, conditionEventSubscriptionQuery.list().size());
  }

  @Test
  public void testNonInterruptingVariableConditionWithVariableEvent() {

    //given process with event sub process non interrupting conditional start event and defined variable event
    deployEventSubProcessWithVarEvent(false, CONDITIONAL_VAR_EVENT_UPDATE);

    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);

    //when variable with name `variable` is set on execution
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1); //create

    //then nothing happens
    task = taskQuery.singleResult();
    assertNotNull(task);

    //when variable is updated twice
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1); //update
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1); //update

    //then execution is for two times at user task after conditional start event
    List<Task> tasks = taskQuery.taskName(TASK_AFTER_CONDITION).list();
    assertEquals(2, tasks.size());
    assertEquals(1, conditionEventSubscriptionQuery.list().size());
  }


  protected void deployEventSubProcessWithVarName(boolean interrupting) {
    final BpmnModelInstance modelInstance = modify(TASK_MODEL)
            .addSubProcessTo(CONDITIONAL_EVENT_PROCESS_KEY)
              .triggerByEvent()
              .embeddedSubProcess()
              .startEvent()
                .interrupting(interrupting)
                .conditionalEventDefinition(CONDITIONAL_EVENT)
                  .condition(TRUE_CONDITION)
                  .camundaVariableName(VARIABLE_NAME)
                .conditionalEventDefinitionDone()
                .userTask()
                .name(TASK_AFTER_CONDITION)
              .endEvent()
            .done();

    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());
  }

  protected void deployEventSubProcessWithVarNameAndEvents(boolean interrupting, String varEvent) {
    final BpmnModelInstance modelInstance = modify(TASK_MODEL)
            .addSubProcessTo(CONDITIONAL_EVENT_PROCESS_KEY)
              .triggerByEvent()
              .embeddedSubProcess()
              .startEvent()
                .interrupting(interrupting)
                .conditionalEventDefinition(CONDITIONAL_EVENT)
                  .condition(CONDITION_EXPR)
                  .camundaVariableName(VARIABLE_NAME)
                  .camundaVariableEvents(varEvent)
                .conditionalEventDefinitionDone()
                .userTask()
                .name(TASK_AFTER_CONDITION)
              .endEvent()
            .done();

    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());
  }

  protected void deployEventSubProcessWithVarEvent(boolean interrupting, String varEvent) {
    final BpmnModelInstance modelInstance = modify(TASK_MODEL)
            .addSubProcessTo(CONDITIONAL_EVENT_PROCESS_KEY)
              .triggerByEvent()
              .embeddedSubProcess()
              .startEvent()
                .interrupting(interrupting)
                .conditionalEventDefinition(CONDITIONAL_EVENT)
                  .condition(CONDITION_EXPR)
                  .camundaVariableEvents(varEvent)
                .conditionalEventDefinitionDone()
                .userTask()
                .name(TASK_AFTER_CONDITION)
              .endEvent()
            .done();

    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());
  }
}
