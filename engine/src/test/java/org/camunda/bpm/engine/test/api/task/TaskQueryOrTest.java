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
package org.camunda.bpm.engine.test.api.task;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

import org.camunda.bpm.engine.CaseService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;

import org.camunda.bpm.engine.impl.util.ClockUtil;
import org.camunda.bpm.engine.runtime.CaseExecution;
import org.camunda.bpm.engine.runtime.CaseInstance;
import org.camunda.bpm.engine.task.DelegationState;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.task.TaskQuery;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Tassilo Weidner
 */
public class TaskQueryOrTest {
  
  @Rule
  public ProcessEngineRule rule = new ProcessEngineRule();
  
  protected RuntimeService runtimeService = rule.getRuntimeService();
  protected TaskService taskService = rule.getTaskService();
  protected CaseService caseService = rule.getCaseService();
  protected RepositoryService repositoryService = rule.getRepositoryService();

  @Before
  public void init() {
    runtimeService = rule.getRuntimeService();
    taskService = rule.getTaskService();
    caseService = rule.getCaseService();
    repositoryService = rule.getRepositoryService();
  }

  @Test
  public void testQueryTaskIds() {
    // given
    BpmnModelInstance processDefinition1 = Bpmn.createExecutableProcess("processDefinition1")
      .startEvent()
        .userTask()
      .endEvent()
      .done();

    String deploymentId1 = repositoryService.createDeployment().addModelInstance("foo.bpmn", processDefinition1).deploy().getId();

    BpmnModelInstance processDefinition2 = Bpmn.createExecutableProcess("processDefinition2")
      .startEvent()
        .userTask()
      .endEvent()
      .done();

    String deploymentId2 = repositoryService.createDeployment().addModelInstance("foo.bpmn", processDefinition2).deploy().getId();

    runtimeService.startProcessInstanceByKey("processDefinition1");
    runtimeService.startProcessInstanceByKey("processDefinition2");

    // when
    List<Task> tasks = taskService.createTaskQuery()
      .startOr()
        .taskId(taskService.createTaskQuery().processDefinitionKey("processDefinition1").singleResult().getId())
        .taskId(taskService.createTaskQuery().processDefinitionKey("processDefinition2").singleResult().getId())
      .endOr()
      .list();

    // then
    assertEquals(2, tasks.size());

    repositoryService.deleteDeployment(deploymentId1, true);
    repositoryService.deleteDeployment(deploymentId2, true);
  }

  @Test
  public void testQueryTaskNames() {
    // given
    BpmnModelInstance processDefinition1 = Bpmn.createExecutableProcess("processDefinition1")
      .startEvent()
        .userTask()
          .name("Task 1")
      .endEvent()
      .done();

    String deploymentId1 = repositoryService.createDeployment().addModelInstance("foo.bpmn", processDefinition1).deploy().getId();

    BpmnModelInstance processDefinition2 = Bpmn.createExecutableProcess("processDefinition2")
      .startEvent()
        .userTask()
          .name("Task 2")
      .endEvent()
      .done();

    String deploymentId2 =  repositoryService.createDeployment().addModelInstance("foo.bpmn", processDefinition2).deploy().getId();

    runtimeService.startProcessInstanceByKey("processDefinition1");
    runtimeService.startProcessInstanceByKey("processDefinition2");

    // when
    List<Task> tasks = taskService.createTaskQuery()
      .startOr()
        .taskName(taskService.createTaskQuery().processDefinitionKey("processDefinition1").singleResult().getName())
        .taskName(taskService.createTaskQuery().processDefinitionKey("processDefinition2").singleResult().getName())
      .endOr()
      .list();

    // then
    assertEquals(2, tasks.size());
    
    repositoryService.deleteDeployment(deploymentId1, true);
    repositoryService.deleteDeployment(deploymentId2, true);
  }

  @Test
  public void testQueryTaskNamesNotEqual() {
    // given
    BpmnModelInstance processDefinition1 = Bpmn.createExecutableProcess("processDefinition1")
      .startEvent()
        .userTask()
          .name("Task 1")
      .endEvent()
      .done();

    String deploymentId1 = repositoryService.createDeployment().addModelInstance("foo.bpmn", processDefinition1).deploy().getId();

    BpmnModelInstance processDefinition2 = Bpmn.createExecutableProcess("processDefinition2")
      .startEvent()
        .userTask()
          .name("Task 2")
      .endEvent()
      .done();

    String deploymentId2 = repositoryService.createDeployment().addModelInstance("foo.bpmn", processDefinition2).deploy().getId();

    runtimeService.startProcessInstanceByKey("processDefinition1");
    runtimeService.startProcessInstanceByKey("processDefinition2");

    // when
    List<Task> tasks = taskService.createTaskQuery()
      .processDefinitionKeyIn("processDefinition1", "processDefinition2")
      .startOr()
        .taskNameNotEqual(taskService.createTaskQuery().processDefinitionKey("processDefinition1").singleResult().getName())
        .taskNameNotEqual(taskService.createTaskQuery().processDefinitionKey("processDefinition2").singleResult().getName())
      .endOr()
      .list();

    // then
    assertEquals(0, tasks.size());

    repositoryService.deleteDeployment(deploymentId1, true);
    repositoryService.deleteDeployment(deploymentId2, true);
  }

  @Test
  public void testQueryTaskNamesLike() {
    // given
    BpmnModelInstance processDefinition1 = Bpmn.createExecutableProcess("processDefinition1")
      .startEvent()
        .userTask()
          .name("Task 1 waits for user")
      .endEvent()
      .done();

    String deploymentId1 = repositoryService.createDeployment().addModelInstance("foo.bpmn", processDefinition1).deploy().getId();

    BpmnModelInstance processDefinition2 = Bpmn.createExecutableProcess("processDefinition2")
      .startEvent()
        .userTask()
          .name("Task 2 waits for user")
      .endEvent()
      .done();

    String deploymentId2 = repositoryService.createDeployment().addModelInstance("foo.bpmn", processDefinition2).deploy().getId();

    runtimeService.startProcessInstanceByKey("processDefinition1");
    runtimeService.startProcessInstanceByKey("processDefinition2");

    // when
    List<Task> tasks = taskService.createTaskQuery()
      .processDefinitionKeyIn("processDefinition1", "processDefinition2")
      .startOr()
        .taskNameLike("Task 1%")
        .taskNameLike("Task 2%")
      .endOr()
      .list();

    // then
    assertEquals(2, tasks.size());

    repositoryService.deleteDeployment(deploymentId1, true);
    repositoryService.deleteDeployment(deploymentId2, true);
  }

  @Test
  public void testQueryTaskNamesNotLike() {
    // given
    BpmnModelInstance processDefinition1 = Bpmn.createExecutableProcess("processDefinition1")
      .startEvent()
        .userTask()
          .name("Task 1 waits for user")
      .endEvent()
      .done();

    String deploymentId1 = repositoryService.createDeployment().addModelInstance("foo.bpmn", processDefinition1).deploy().getId();

    BpmnModelInstance processDefinition2 = Bpmn.createExecutableProcess("processDefinition2")
      .startEvent()
        .userTask()
          .name("Task 2 waits for user")
      .endEvent()
      .done();

    String deploymentId2 = repositoryService.createDeployment().addModelInstance("foo.bpmn", processDefinition2).deploy().getId();

    runtimeService.startProcessInstanceByKey("processDefinition1");
    runtimeService.startProcessInstanceByKey("processDefinition2");

    // when
    List<Task> tasks = taskService.createTaskQuery()
      .processDefinitionKeyIn("processDefinition1", "processDefinition2")
      .startOr()
        .taskNameNotLike("Task 1%")
        .taskNameNotLike("Task 2%")
      .endOr()
      .list();

    // then
    assertEquals(2, tasks.size());

    repositoryService.deleteDeployment(deploymentId1, true);
    repositoryService.deleteDeployment(deploymentId2, true);
  }

  @Test
  public void testQueryTaskDescriptions() {
    // given
    Task task1 = taskService.newTask();
    task1.setDescription("Task 1 description");
    taskService.saveTask(task1);

    Task task2 = taskService.newTask();
    task2.setDescription("Task 2 description");
    taskService.saveTask(task2);

    // when
    List<Task> tasks = taskService.createTaskQuery()
      .startOr()
        .taskDescription("Task 1 description")
        .taskDescription("Task 2 description")
      .endOr()
      .list();

    // then
    assertEquals(2, tasks.size());
    taskService.deleteTasks(Arrays.asList(task1.getId(), task2.getId()), true);
  }

  @Test
  public void testQueryTaskDescriptionsLike() {
    // given
    Task task1 = taskService.newTask();
    task1.setPriority(333);
    taskService.saveTask(task1);

    Task task2 = taskService.newTask();
    task2.setPriority(444);
    taskService.saveTask(task2);

    // when
    List<Task> tasks = taskService.createTaskQuery()
      .startOr()
        .taskPriority(333)
        .taskPriority(444)
      .endOr()
      .list();

    // then
    assertEquals(2, tasks.size());
    taskService.deleteTasks(Arrays.asList(task1.getId(), task2.getId()), true);
  }

  @Test
  public void testQueryTaskPriorities() {
    // given
    Task task1 = taskService.newTask();
    task1.setName("Task 1");
    task1.setDescription("Task 1 description");
    taskService.saveTask(task1);

    Task task2 = taskService.newTask();
    task2.setDescription("Task 2 description");
    task2.setName("Task 2");
    taskService.saveTask(task2);

    // when
    List<Task> tasks = taskService.createTaskQuery()
      .startOr()
        .taskDescriptionLike("Task 1%")
        .taskDescriptionLike("Task 2%")
      .endOr()
      .list();

    // then
    assertEquals(2, tasks.size());
    taskService.deleteTasks(Arrays.asList(task1.getId(), task2.getId()), true);
  }

  @Test
  public void testQueryTaskAsignees() {
    // given
    Task task1 = taskService.newTask();
    task1.setAssignee("John Doe");
    taskService.saveTask(task1);

    Task task2 = taskService.newTask();
    task2.setAssignee("Jack Camundo");
    taskService.saveTask(task2);

    // when
    List<Task> tasks = taskService.createTaskQuery()
      .startOr()
        .taskAssignee("John Doe")
        .taskAssignee("Jack Camundo")
      .endOr()
      .list();

    // then
    assertEquals(2, tasks.size());
    taskService.deleteTasks(Arrays.asList(task1.getId(), task2.getId()), true);
  }

  @Test
  public void testQueryTaskAsigneesLike() {
    // given
    Task task1 = taskService.newTask();
    task1.setAssignee("John Doe");
    taskService.saveTask(task1);

    Task task2 = taskService.newTask();
    task2.setAssignee("Jack Camundo");
    taskService.saveTask(task2);

    // when
    List<Task> tasks = taskService.createTaskQuery()
      .startOr()
        .taskAssigneeLike("John%")
        .taskAssigneeLike("Jack%")
      .endOr()
      .list();

    // then
    assertEquals(2, tasks.size());
    taskService.deleteTasks(Arrays.asList(task1.getId(), task2.getId()), true);
  }

  @Test
  public void testQueryTaskOwners() {
    // given
    Task task1 = taskService.newTask();
    task1.setOwner("John Doe");
    taskService.saveTask(task1);

    Task task2 = taskService.newTask();
    task2.setOwner("Jack Camundo");
    taskService.saveTask(task2);

    // when
    List<Task> tasks = taskService.createTaskQuery()
      .startOr()
        .taskOwner("John Doe")
        .taskOwner("Jack Camundo")
      .endOr()
      .list();

    // then
    assertEquals(2, tasks.size());
    taskService.deleteTasks(Arrays.asList(task1.getId(), task2.getId()), true);
  }

  @Test
  public void testQueryTaskProcessInstanceIds() {
    // given
    BpmnModelInstance processDefinition1 = Bpmn.createExecutableProcess("processDefinition1")
      .startEvent()
        .userTask()
      .endEvent()
      .done();

    String deploymentId1 = repositoryService.createDeployment().addModelInstance("foo.bpmn", processDefinition1).deploy().getId();

    BpmnModelInstance processDefinition2 = Bpmn.createExecutableProcess("processDefinition2")
      .startEvent()
        .userTask()
      .endEvent()
      .done();

    String deploymentId2 = repositoryService.createDeployment().addModelInstance("foo.bpmn", processDefinition2).deploy().getId();

    ProcessInstance pi1 = runtimeService.startProcessInstanceByKey("processDefinition1");
    ProcessInstance pi2 = runtimeService.startProcessInstanceByKey("processDefinition2");


    // when
    List<Task> tasks = taskService.createTaskQuery()
      .startOr()
        .processInstanceId(pi1.getId())
        .processInstanceId(pi2.getId())
      .endOr()
      .list();

    // then
    assertEquals(2, tasks.size());

    repositoryService.deleteDeployment(deploymentId1, true);
    repositoryService.deleteDeployment(deploymentId2, true);
  }

  @Test
  public void testQueryTaskProcessInstanceBusinessKeys() {
    // given
    BpmnModelInstance processDefinition1 = Bpmn.createExecutableProcess("processDefinition1")
      .startEvent()
        .userTask()
      .endEvent()
      .done();

    String deploymentId1 = repositoryService.createDeployment().addModelInstance("foo.bpmn", processDefinition1).deploy().getId();

    BpmnModelInstance processDefinition2 = Bpmn.createExecutableProcess("processDefinition2")
      .startEvent()
        .userTask()
      .endEvent()
      .done();

    String deploymentId2 = repositoryService.createDeployment().addModelInstance("foo.bpmn", processDefinition2).deploy().getId();

    ProcessInstance pi1 = runtimeService.startProcessInstanceByKey("processDefinition1", "businessKey1");
    ProcessInstance pi2 = runtimeService.startProcessInstanceByKey("processDefinition2", "businessKey2");


    // when
    List<Task> tasks = taskService.createTaskQuery()
      .startOr()
        .processInstanceBusinessKey(pi1.getBusinessKey())
        .processInstanceBusinessKey(pi2.getBusinessKey())
      .endOr()
      .list();

    // then
    assertEquals(2, tasks.size());

    repositoryService.deleteDeployment(deploymentId1, true);
    repositoryService.deleteDeployment(deploymentId2, true);
  }

  @Test
  public void testQueryTaskProcessInstanceBusinessKeysLike() {
    // given
    BpmnModelInstance processDefinition1 = Bpmn.createExecutableProcess("processDefinition1")
      .startEvent()
        .userTask()
      .endEvent()
      .done();

    String deploymentId1 = repositoryService.createDeployment().addModelInstance("foo.bpmn", processDefinition1).deploy().getId();

    BpmnModelInstance processDefinition2 = Bpmn.createExecutableProcess("processDefinition2")
      .startEvent()
        .userTask()
      .endEvent()
      .done();

    String deploymentId2 = repositoryService.createDeployment().addModelInstance("foo.bpmn", processDefinition2).deploy().getId();

    runtimeService.startProcessInstanceByKey("processDefinition1", "businessKey1 of Task");
    runtimeService.startProcessInstanceByKey("processDefinition2", "businessKey2 of Task");


    // when
    List<Task> tasks = taskService.createTaskQuery()
      .startOr()
        .processInstanceBusinessKeyLike("businessKey1%")
        .processInstanceBusinessKeyLike("businessKey2%")
      .endOr()
      .list();

    // then
    assertEquals(2, tasks.size());

    repositoryService.deleteDeployment(deploymentId1, true);
    repositoryService.deleteDeployment(deploymentId2, true);
  }

  @Test
  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn"})
  public void testQueryTaskCaseInstanceIds() {
    // given
    CaseInstance ci1 = caseService
      .withCaseDefinition(repositoryService.createCaseDefinitionQuery().singleResult().getId())
      .create();

    CaseInstance ci2 = caseService
      .withCaseDefinition(repositoryService.createCaseDefinitionQuery().singleResult().getId())
      .create();

    caseService
      .withCaseExecution(caseService.createCaseExecutionQuery().caseInstanceId(ci1.getId()).activityId("PI_HumanTask_1").singleResult().getId())
      .manualStart();

    caseService
      .withCaseExecution(caseService.createCaseExecutionQuery().caseInstanceId(ci2.getId()).activityId("PI_HumanTask_1").singleResult().getId())
      .manualStart();

    // when
    List<Task> tasks = taskService.createTaskQuery()
      .startOr()
        .caseInstanceId(ci1.getId())
        .caseInstanceId(ci2.getId())
      .endOr()
      .list();

    // then
    assertEquals(2, tasks.size());
  }

  @Test
  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn"})
  public void testQueryCaseInstanceBusinessKeys() {
    // given
    CaseInstance ci1 = caseService
      .withCaseDefinition(repositoryService.createCaseDefinitionQuery().singleResult().getId())
      .businessKey("businessKey 1")
      .create();

    CaseInstance ci2 = caseService
      .withCaseDefinition(repositoryService.createCaseDefinitionQuery().singleResult().getId())
      .businessKey("businessKey 2")
      .create();

    caseService
      .withCaseExecution(caseService.createCaseExecutionQuery().caseInstanceId(ci1.getId()).activityId("PI_HumanTask_1").singleResult().getId())
      .manualStart();

    caseService
      .withCaseExecution(caseService.createCaseExecutionQuery().caseInstanceId(ci2.getId()).activityId("PI_HumanTask_1").singleResult().getId())
      .manualStart();

    // when
    List<Task> tasks = taskService.createTaskQuery()
      .startOr()
        .caseInstanceBusinessKey(ci1.getBusinessKey())
        .caseInstanceBusinessKey(ci2.getBusinessKey())
      .endOr()
      .list();

    // then
    assertEquals(2, tasks.size());
  }

  @Test
  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn"})
  public void testQueryCaseInstanceBusinessKeysLike() {
    // given
    CaseInstance ci1 = caseService
      .withCaseDefinition(repositoryService.createCaseDefinitionQuery().singleResult().getId())
      .businessKey("businessKey 1 of caseInstance")
      .create();

    CaseInstance ci2 = caseService
      .withCaseDefinition(repositoryService.createCaseDefinitionQuery().singleResult().getId())
      .businessKey("businessKey 2 of caseInstance")
      .create();

    caseService
      .withCaseExecution(caseService.createCaseExecutionQuery().caseInstanceId(ci1.getId()).activityId("PI_HumanTask_1").singleResult().getId())
      .manualStart();

    caseService
      .withCaseExecution(caseService.createCaseExecutionQuery().caseInstanceId(ci2.getId()).activityId("PI_HumanTask_1").singleResult().getId())
      .manualStart();

    // when
    List<Task> tasks = taskService.createTaskQuery()
      .startOr()
        .caseInstanceBusinessKeyLike("businessKey 1%")
        .caseInstanceBusinessKeyLike("businessKey 2%")
      .endOr()
      .list();

    // then
    assertEquals(2, tasks.size());
  }

  @Test
  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn"})
  public void testQueryCaseInstanceExecutionIds() {
    // given
    CaseInstance ci1 = caseService
      .withCaseDefinition(repositoryService.createCaseDefinitionQuery().singleResult().getId())
      .create();

    CaseInstance ci2 = caseService
      .withCaseDefinition(repositoryService.createCaseDefinitionQuery().singleResult().getId())
      .create();

    CaseExecution ce1 = caseService.createCaseExecutionQuery().caseInstanceId(ci1.getId()).activityId("PI_HumanTask_1").singleResult();

    caseService
      .withCaseExecution(ce1.getId())
      .manualStart();

    CaseExecution ce2 = caseService.createCaseExecutionQuery().caseInstanceId(ci2.getId()).activityId("PI_HumanTask_1").singleResult();

    caseService
      .withCaseExecution(ce2.getId())
      .manualStart();

    // when
    List<Task> tasks = taskService.createTaskQuery()
      .startOr()
        .caseExecutionId(ce1.getId())
        .caseExecutionId(ce2.getId())
      .endOr()
      .list();

    // then
    assertEquals(2, tasks.size());
  }

  @Test
  public void testQueryCreateTimes() throws ParseException {
    // given
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");
    ClockUtil.setCurrentTime(sdf.parse("06/06/2006 06:06:06.000"));

    Task task1 = taskService.newTask();
    taskService.saveTask(task1);

    Task task2 = taskService.newTask();
    taskService.saveTask(task2);

    // when
    List<Task> tasks = taskService.createTaskQuery()
      .startOr()
        .taskCreatedOn(task1.getCreateTime())
        .taskCreatedOn(task2.getCreateTime())
      .endOr()
      .list();

    // then
    assertEquals(2, tasks.size());
  }

  @Test
  @Deployment(resources="org/camunda/bpm/engine/test/api/task/taskDefinitionProcess.bpmn20.xml")
  public void testQueryTaskDefinitionKeys() {
    // given
    runtimeService.startProcessInstanceByKey("taskDefinitionKeyProcess");

    // when
    List<Task> tasks = taskService.createTaskQuery()
      .startOr()
        .taskDefinitionKey("taskKey_1")
        .taskDefinitionKey("taskKey_123")
      .endOr()
      .list();

    // then
    assertEquals(2, tasks.size());
  }

  @Test
  @Deployment(resources="org/camunda/bpm/engine/test/api/task/taskDefinitionProcess.bpmn20.xml")
  public void testQueryTaskDefinitionKeysLike() {
    // given
    runtimeService.startProcessInstanceByKey("taskDefinitionKeyProcess");

    // when
    List<Task> tasks = taskService.createTaskQuery()
      .startOr()
        .taskDefinitionKeyLike("%1")
        .taskDefinitionKeyLike("%123")
      .endOr()
      .list();

    // then
    assertEquals(2, tasks.size());
  }

  @Test
  public void testQueryParentTaskIds() {
    // given
    String parentTaskId1 = "parentTask 1";
    Task parent1 = taskService.newTask(parentTaskId1);
    taskService.saveTask(parent1);

    String parentTaskId2 = "parentTask 2";
    Task parent2 = taskService.newTask(parentTaskId2);
    taskService.saveTask(parent2);

    Task sub1 = taskService.newTask("subTask1");
    sub1.setParentTaskId(parentTaskId1);
    taskService.saveTask(sub1);

    Task sub2 = taskService.newTask("subTask2");
    sub2.setParentTaskId(parentTaskId2);
    taskService.saveTask(sub2);

    // when
    List<Task> tasks = taskService.createTaskQuery()
      .startOr()
        .taskParentTaskId(parentTaskId1)
        .taskParentTaskId(parentTaskId2)
      .endOr()
      .list();

    // then
    assertEquals(2, tasks.size());
    taskService.deleteTasks(Arrays.asList(parentTaskId1, parentTaskId2), true);
  }

  @Test
  public void testQueryTaskProcessDefinitionIds() {
    // given
    BpmnModelInstance processDefinition1 = Bpmn.createExecutableProcess("processDefinition1")
      .startEvent()
        .userTask()
      .endEvent()
      .done();

    String deploymentId1 = repositoryService.createDeployment().addModelInstance("foo.bpmn", processDefinition1).deploy().getId();

    BpmnModelInstance processDefinition2 = Bpmn.createExecutableProcess("processDefinition2")
      .startEvent()
        .userTask()
      .endEvent()
      .done();

    String deploymentId2 = repositoryService.createDeployment().addModelInstance("foo.bpmn", processDefinition2).deploy().getId();

    ProcessInstance pi1 = runtimeService.startProcessInstanceByKey("processDefinition1");
    ProcessInstance pi2 = runtimeService.startProcessInstanceByKey("processDefinition2");

    // when
    List<Task> tasks = taskService.createTaskQuery()
      .startOr()
        .processDefinitionId(pi1.getProcessDefinitionId())
        .processDefinitionId(pi2.getProcessDefinitionId())
      .endOr()
      .list();

    // then
    assertEquals(2, tasks.size());

    repositoryService.deleteDeployment(deploymentId1, true);
    repositoryService.deleteDeployment(deploymentId2, true);
  }

  @Test
  public void testQueryTaskProcessDefinitionKeys() {
    // given
    BpmnModelInstance processDefinition1 = Bpmn.createExecutableProcess("processDefinition1")
      .startEvent()
        .userTask()
      .endEvent()
      .done();

    String deploymentId1 = repositoryService.createDeployment().addModelInstance("foo.bpmn", processDefinition1).deploy().getId();

    BpmnModelInstance processDefinition2 = Bpmn.createExecutableProcess("processDefinition2")
      .startEvent()
        .userTask()
      .endEvent()
      .done();

    String deploymentId2 = repositoryService.createDeployment().addModelInstance("foo.bpmn", processDefinition2).deploy().getId();

    runtimeService.startProcessInstanceByKey("processDefinition1");
    runtimeService.startProcessInstanceByKey("processDefinition2");

    // when
    List<Task> tasks = taskService.createTaskQuery()
      .startOr()
        .processDefinitionKey("processDefinition1")
        .processDefinitionKey("processDefinition2")
      .endOr()
      .list();

    // then
    assertEquals(2, tasks.size());

    repositoryService.deleteDeployment(deploymentId1, true);
    repositoryService.deleteDeployment(deploymentId2, true);
  }

  @Test
  public void testQueryTaskProcessDefinitionNames() {
    // given
    BpmnModelInstance processDefinition1 = Bpmn.createExecutableProcess("processDefinition1")
      .name("process def name 1")
      .startEvent()
        .userTask()
      .endEvent()
      .done();

    String deploymentId1 = repositoryService.createDeployment().addModelInstance("foo.bpmn", processDefinition1).deploy().getId();

    BpmnModelInstance processDefinition2 = Bpmn.createExecutableProcess("processDefinition2")
      .name("process def name 2")
      .startEvent()
        .userTask()
      .endEvent()
      .done();

    String deploymentId2 = repositoryService.createDeployment().addModelInstance("foo.bpmn", processDefinition2).deploy().getId();

    runtimeService.startProcessInstanceByKey("processDefinition1");
    runtimeService.startProcessInstanceByKey("processDefinition2");

    // when
    List<Task> tasks = taskService.createTaskQuery()
      .startOr()
        .processDefinitionName("process def name 1")
        .processDefinitionName("process def name 2")
      .endOr()
      .list();

    // then
    assertEquals(2, tasks.size());

    repositoryService.deleteDeployment(deploymentId1, true);
    repositoryService.deleteDeployment(deploymentId2, true);
  }

  @Test
  public void testQueryTaskProcessDefinitionNamesLike() {
    // given
    BpmnModelInstance processDefinition1 = Bpmn.createExecutableProcess("processDefinition1")
      .name("process def name 1")
      .startEvent()
        .userTask()
      .endEvent()
      .done();

    String deploymentId1 = repositoryService.createDeployment().addModelInstance("foo.bpmn", processDefinition1).deploy().getId();

    BpmnModelInstance processDefinition2 = Bpmn.createExecutableProcess("processDefinition2")
      .name("process def name 2")
      .startEvent()
        .userTask()
      .endEvent()
      .done();

    String deploymentId2 = repositoryService.createDeployment().addModelInstance("foo.bpmn", processDefinition2).deploy().getId();

    runtimeService.startProcessInstanceByKey("processDefinition1");
    runtimeService.startProcessInstanceByKey("processDefinition2");

    // when
    List<Task> tasks = taskService.createTaskQuery()
      .startOr()
        .processDefinitionNameLike("%1")
        .processDefinitionNameLike("%2")
      .endOr()
      .list();

    // then
    assertEquals(2, tasks.size());

    repositoryService.deleteDeployment(deploymentId1, true);
    repositoryService.deleteDeployment(deploymentId2, true);
  }

  @Test
  @Deployment(resources={
    "org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn",
    "org/camunda/bpm/engine/test/api/cmmn/oneTaskCase2.cmmn"})
  public void testQueryCaseDefinitionIds() {
    // given
    CaseInstance ci1 = caseService
      .withCaseDefinition(repositoryService.createCaseDefinitionQuery().caseDefinitionKey("oneTaskCase").singleResult().getId())
      .create();

    CaseInstance ci2 = caseService
      .withCaseDefinition(repositoryService.createCaseDefinitionQuery().caseDefinitionKey("oneTaskCase2").singleResult().getId())
      .create();

    // when
    List<Task> tasks = taskService.createTaskQuery()
      .startOr()
        .caseDefinitionId(ci1.getCaseDefinitionId())
        .caseDefinitionId(ci2.getCaseDefinitionId())
      .endOr()
      .list();

    // then
    assertEquals(2, tasks.size());
  }

  @Test
  @Deployment(resources={
    "org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn",
    "org/camunda/bpm/engine/test/api/cmmn/oneTaskCase2.cmmn"})
  public void testQueryCaseDefinitionKeys() {
    // given
    caseService
      .withCaseDefinition(repositoryService.createCaseDefinitionQuery().caseDefinitionKey("oneTaskCase").singleResult().getId())
      .create();

    caseService
      .withCaseDefinition(repositoryService.createCaseDefinitionQuery().caseDefinitionKey("oneTaskCase2").singleResult().getId())
      .create();

    // when
    List<Task> tasks = taskService.createTaskQuery()
      .startOr()
        .caseDefinitionKey("oneTaskCase")
        .caseDefinitionKey("oneTaskCase2")
      .endOr()
      .list();

    // then
    assertEquals(2, tasks.size());
  }

  @Test
  @Deployment(resources={
    "org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn",
    "org/camunda/bpm/engine/test/api/cmmn/oneTaskCase2.cmmn"})
  public void testQueryCaseDefinitionNames() {
    // given
    caseService
      .withCaseDefinition(repositoryService.createCaseDefinitionQuery().caseDefinitionKey("oneTaskCase").singleResult().getId())
      .create();

    caseService
      .withCaseDefinition(repositoryService.createCaseDefinitionQuery().caseDefinitionKey("oneTaskCase2").singleResult().getId())
      .create();

    // when
    List<Task> tasks = taskService.createTaskQuery()
      .startOr()
        .caseDefinitionName("One Task Case")
        .caseDefinitionName("One")
      .endOr()
      .list();

    // then
    assertEquals(2, tasks.size());
  }

  @Test
  @Deployment(resources={
    "org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn",
    "org/camunda/bpm/engine/test/api/cmmn/oneTaskCase2.cmmn"})
  public void testQueryCaseDefinitionNamesLike() {
    // given
    caseService
      .withCaseDefinition(repositoryService.createCaseDefinitionQuery().caseDefinitionKey("oneTaskCase").singleResult().getId())
      .create();

    caseService
      .withCaseDefinition(repositoryService.createCaseDefinitionQuery().caseDefinitionKey("oneTaskCase2").singleResult().getId())
      .create();

    // when
    List<Task> tasks = taskService.createTaskQuery()
      .startOr()
        .caseDefinitionNameLike("One Task%")
        .caseDefinitionNameLike("One")
      .endOr()
      .list();

    // then
    assertEquals(2, tasks.size());
  }

  @Test
  public void testQueryTaskDueDates() throws ParseException {
    // given
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");

    Task task1 = taskService.newTask();
    task1.setDueDate(sdf.parse("06/06/2006 06:06:06.000"));
    taskService.saveTask(task1);

    Task task2 = taskService.newTask();
    task2.setDueDate(sdf.parse("07/07/2007 07:07:07.000"));
    taskService.saveTask(task2);

    // when
    List<Task> tasks = taskService.createTaskQuery()
      .startOr()
        .dueDate(task1.getDueDate())
        .dueDate(task2.getDueDate())
      .endOr()
      .list();

    // then
    assertEquals(2, tasks.size());
    taskService.deleteTasks(Arrays.asList(task1.getId(), task2.getId()), true);
  }

  @Test
  public void testQueryTaskFollowUpDates() throws ParseException {
    // given
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");

    Task task1 = taskService.newTask();
    task1.setFollowUpDate(sdf.parse("06/06/2006 06:06:06.000"));
    taskService.saveTask(task1);

    Task task2 = taskService.newTask();
    task2.setFollowUpDate(sdf.parse("07/07/2007 07:07:07.000"));
    taskService.saveTask(task2);

    // when
    List<Task> tasks = taskService.createTaskQuery()
      .startOr()
        .followUpDate(task1.getFollowUpDate())
        .followUpDate(task2.getFollowUpDate())
      .endOr()
      .list();

    // then
    assertEquals(2, tasks.size());
    taskService.deleteTasks(Arrays.asList(task1.getId(), task2.getId()), true);
  }
  @Test
  @Deployment(resources={
    "org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn",
    "org/camunda/bpm/engine/test/api/cmmn/oneTaskCase2.cmmn"})
  public void testQueryLongCaseInstanceVariableValueEquals() {
    // given
    caseService
      .withCaseDefinition(repositoryService.createCaseDefinitionQuery().caseDefinitionKey("oneTaskCase").singleResult().getId())
      .setVariable("aLongValue", 789L)
      .create();

    caseService
      .withCaseDefinition(repositoryService.createCaseDefinitionQuery().caseDefinitionKey("oneTaskCase2").singleResult().getId())
      .setVariable("anEvenLongerValue", 1000L)
      .create();

    TaskQuery query = taskService.createTaskQuery()
      .startOr()
        .caseInstanceVariableValueEquals("aLongValue", 789L)
        .caseInstanceVariableValueEquals("anEvenLongerValue", 1000L)
      .endOr();

    assertEquals(2, query.count());
  }

  @Test
  public void testQueryTaskDelegationStates() {
    // given
    Task task1 = taskService.newTask();
    task1.setName("Task 1");
    task1.setDelegationState(DelegationState.PENDING);
    taskService.saveTask(task1);

    Task task2 = taskService.newTask();
    task2.setName("Task 2");
    task2.setDelegationState(DelegationState.RESOLVED);
    taskService.saveTask(task2);

    Task task3 = taskService.newTask();
    task3.setName("Task 3");
    task3.setDelegationState(null);
    taskService.saveTask(task3);

    // when
    List<Task> tasks = taskService.createTaskQuery()
      .taskNameLike("Task%")
      .startOr()
        .taskDelegationState(DelegationState.PENDING)
        .taskDelegationState(DelegationState.RESOLVED)
        .taskDelegationState(null)
      .endOr()
      .list();

    // then
    assertEquals(3, tasks.size());
    taskService.deleteTasks(Arrays.asList(task1.getId(), task2.getId(), task3.getId()), true);
  }

  @Test
  public void testQuerySuspensionState() {
    // given
    BpmnModelInstance processDefinition1 = Bpmn.createExecutableProcess("processDefinition1")
      .startEvent()
        .userTask()
          .name("Task 1")
      .endEvent()
      .done();

    String deploymentId1 = repositoryService.createDeployment().addModelInstance("foo.bpmn", processDefinition1).deploy().getId();

    BpmnModelInstance processDefinition2 = Bpmn.createExecutableProcess("processDefinition2")
      .startEvent()
        .userTask()
          .name("Task 2")
      .endEvent()
      .done();

    String deploymentId2 = repositoryService.createDeployment().addModelInstance("foo.bpmn", processDefinition2).deploy().getId();

    ProcessInstance pi1 = runtimeService.startProcessInstanceByKey("processDefinition1");
    runtimeService.startProcessInstanceByKey("processDefinition2");
    runtimeService.suspendProcessInstanceById(pi1.getId());

    // when
    List<Task> tasks = taskService.createTaskQuery()
      .taskNameLike("Task%")
      .startOr()
        .suspended()
        .active()
      .endOr()
      .list();

    // then
    assertEquals(2, tasks.size());

    repositoryService.deleteDeployment(deploymentId1, true);
    repositoryService.deleteDeployment(deploymentId2, true);
  }

  @Test
  public void testQueryTaskInvolvedUsers() {
    // given
    Task task1 = taskService.newTask("Task1");
    taskService.saveTask(task1);
    taskService.addCandidateUser(task1.getId(), "John Doe");

    Task task2 = taskService.newTask("Task2");
    taskService.saveTask(task2);
    taskService.addCandidateUser(task2.getId(), "Jack Camundo");

    // when
    List<Task> tasks = taskService.createTaskQuery()
      .startOr()
        .taskInvolvedUser("John Doe")
        .taskInvolvedUser("Jack Camundo")
      .endOr()
      .list();

    // then
    assertEquals(2, tasks.size());
    taskService.deleteTasks(Arrays.asList(task1.getId(), task2.getId()), true);
  }

  @Test
  public void testQueryCandidateUsers() {
    // given
    BpmnModelInstance processDefinition1 = Bpmn.createExecutableProcess("processDefinition1")
      .startEvent()
        .userTask()
          .camundaCandidateUsers("John Doe")
      .endEvent()
      .done();

    String deploymentId1 = repositoryService.createDeployment().addModelInstance("foo.bpmn", processDefinition1).deploy().getId();

    BpmnModelInstance processDefinition2 = Bpmn.createExecutableProcess("processDefinition2")
      .startEvent()
        .userTask()
          .camundaCandidateUsers("Jack Camundo")
      .endEvent()
      .done();

    String deploymentId2 = repositoryService.createDeployment().addModelInstance("foo.bpmn", processDefinition2).deploy().getId();

    runtimeService.startProcessInstanceByKey("processDefinition1");
    runtimeService.startProcessInstanceByKey("processDefinition2");

    // when
    List<Task> tasks = taskService.createTaskQuery()
      .startOr()
        .taskCandidateUser("John Doe")
        .taskCandidateUser("Jack Camundo")
      .endOr()
      .list();

    // then
    assertEquals(2, tasks.size());

    repositoryService.deleteDeployment(deploymentId1, true);
    repositoryService.deleteDeployment(deploymentId2, true);
  }

  @Test
  public void testQueryCandidateGroups() {
    // given
    BpmnModelInstance processDefinition1 = Bpmn.createExecutableProcess("processDefinition1")
      .startEvent()
        .userTask()
          .camundaCandidateGroups("sales")
      .endEvent()
      .done();

    String deploymentId1 = repositoryService.createDeployment().addModelInstance("foo.bpmn", processDefinition1).deploy().getId();

    BpmnModelInstance processDefinition2 = Bpmn.createExecutableProcess("processDefinition2")
      .startEvent()
        .userTask()
          .camundaCandidateGroups("marketing")
      .endEvent()
      .done();

    String deploymentId2 = repositoryService.createDeployment().addModelInstance("foo.bpmn", processDefinition2).deploy().getId();

    runtimeService.startProcessInstanceByKey("processDefinition1");
    runtimeService.startProcessInstanceByKey("processDefinition2");

    // when
    List<Task> tasks = taskService.createTaskQuery()
      .startOr()
        .taskCandidateGroup("sales")
        .taskCandidateGroup("marketing")
      .endOr()
      .list();

    // then
    assertEquals(2, tasks.size());

    repositoryService.deleteDeployment(deploymentId1, true);
    repositoryService.deleteDeployment(deploymentId2, true);
  }

  @Test
  public void testQueryCandidateGroupIn() {
    // given
    Task task1 = taskService.newTask();
    taskService.saveTask(task1);
    taskService.addCandidateGroup(task1.getId(), "sales");

    Task task2 = taskService.newTask();
    taskService.saveTask(task2);
    taskService.addCandidateGroup(task2.getId(), "marketing");

    Task task3 = taskService.newTask();
    taskService.saveTask(task3);
    taskService.addCandidateGroup(task3.getId(), "controlling");

    Task task4 = taskService.newTask();
    taskService.saveTask(task4);
    taskService.addCandidateGroup(task4.getId(), "management");

    // when
    List<Task> tasks = taskService.createTaskQuery()
      .startOr()
        .taskCandidateGroupIn(Arrays.asList("sales", "marketing"))
        .taskCandidateGroupIn(Arrays.asList("controlling", "management"))
      .endOr()
      .list();

    // then
    assertEquals(4, tasks.size());

    taskService.deleteTasks(Arrays.asList(task1.getId(), task2.getId(), task3.getId(), task4.getId()), true);
  }

  @Test
  public void testQueryWithOrWithoutCandidateUsers() {
    // given
    BpmnModelInstance processDefinitionWithoutCandidateUser = Bpmn.createExecutableProcess("processDefinitionWithoutCandidateUser")
      .startEvent()
        .userTask()
          .camundaCandidateGroups("sales")
      .endEvent()
      .done();

    String deploymentId1 = repositoryService.createDeployment().addModelInstance("foo.bpmn", processDefinitionWithoutCandidateUser).deploy().getId();

    BpmnModelInstance processDefinitionWithCandidateUser = Bpmn.createExecutableProcess("processDefinitionWithCandidateUser")
      .startEvent()
        .userTask()
          .camundaCandidateUsers("John Doe")
      .endEvent()
      .done();

    String deploymentId2 = repositoryService.createDeployment().addModelInstance("foo.bpmn", processDefinitionWithCandidateUser).deploy().getId();

    runtimeService.startProcessInstanceByKey("processDefinitionWithoutCandidateUser");
    runtimeService.startProcessInstanceByKey("processDefinitionWithCandidateUser");

    // when
    List<Task> tasks = taskService.createTaskQuery()
      .startOr()
        .withCandidateUsers()
        .withoutCandidateUsers()
      .endOr()
      .list();

    // then
    assertEquals(2, tasks.size());

    repositoryService.deleteDeployment(deploymentId1, true);
    repositoryService.deleteDeployment(deploymentId2, true);
  }

  @Test
  public void testQueryWithOrWithoutCandidateGroups() {
    // given
    BpmnModelInstance processDefinitionWithoutCandidateUser = Bpmn.createExecutableProcess("processDefinitionWithoutCandidateUser")
      .startEvent()
        .userTask()
          .camundaCandidateGroups("sales")
      .endEvent()
      .done();

    String deploymentId1 = repositoryService.createDeployment().addModelInstance("foo.bpmn", processDefinitionWithoutCandidateUser).deploy().getId();

    BpmnModelInstance processDefinitionWithCandidateUser = Bpmn.createExecutableProcess("processDefinitionWithCandidateUser")
      .startEvent()
        .userTask()
          .camundaCandidateUsers("John Doe")
      .endEvent()
      .done();

    String deploymentId2 = repositoryService.createDeployment().addModelInstance("foo.bpmn", processDefinitionWithCandidateUser).deploy().getId();

    runtimeService.startProcessInstanceByKey("processDefinitionWithoutCandidateUser");
    runtimeService.startProcessInstanceByKey("processDefinitionWithCandidateUser");

    // when
    List<Task> tasks = taskService.createTaskQuery()
      .startOr()
        .withCandidateGroups()
        .withoutCandidateGroups()
      .endOr()
      .list();

    // then
    assertEquals(2, tasks.size());

    repositoryService.deleteDeployment(deploymentId1, true);
    repositoryService.deleteDeployment(deploymentId2, true);
  }

  @Test
  public void testQueryWithCandidateUsersOrWithCandidateGroups() {
    // given
    BpmnModelInstance processDefinitionWithoutCandidateUser = Bpmn.createExecutableProcess("processDefinitionWithoutCandidateUser")
      .startEvent()
        .userTask()
          .camundaCandidateGroups("sales")
      .endEvent()
      .done();

    String deploymentId1 = repositoryService.createDeployment().addModelInstance("foo.bpmn", processDefinitionWithoutCandidateUser).deploy().getId();

    BpmnModelInstance processDefinitionWithCandidateUser = Bpmn.createExecutableProcess("processDefinitionWithCandidateUser")
      .startEvent()
        .userTask()
          .camundaCandidateUsers("John Doe")
      .endEvent()
      .done();

    String deploymentId2 = repositoryService.createDeployment().addModelInstance("foo.bpmn", processDefinitionWithCandidateUser).deploy().getId();

    runtimeService.startProcessInstanceByKey("processDefinitionWithoutCandidateUser");
    runtimeService.startProcessInstanceByKey("processDefinitionWithCandidateUser");

    // when
    List<Task> tasks = taskService.createTaskQuery()
      .startOr()
        .withCandidateUsers()
        .withCandidateGroups()
      .endOr()
      .list();

    // then
    assertEquals(2, tasks.size());

    repositoryService.deleteDeployment(deploymentId1, true);
    repositoryService.deleteDeployment(deploymentId2, true);
  }

  /*@Test
  public void testQueryWithOrWithoutCandidateGroups() {
    // given
    BpmnModelInstance processDefinitionWithoutCandidateUser = Bpmn.createExecutableProcess("processDefinitionWithoutCandidateUser")
      .startEvent()
        .userTask()
          .camundaCandidateGroups("sales")
      .endEvent()
      .done();

    String deploymentId1 = repositoryService.createDeployment().addModelInstance("foo.bpmn", processDefinitionWithoutCandidateUser).deploy().getId();

    BpmnModelInstance processDefinitionWithCandidateUser = Bpmn.createExecutableProcess("processDefinitionWithCandidateUser")
      .startEvent()
        .userTask()
          .camundaCandidateUsers("John Doe")
      .endEvent()
      .done();

    String deploymentId2 = repositoryService.createDeployment().addModelInstance("foo.bpmn", processDefinitionWithCandidateUser).deploy().getId();

    runtimeService.startProcessInstanceByKey("processDefinitionWithoutCandidateUser");
    runtimeService.startProcessInstanceByKey("processDefinitionWithCandidateUser");

    // when
    List<Task> tasks = taskService.createTaskQuery()
      .startOr()
        .withCandidateGroups()
        .withoutCandidateGroups()
      .endOr()
      .list();

    // then
    assertEquals(2, tasks.size());

    repositoryService.deleteDeployment(deploymentId1, true);
    repositoryService.deleteDeployment(deploymentId2, true);
  }*/

  @Test
  public void testQueryOr() {
    // given
    List<Task> tasks = taskService.createTaskQuery()
      .startOr()
        .taskNameLike("management%")
        .startAnd()
          .taskUnassigned()
          .taskDescription("gonzo_description")
        .endAnd()
      .endOr()
      .list();

    for (Task task: tasks) {
      System.out.println("name: " + task.getName() + ", assignee: " + task.getAssignee() + ", description: " + task.getDescription());
    }

    // then
    assertEquals(3, tasks.size());
  }

  @Test
  public void testLogicalExpressionQuery() {
    // given
    List<Task> tasks = taskService.createTaskQuery()
      .taskNameLike("management%")
      .taskName("management")
      .startAnd()
        .taskNameLike("management%")
        .taskName("management")
        .startOr()
          .taskNameLike("management%")
          .taskName("management")
          .startAnd()
            .taskUnassigned()
            .taskName("management")
          .endAnd()
        .endOr()
      .endAnd()
      .startAnd()
        .startOr()
          .taskNameLike("management%")
        .endOr()
        .startOr()
          .taskNameLike("management%")
        .endOr()
        .taskNameLike("management%")
        .taskName("management")
        .startOr()
          .taskNameLike("management%")
          .taskName("management")
          .startOr()
            .taskNameLike("management%")
            .taskName("management")
            .startOr()
              .taskNameLike("management%")
              .taskName("management")
              .startAnd()
                .taskNameLike("management%")
                .taskName("management")
              .endAnd()
            .endOr()
          .endOr()
        .endOr()
      .endAnd()
      .list();

    System.out.println(tasks);
  }

}
