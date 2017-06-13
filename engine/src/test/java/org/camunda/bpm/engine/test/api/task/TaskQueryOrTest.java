package org.camunda.bpm.engine.test.api.task;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.BadUserRequestException;
import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.impl.TaskQueryImpl;
import org.camunda.bpm.engine.impl.test.PluggableProcessEngineTestCase;
import org.camunda.bpm.engine.impl.util.ClockUtil;
import org.camunda.bpm.engine.runtime.CaseExecution;
import org.camunda.bpm.engine.runtime.CaseInstance;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.task.TaskQuery;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.bpm.engine.variable.value.FileValue;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;

/**
 * Created by tasso on 09.06.17.
 */
public class TaskQueryOrTest extends PluggableProcessEngineTestCase {
  private List<String> taskIds;

  // The range of Oracle's NUMBER field is limited to ~10e+125
  // which is below Double.MAX_VALUE, so we only test with the following
  // max value
  protected static final double MAX_DOUBLE_VALUE = 10E+124;

  @Override
  public void setUp() throws Exception {

    identityService.saveUser(identityService.newUser("kermit"));
    identityService.saveUser(identityService.newUser("gonzo"));
    identityService.saveUser(identityService.newUser("fozzie"));

    identityService.saveGroup(identityService.newGroup("management"));
    identityService.saveGroup(identityService.newGroup("accountancy"));

    identityService.createMembership("kermit", "management");
    identityService.createMembership("kermit", "accountancy");
    identityService.createMembership("fozzie", "management");

    taskIds = generateTestTasks();
  }

  @Override
  public void tearDown() throws Exception {
    identityService.deleteGroup("accountancy");
    identityService.deleteGroup("management");
    identityService.deleteUser("fozzie");
    identityService.deleteUser("gonzo");
    identityService.deleteUser("kermit");
    taskService.deleteTasks(taskIds, true);
  }

  public void testQueryByCandidateUserOrCandidateGroup() {
    // kermit is candidate for 12 tasks, two of them are already assigned
    TaskQuery query = taskService.createTaskQuery()
      .startOr()
      .taskCandidateUser("kermit")
      .taskCandidateGroupIn(Arrays.asList("management", "accountancy", "FOO"))
      .endOr();

    for(Task q: query.list()) {
      System.out.println(q.getId() + " " + q.getName() + " " + q.getAssignee());
    }
    //assertEquals(10, query.list().size());
  }

  public void testQueryByCandidateUserOr() {
    // kermit is candidate for 12 tasks, two of them are already assigned
    TaskQuery query = taskService.createTaskQuery().startOr().taskCandidateUser("kermit").endOr();
    assertEquals(10, query.count());
    assertEquals(10, query.list().size());
    try {
      query.singleResult();
      fail("expected exception");
    } catch (ProcessEngineException e) {
      // OK
    }

    // test including assigned tasks
    query = taskService.createTaskQuery().startOr().taskCandidateUser("kermit").includeAssignedTasks().endOr();
    assertEquals(12, query.count());
    assertEquals(12, query.list().size());

    // fozzie is candidate for one task and her groups are candidate for 2 tasks, one of them is already assigned
    query = taskService.createTaskQuery().startOr().taskCandidateUser("fozzie").endOr();
    assertEquals(2, query.count());
    assertEquals(2, query.list().size());
    try {
      query.singleResult();
      fail("expected exception");
    } catch (ProcessEngineException e) {
      // OK
    }

    // test including assigned tasks
    query = taskService.createTaskQuery().startOr().taskCandidateUser("fozzie").includeAssignedTasks().endOr();
    assertEquals(3, query.count());
    assertEquals(3, query.list().size());

    // gonzo is candidate for one task, which is already assinged
    query = taskService.createTaskQuery().startOr().taskCandidateUser("gonzo").endOr();
    assertEquals(0, query.count());
    assertEquals(0, query.list().size());

    // test including assigned tasks
    query = taskService.createTaskQuery().startOr().taskCandidateUser("gonzo").includeAssignedTasks().endOr();
    assertEquals(1, query.count());
    assertEquals(1, query.list().size());
  }

  public void testQueryByNullCandidateUserOr() {
    try {
      taskService.createTaskQuery().startOr().taskCandidateUser(null).endOr().list();
      fail();
    } catch(ProcessEngineException e) {}
  }

  public void testQueryByIncludeAssignedTasksWithMissingCandidateUserOrGroupOr() {
    try {
      taskService.createTaskQuery().startOr().includeAssignedTasks().endOr();
      fail("expected exception");
    } catch (ProcessEngineException e) {
      // OK
    }
  }

  public void testQueryByCandidateGroupOr() {
    // management group is candidate for 3 tasks, one of them is already assigned
    TaskQuery query = taskService.createTaskQuery().startOr().taskCandidateGroup("management").endOr();
    assertEquals(2, query.count());
    assertEquals(2, query.list().size());
    try {
      query.singleResult();
      fail("expected exception");
    } catch (ProcessEngineException e) {
      // OK
    }

    // test including assigned tasks
    query = taskService.createTaskQuery().startOr().taskCandidateGroup("management").includeAssignedTasks().endOr();
    assertEquals(3, query.count());
    assertEquals(3, query.list().size());


    // accountancy group is candidate for 3 tasks, one of them is already assigned
    query = taskService.createTaskQuery().startOr().taskCandidateGroup("accountancy").endOr();
    assertEquals(2, query.count());
    assertEquals(2, query.list().size());

    // test including assigned tasks
    query = taskService.createTaskQuery().startOr().taskCandidateGroup("accountancy").includeAssignedTasks().endOr();
    assertEquals(3, query.count());
    assertEquals(3, query.list().size());

    // sales group is candidate for no tasks
    query = taskService.createTaskQuery().startOr().taskCandidateGroup("sales").endOr();
    assertEquals(0, query.count());
    assertEquals(0, query.list().size());

    // test including assigned tasks
    query = taskService.createTaskQuery().startOr().taskCandidateGroup("sales").includeAssignedTasks().endOr();
    assertEquals(0, query.count());
    assertEquals(0, query.list().size());
  }

  public void testQueryWithCandidateGroupsOr() {
    // test withCandidateGroups
    TaskQuery query = taskService.createTaskQuery().startOr().withCandidateGroups();
    assertEquals(4, query.endOr().count());

    query = taskService.createTaskQuery().startOr().withCandidateGroups();
    assertEquals(4, query.endOr().list().size());

    query = taskService.createTaskQuery().startOr().withCandidateGroups();
    assertEquals(5, query.includeAssignedTasks().endOr().count());

    query = taskService.createTaskQuery().startOr().withCandidateGroups();
    assertEquals(5, query.includeAssignedTasks().endOr().list().size());
  }

  public void testQueryWithCandidateGroupsOr2() throws Throwable {
    //super.runTest();
    // test withCandidateGroups
    TaskQuery query = taskService.createTaskQuery().startOr().withCandidateGroups();
    assertEquals(4, query.endOr().count());
    assertEquals(4, query.endOr().list().size());

    assertEquals(5, query.includeAssignedTasks().endOr().count());
    assertEquals(5, query.includeAssignedTasks().endOr().list().size());
  }

  public void testQueryWithoutCandidateGroupsOr2() throws Throwable {
    //super.runTest();
    // test withoutCandidateGroups
    TaskQuery query = taskService.createTaskQuery().startOr().withoutCandidateGroups();
    assertEquals(6, query.endOr().count());
    assertEquals(6, query.endOr().list().size());

    assertEquals(7, query.includeAssignedTasks().endOr().count());
    assertEquals(7, query.includeAssignedTasks().endOr().list().size());
  }

  public void testQueryWithoutCandidateGroupsOr() {
    // test withoutCandidateGroups
    TaskQuery query = taskService.createTaskQuery().startOr().withoutCandidateGroups();
    assertEquals(6, query.endOr().count());

    query = taskService.createTaskQuery().startOr().withoutCandidateGroups();
    assertEquals(6, query.endOr().list().size());

    query = taskService.createTaskQuery().startOr().withoutCandidateGroups();
    assertEquals(7, query.includeAssignedTasks().endOr().count());

    query = taskService.createTaskQuery().startOr().withoutCandidateGroups();
    assertEquals(7, query.includeAssignedTasks().endOr().list().size());
  }

  public void testQueryByNullCandidateGroupOr() {
    try {
      taskService.createTaskQuery().startOr().taskCandidateGroup(null).endOr().list();
      fail("expected exception");
    } catch (ProcessEngineException e) {
      // OK
    }
  }

  public void testQueryByCandidateGroupInOr() {
    List<String> groups = Arrays.asList("management", "accountancy");
    TaskQuery query = taskService.createTaskQuery().startOr().taskCandidateGroupIn(groups).endOr();
    System.out.println(query);
    //assertEquals(4, query.count());
    assertEquals(4, query.list().size());
    try {
      query.singleResult();
      fail("expected exception");
    } catch (ProcessEngineException e) {
      // OK
    }

    // test including assigned tasks
    query = taskService.createTaskQuery().startOr().taskCandidateGroupIn(groups).includeAssignedTasks().endOr();
    assertEquals(5, query.count());
    assertEquals(5, query.list().size());

    // Unexisting groups or groups that don't have candidate tasks shouldn't influence other results
    groups = Arrays.asList("management", "accountancy", "sales", "unexising");
    query = taskService.createTaskQuery().startOr().taskCandidateGroupIn(groups).endOr();
    assertEquals(4, query.count());
    assertEquals(4, query.list().size());

    // test including assigned tasks
    query = taskService.createTaskQuery().startOr().taskCandidateGroupIn(groups).includeAssignedTasks().endOr();
    assertEquals(5, query.count());
    assertEquals(5, query.list().size());
  }

  public void testQueryByNullCandidateGroupInOr() {
    try {
      taskService.createTaskQuery().startOr().taskCandidateGroupIn(null).endOr().list();
      fail("expected exception");
    } catch (ProcessEngineException e) {
      // OK
    }
    try {
      taskService.createTaskQuery().startOr().taskCandidateGroupIn(new ArrayList<String>()).endOr().list();
      fail("expected exception");
    } catch (ProcessEngineException e) {
      // OK
    }
  }
  @Deployment(resources = "org/camunda/bpm/engine/test/api/task/TaskQueryTest.testTaskVariableValueEquals.bpmn20.xml")
  public void testTaskVariableValueEqualsOr() throws Exception {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().startOr().processInstanceId(processInstance.getId()).endOr().singleResult();

    // No task should be found for an unexisting var
    assertEquals(0, taskService.createTaskQuery().startOr().taskVariableValueEquals("unexistingVar", "value").endOr().count());

    // Create a map with a variable for all default types
    Map<String, Object> variables = new HashMap<String, Object>();
    variables.put("longVar", 928374L);
    variables.put("shortVar", (short) 123);
    variables.put("integerVar", 1234);
    variables.put("stringVar", "stringValue");
    variables.put("booleanVar", true);
    Date date = Calendar.getInstance().getTime();
    variables.put("dateVar", date);
    variables.put("nullVar", null);

    taskService.setVariablesLocal(task.getId(), variables);

    // Test query matches
    assertEquals(1, taskService.createTaskQuery().startOr().taskVariableValueEquals("longVar", 928374L).endOr().count());
    assertEquals(1, taskService.createTaskQuery().startOr().taskVariableValueEquals("shortVar",  (short) 123).endOr().count());
    assertEquals(1, taskService.createTaskQuery().startOr().taskVariableValueEquals("integerVar", 1234).endOr().count());
    assertEquals(1, taskService.createTaskQuery().startOr().taskVariableValueEquals("stringVar", "stringValue").endOr().count());
    assertEquals(1, taskService.createTaskQuery().startOr().taskVariableValueEquals("booleanVar", true).endOr().count());
    assertEquals(1, taskService.createTaskQuery().startOr().taskVariableValueEquals("dateVar", date).endOr().count());
    assertEquals(1, taskService.createTaskQuery().startOr().taskVariableValueEquals("nullVar", null).endOr().count());

    // Test query for other values on existing variables
    assertEquals(0, taskService.createTaskQuery().startOr().taskVariableValueEquals("longVar", 999L).endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().taskVariableValueEquals("shortVar",  (short) 999).endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().taskVariableValueEquals("integerVar", 999).endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().taskVariableValueEquals("stringVar", "999").endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().taskVariableValueEquals("booleanVar", false).endOr().count());
    Calendar otherDate = Calendar.getInstance();
    otherDate.add(Calendar.YEAR, 1);
    assertEquals(0, taskService.createTaskQuery().startOr().taskVariableValueEquals("dateVar", otherDate.getTime()).endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().taskVariableValueEquals("nullVar", "999").endOr().count());

    // Test query for not equals
    assertEquals(1, taskService.createTaskQuery().startOr().taskVariableValueNotEquals("longVar", 999L).endOr().count());
    assertEquals(1, taskService.createTaskQuery().startOr().taskVariableValueNotEquals("shortVar",  (short) 999).endOr().count());
    assertEquals(1, taskService.createTaskQuery().startOr().taskVariableValueNotEquals("integerVar", 999).endOr().count());
    assertEquals(1, taskService.createTaskQuery().startOr().taskVariableValueNotEquals("stringVar", "999").endOr().count());
    assertEquals(1, taskService.createTaskQuery().startOr().taskVariableValueNotEquals("booleanVar", false).endOr().count());

  }
  @Deployment(resources="org/camunda/bpm/engine/test/api/task/TaskQueryTest.testTaskVariableValueEquals.bpmn20.xml")
  public void testTaskVariableValueLikeOr() throws Exception {

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

    Map<String, Object> variables = new HashMap<String, Object>();
    variables.put("stringVar", "stringValue");

    taskService.setVariablesLocal(task.getId(), variables);

    assertEquals(1, taskService.createTaskQuery().startOr().taskVariableValueLike("stringVar", "stringVal%").endOr().count());
    assertEquals(1, taskService.createTaskQuery().startOr().taskVariableValueLike("stringVar", "%ngValue").endOr().count());
    assertEquals(1, taskService.createTaskQuery().startOr().taskVariableValueLike("stringVar", "%ngVal%").endOr().count());

    assertEquals(0, taskService.createTaskQuery().startOr().taskVariableValueLike("stringVar", "stringVar%").endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().taskVariableValueLike("stringVar", "%ngVar").endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().taskVariableValueLike("stringVar", "%ngVar%").endOr().count());

    assertEquals(0, taskService.createTaskQuery().startOr().taskVariableValueLike("stringVar", "stringVal").endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().taskVariableValueLike("nonExistingVar", "string%").endOr().count());

    // test with null value
    try {
      taskService.createTaskQuery().startOr().taskVariableValueLike("stringVar", null).endOr().count();
      fail("expected exception");
    } catch (final ProcessEngineException e) {/*OK*/}
  }

  @Deployment(resources="org/camunda/bpm/engine/test/api/task/TaskQueryTest.testTaskVariableValueEquals.bpmn20.xml")
  public void testTaskVariableValueCompareOr() throws Exception {

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

    Map<String, Object> variables = new HashMap<String, Object>();
    variables.put("numericVar", 928374);
    Date date = new GregorianCalendar(2014, 2, 2, 2, 2, 2).getTime();
    variables.put("dateVar", date);
    variables.put("stringVar", "ab");
    variables.put("nullVar", null);

    taskService.setVariablesLocal(task.getId(), variables);

    // test compare methods with numeric values
    assertEquals(1, taskService.createTaskQuery().startOr().taskVariableValueGreaterThan("numericVar", 928373).endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().taskVariableValueGreaterThan("numericVar", 928374).endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().taskVariableValueGreaterThan("numericVar", 928375).endOr().count());

    assertEquals(1, taskService.createTaskQuery().startOr().taskVariableValueGreaterThanOrEquals("numericVar", 928373).endOr().count());
    assertEquals(1, taskService.createTaskQuery().startOr().taskVariableValueGreaterThanOrEquals("numericVar", 928374).endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().taskVariableValueGreaterThanOrEquals("numericVar", 928375).endOr().count());

    assertEquals(1, taskService.createTaskQuery().startOr().taskVariableValueLessThan("numericVar", 928375).endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().taskVariableValueLessThan("numericVar", 928374).endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().taskVariableValueLessThan("numericVar", 928373).endOr().count());

    assertEquals(1, taskService.createTaskQuery().startOr().taskVariableValueLessThanOrEquals("numericVar", 928375).endOr().count());
    assertEquals(1, taskService.createTaskQuery().startOr().taskVariableValueLessThanOrEquals("numericVar", 928374).endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().taskVariableValueLessThanOrEquals("numericVar", 928373).endOr().count());

    // test compare methods with date values
    Date before = new GregorianCalendar(2014, 2, 2, 2, 2, 1).getTime();
    Date after = new GregorianCalendar(2014, 2, 2, 2, 2, 3).getTime();

    assertEquals(1, taskService.createTaskQuery().startOr().taskVariableValueGreaterThan("dateVar", before).endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().taskVariableValueGreaterThan("dateVar", date).endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().taskVariableValueGreaterThan("dateVar", after).endOr().count());

    assertEquals(1, taskService.createTaskQuery().startOr().taskVariableValueGreaterThanOrEquals("dateVar", before).endOr().count());
    assertEquals(1, taskService.createTaskQuery().startOr().taskVariableValueGreaterThanOrEquals("dateVar", date).endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().taskVariableValueGreaterThanOrEquals("dateVar", after).endOr().count());

    assertEquals(1, taskService.createTaskQuery().startOr().taskVariableValueLessThan("dateVar", after).endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().taskVariableValueLessThan("dateVar", date).endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().taskVariableValueLessThan("dateVar", before).endOr().count());

    assertEquals(1, taskService.createTaskQuery().startOr().taskVariableValueLessThanOrEquals("dateVar", after).endOr().count());
    assertEquals(1, taskService.createTaskQuery().startOr().taskVariableValueLessThanOrEquals("dateVar", date).endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().taskVariableValueLessThanOrEquals("dateVar", before).endOr().count());

    //test with string values
    assertEquals(1, taskService.createTaskQuery().startOr().taskVariableValueGreaterThan("stringVar", "aa").endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().taskVariableValueGreaterThan("stringVar", "ab").endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().taskVariableValueGreaterThan("stringVar", "ba").endOr().count());

    assertEquals(1, taskService.createTaskQuery().startOr().taskVariableValueGreaterThanOrEquals("stringVar", "aa").endOr().count());
    assertEquals(1, taskService.createTaskQuery().startOr().taskVariableValueGreaterThanOrEquals("stringVar", "ab").endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().taskVariableValueGreaterThanOrEquals("stringVar", "ba").endOr().count());

    assertEquals(1, taskService.createTaskQuery().startOr().taskVariableValueLessThan("stringVar", "ba").endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().taskVariableValueLessThan("stringVar", "ab").endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().taskVariableValueLessThan("stringVar", "aa").endOr().count());

    assertEquals(1, taskService.createTaskQuery().startOr().taskVariableValueLessThanOrEquals("stringVar", "ba").endOr().count());
    assertEquals(1, taskService.createTaskQuery().startOr().taskVariableValueLessThanOrEquals("stringVar", "ab").endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().taskVariableValueLessThanOrEquals("stringVar", "aa").endOr().count());

    // test with null value
    try {
      taskService.createTaskQuery().startOr().taskVariableValueGreaterThan("nullVar", null).endOr().count();
      fail("expected exception");
    } catch (final ProcessEngineException e) {/*OK*/}
    try {
      taskService.createTaskQuery().startOr().taskVariableValueGreaterThanOrEquals("nullVar", null).endOr().count();
      fail("expected exception");
    } catch (final ProcessEngineException e) {/*OK*/}
    try {
      taskService.createTaskQuery().startOr().taskVariableValueLessThan("nullVar", null).endOr().count();
      fail("expected exception");
    } catch (final ProcessEngineException e) {/*OK*/}
    try {
      taskService.createTaskQuery().startOr().taskVariableValueLessThanOrEquals("nullVar", null).endOr().count();
      fail("expected exception");
    } catch (final ProcessEngineException e) {/*OK*/}

    // test with boolean value
    try {
      taskService.createTaskQuery().startOr().taskVariableValueGreaterThan("nullVar", true).endOr().count();
      fail("expected exception");
    } catch (final ProcessEngineException e) {/*OK*/}
    try {
      taskService.createTaskQuery().startOr().taskVariableValueGreaterThanOrEquals("nullVar", false).endOr().count();
      fail("expected exception");
    } catch (final ProcessEngineException e) {/*OK*/}
    try {
      taskService.createTaskQuery().startOr().taskVariableValueLessThan("nullVar", true).endOr().count();
      fail("expected exception");
    } catch (final ProcessEngineException e) {/*OK*/}
    try {
      taskService.createTaskQuery().startOr().taskVariableValueLessThanOrEquals("nullVar", false).endOr().count();
      fail("expected exception");
    } catch (final ProcessEngineException e) {/*OK*/}

    // test non existing variable
    assertEquals(0, taskService.createTaskQuery().startOr().taskVariableValueLessThanOrEquals("nonExisting", 123).endOr().count());
  }

  @Deployment(resources="org/camunda/bpm/engine/test/api/task/TaskQueryTest.testProcessVariableValueEquals.bpmn20.xml")
  public void testProcessVariableValueEqualsOr() throws Exception {
    Map<String, Object> variables = new HashMap<String, Object>();
    variables.put("longVar", 928374L);
    variables.put("shortVar", (short) 123);
    variables.put("integerVar", 1234);
    variables.put("stringVar", "stringValue");
    variables.put("booleanVar", true);
    Date date = Calendar.getInstance().getTime();
    variables.put("dateVar", date);
    variables.put("nullVar", null);

    // Start process-instance with all types of variables
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

    // Test query matches
    assertEquals(1, taskService.createTaskQuery().startOr().processVariableValueEquals("longVar", 928374L).endOr().count());
    assertEquals(1, taskService.createTaskQuery().startOr().processVariableValueEquals("shortVar",  (short) 123).endOr().count());
    assertEquals(1, taskService.createTaskQuery().startOr().processVariableValueEquals("integerVar", 1234).endOr().count());
    assertEquals(1, taskService.createTaskQuery().startOr().processVariableValueEquals("stringVar", "stringValue").endOr().count());
    assertEquals(1, taskService.createTaskQuery().startOr().processVariableValueEquals("booleanVar", true).endOr().count());
    assertEquals(1, taskService.createTaskQuery().startOr().processVariableValueEquals("dateVar", date).endOr().count());
    assertEquals(1, taskService.createTaskQuery().startOr().processVariableValueEquals("nullVar", null).endOr().count());

    // Test query for other values on existing variables
    assertEquals(0, taskService.createTaskQuery().startOr().processVariableValueEquals("longVar", 999L).endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().processVariableValueEquals("shortVar",  (short) 999).endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().processVariableValueEquals("integerVar", 999).endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().processVariableValueEquals("stringVar", "999").endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().processVariableValueEquals("booleanVar", false).endOr().count());
    Calendar otherDate = Calendar.getInstance();
    otherDate.add(Calendar.YEAR, 1);
    assertEquals(0, taskService.createTaskQuery().startOr().processVariableValueEquals("dateVar", otherDate.getTime()).endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().processVariableValueEquals("nullVar", "999").endOr().count());

    // Test querying for task variables don't match the process-variables
    assertEquals(0, taskService.createTaskQuery().startOr().taskVariableValueEquals("longVar", 928374L).endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().taskVariableValueEquals("shortVar",  (short) 123).endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().taskVariableValueEquals("integerVar", 1234).endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().taskVariableValueEquals("stringVar", "stringValue").endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().taskVariableValueEquals("booleanVar", true).endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().taskVariableValueEquals("dateVar", date).endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().taskVariableValueEquals("nullVar", null).endOr().count());

    // Test querying for task variables not equals
    assertEquals(1, taskService.createTaskQuery().startOr().processVariableValueNotEquals("longVar", 999L).endOr().count());
    assertEquals(1, taskService.createTaskQuery().startOr().processVariableValueNotEquals("shortVar",  (short) 999).endOr().count());
    assertEquals(1, taskService.createTaskQuery().startOr().processVariableValueNotEquals("integerVar", 999).endOr().count());
    assertEquals(1, taskService.createTaskQuery().startOr().processVariableValueNotEquals("stringVar", "999").endOr().count());
    assertEquals(1, taskService.createTaskQuery().startOr().processVariableValueNotEquals("booleanVar", false).endOr().count());

    // and query for the existing variable with NOT shoudl result in nothing found:
    assertEquals(0, taskService.createTaskQuery().startOr().processVariableValueNotEquals("longVar", 928374L).endOr().count());

    // Test combination of task-variable and process-variable
    Task task = taskService.createTaskQuery().startOr().processInstanceId(processInstance.getId()).endOr().singleResult();
    taskService.setVariableLocal(task.getId(), "taskVar", "theValue");
    taskService.setVariableLocal(task.getId(), "longVar", 928374L);

    assertEquals(1, taskService.createTaskQuery().startOr()
      .processVariableValueEquals("longVar", 928374L)
      .taskVariableValueEquals("taskVar", "theValue")
      .endOr().count());

    assertEquals(1, taskService.createTaskQuery().startOr()
      .processVariableValueEquals("longVar", 928374L)
      .taskVariableValueEquals("longVar", 928374L)
      .endOr().count());
  }

  @Deployment(resources="org/camunda/bpm/engine/test/api/task/TaskQueryTest.testProcessVariableValueEquals.bpmn20.xml")
  public void testProcessVariableValueLikeOr() throws Exception {

    Map<String, Object> variables = new HashMap<String, Object>();
    variables.put("stringVar", "stringValue");
    runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

    assertEquals(1, taskService.createTaskQuery().startOr().processVariableValueLike("stringVar", "stringVal%").endOr().count());
    assertEquals(1, taskService.createTaskQuery().startOr().processVariableValueLike("stringVar", "%ngValue").endOr().count());
    assertEquals(1, taskService.createTaskQuery().startOr().processVariableValueLike("stringVar", "%ngVal%").endOr().count());

    assertEquals(0, taskService.createTaskQuery().startOr().processVariableValueLike("stringVar", "stringVar%").endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().processVariableValueLike("stringVar", "%ngVar").endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().processVariableValueLike("stringVar", "%ngVar%").endOr().count());

    assertEquals(0, taskService.createTaskQuery().startOr().processVariableValueLike("stringVar", "stringVal").endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().processVariableValueLike("nonExistingVar", "string%").endOr().count());

    // test with null value
    try {
      taskService.createTaskQuery().startOr().processVariableValueLike("stringVar", null).endOr().count();
      fail("expected exception");
    } catch (final ProcessEngineException e) {/*OK*/}
  }

  @Deployment(resources="org/camunda/bpm/engine/test/api/task/TaskQueryTest.testProcessVariableValueEquals.bpmn20.xml")
  public void testProcessVariableValueCompareOr() throws Exception {

    Map<String, Object> variables = new HashMap<String, Object>();
    variables.put("numericVar", 928374);
    Date date = new GregorianCalendar(2014, 2, 2, 2, 2, 2).getTime();
    variables.put("dateVar", date);
    variables.put("stringVar", "ab");
    variables.put("nullVar", null);

    runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

    // test compare methods with numeric values
    assertEquals(1, taskService.createTaskQuery().startOr().processVariableValueGreaterThan("numericVar", 928373).endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().processVariableValueGreaterThan("numericVar", 928374).endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().processVariableValueGreaterThan("numericVar", 928375).endOr().count());

    assertEquals(1, taskService.createTaskQuery().startOr().processVariableValueGreaterThanOrEquals("numericVar", 928373).endOr().count());
    assertEquals(1, taskService.createTaskQuery().startOr().processVariableValueGreaterThanOrEquals("numericVar", 928374).endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().processVariableValueGreaterThanOrEquals("numericVar", 928375).endOr().count());

    assertEquals(1, taskService.createTaskQuery().startOr().processVariableValueLessThan("numericVar", 928375).endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().processVariableValueLessThan("numericVar", 928374).endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().processVariableValueLessThan("numericVar", 928373).endOr().count());

    assertEquals(1, taskService.createTaskQuery().startOr().processVariableValueLessThanOrEquals("numericVar", 928375).endOr().count());
    assertEquals(1, taskService.createTaskQuery().startOr().processVariableValueLessThanOrEquals("numericVar", 928374).endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().processVariableValueLessThanOrEquals("numericVar", 928373).endOr().count());

    // test compare methods with date values
    Date before = new GregorianCalendar(2014, 2, 2, 2, 2, 1).getTime();
    Date after = new GregorianCalendar(2014, 2, 2, 2, 2, 3).getTime();

    assertEquals(1, taskService.createTaskQuery().startOr().processVariableValueGreaterThan("dateVar", before).endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().processVariableValueGreaterThan("dateVar", date).endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().processVariableValueGreaterThan("dateVar", after).endOr().count());

    assertEquals(1, taskService.createTaskQuery().startOr().processVariableValueGreaterThanOrEquals("dateVar", before).endOr().count());
    assertEquals(1, taskService.createTaskQuery().startOr().processVariableValueGreaterThanOrEquals("dateVar", date).endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().processVariableValueGreaterThanOrEquals("dateVar", after).endOr().count());

    assertEquals(1, taskService.createTaskQuery().startOr().processVariableValueLessThan("dateVar", after).endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().processVariableValueLessThan("dateVar", date).endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().processVariableValueLessThan("dateVar", before).endOr().count());

    assertEquals(1, taskService.createTaskQuery().startOr().processVariableValueLessThanOrEquals("dateVar", after).endOr().count());
    assertEquals(1, taskService.createTaskQuery().startOr().processVariableValueLessThanOrEquals("dateVar", date).endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().processVariableValueLessThanOrEquals("dateVar", before).endOr().count());

    //test with string values
    assertEquals(1, taskService.createTaskQuery().startOr().processVariableValueGreaterThan("stringVar", "aa").endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().processVariableValueGreaterThan("stringVar", "ab").endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().processVariableValueGreaterThan("stringVar", "ba").endOr().count());

    assertEquals(1, taskService.createTaskQuery().startOr().processVariableValueGreaterThanOrEquals("stringVar", "aa").endOr().count());
    assertEquals(1, taskService.createTaskQuery().startOr().processVariableValueGreaterThanOrEquals("stringVar", "ab").endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().processVariableValueGreaterThanOrEquals("stringVar", "ba").endOr().count());

    assertEquals(1, taskService.createTaskQuery().startOr().processVariableValueLessThan("stringVar", "ba").endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().processVariableValueLessThan("stringVar", "ab").endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().processVariableValueLessThan("stringVar", "aa").endOr().count());

    assertEquals(1, taskService.createTaskQuery().startOr().processVariableValueLessThanOrEquals("stringVar", "ba").endOr().count());
    assertEquals(1, taskService.createTaskQuery().startOr().processVariableValueLessThanOrEquals("stringVar", "ab").endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().processVariableValueLessThanOrEquals("stringVar", "aa").endOr().count());

    // test with null value
    try {
      taskService.createTaskQuery().startOr().processVariableValueGreaterThan("nullVar", null).endOr().count();
      fail("expected exception");
    } catch (final ProcessEngineException e) {/*OK*/}
    try {
      taskService.createTaskQuery().startOr().processVariableValueGreaterThanOrEquals("nullVar", null).endOr().count();
      fail("expected exception");
    } catch (final ProcessEngineException e) {/*OK*/}
    try {
      taskService.createTaskQuery().startOr().processVariableValueLessThan("nullVar", null).endOr().count();
      fail("expected exception");
    } catch (final ProcessEngineException e) {/*OK*/}
    try {
      taskService.createTaskQuery().startOr().processVariableValueLessThanOrEquals("nullVar", null).endOr().count();
      fail("expected exception");
    } catch (final ProcessEngineException e) {/*OK*/}

    // test with boolean value
    try {
      taskService.createTaskQuery().startOr().processVariableValueGreaterThan("nullVar", true).endOr().count();
      fail("expected exception");
    } catch (final ProcessEngineException e) {/*OK*/}
    try {
      taskService.createTaskQuery().startOr().processVariableValueGreaterThanOrEquals("nullVar", false).endOr().count();
      fail("expected exception");
    } catch (final ProcessEngineException e) {/*OK*/}
    try {
      taskService.createTaskQuery().startOr().processVariableValueLessThan("nullVar", true).endOr().count();
      fail("expected exception");
    } catch (final ProcessEngineException e) {/*OK*/}
    try {
      taskService.createTaskQuery().startOr().processVariableValueLessThanOrEquals("nullVar", false).endOr().count();
      fail("expected exception");
    } catch (final ProcessEngineException e) {/*OK*/}

    // test non existing variable
    assertEquals(0, taskService.createTaskQuery().startOr().processVariableValueLessThanOrEquals("nonExisting", 123).endOr().count());
  }

  @Deployment(resources = "org/camunda/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  public void testProcessVariableValueEqualsNumberOr() throws Exception {
    // long
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
      Collections.<String, Object>singletonMap("var", 123L));

    // non-matching long
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
      Collections.<String, Object>singletonMap("var", 12345L));

    // short
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
      Collections.<String, Object>singletonMap("var", (short) 123));

    // double
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
      Collections.<String, Object>singletonMap("var", 123.0d));

    // integer
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
      Collections.<String, Object>singletonMap("var", 123));

    // untyped null (should not match)
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
      Collections.<String, Object>singletonMap("var", null));

    // typed null (should not match)
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
      Collections.<String, Object>singletonMap("var", Variables.longValue(null)));

    runtimeService.startProcessInstanceByKey("oneTaskProcess",
      Collections.<String, Object>singletonMap("var", "123"));

    assertEquals(4, taskService.createTaskQuery().startOr().processVariableValueEquals("var", Variables.numberValue(123)).endOr().count());
    assertEquals(4, taskService.createTaskQuery().startOr().processVariableValueEquals("var", Variables.numberValue(123L)).endOr().count());
    assertEquals(4, taskService.createTaskQuery().startOr().processVariableValueEquals("var", Variables.numberValue(123.0d)).endOr().count());
    assertEquals(4, taskService.createTaskQuery().startOr().processVariableValueEquals("var", Variables.numberValue((short) 123)).endOr().count());

    assertEquals(1, taskService.createTaskQuery().startOr().processVariableValueEquals("var", Variables.numberValue(null)).endOr().count());
  }

  @Deployment(resources = "org/camunda/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  public void testProcessVariableValueNumberComparisonOr() throws Exception {
    // long
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
      Collections.<String, Object>singletonMap("var", 123L));

    // non-matching long
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
      Collections.<String, Object>singletonMap("var", 12345L));

    // short
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
      Collections.<String, Object>singletonMap("var", (short) 123));

    // double
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
      Collections.<String, Object>singletonMap("var", 123.0d));

    // integer
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
      Collections.<String, Object>singletonMap("var", 123));

    // untyped null
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
      Collections.<String, Object>singletonMap("var", null));

    // typed null
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
      Collections.<String, Object>singletonMap("var", Variables.longValue(null)));

    runtimeService.startProcessInstanceByKey("oneTaskProcess",
      Collections.<String, Object>singletonMap("var", "123"));

    assertEquals(4, taskService.createTaskQuery().startOr().processVariableValueNotEquals("var", Variables.numberValue(123)).endOr().count());
    assertEquals(1, taskService.createTaskQuery().startOr().processVariableValueGreaterThan("var", Variables.numberValue(123)).endOr().count());
    assertEquals(5, taskService.createTaskQuery().startOr().processVariableValueGreaterThanOrEquals("var", Variables.numberValue(123)).endOr().count());
    assertEquals(0, taskService.createTaskQuery().startOr().processVariableValueLessThan("var", Variables.numberValue(123)).endOr().count());
    assertEquals(4, taskService.createTaskQuery().startOr().processVariableValueLessThanOrEquals("var", Variables.numberValue(123)).endOr().count());
  }

  @Deployment(resources = "org/camunda/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  public void testTaskVariableValueEqualsNumberOr() throws Exception {
    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    runtimeService.startProcessInstanceByKey("oneTaskProcess");

    List<Task> tasks = taskService.createTaskQuery().startOr().processDefinitionKey("oneTaskProcess").endOr().list();
    assertEquals(8, tasks.size());
    taskService.setVariableLocal(tasks.get(0).getId(), "var", 123L);
    taskService.setVariableLocal(tasks.get(1).getId(), "var", 12345L);
    taskService.setVariableLocal(tasks.get(2).getId(), "var", (short) 123);
    taskService.setVariableLocal(tasks.get(3).getId(), "var", 123.0d);
    taskService.setVariableLocal(tasks.get(4).getId(), "var", 123);
    taskService.setVariableLocal(tasks.get(5).getId(), "var", null);
    taskService.setVariableLocal(tasks.get(6).getId(), "var", Variables.longValue(null));
    taskService.setVariableLocal(tasks.get(7).getId(), "var", "123");

    assertEquals(4, taskService.createTaskQuery().startOr().taskVariableValueEquals("var", Variables.numberValue(123)).endOr().count());
    assertEquals(4, taskService.createTaskQuery().startOr().taskVariableValueEquals("var", Variables.numberValue(123L)).endOr().count());
    assertEquals(4, taskService.createTaskQuery().startOr().taskVariableValueEquals("var", Variables.numberValue(123.0d)).endOr().count());
    assertEquals(4, taskService.createTaskQuery().startOr().taskVariableValueEquals("var", Variables.numberValue((short) 123)).endOr().count());

    assertEquals(1, taskService.createTaskQuery().startOr().taskVariableValueEquals("var", Variables.numberValue(null)).endOr().count());
  }

  @Deployment(resources = "org/camunda/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  public void testVariableEqualsNumberMaxOr() throws Exception {
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
      Collections.<String, Object>singletonMap("var", MAX_DOUBLE_VALUE));
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
      Collections.<String, Object>singletonMap("var", Long.MAX_VALUE));

    assertEquals(1, taskService.createTaskQuery().startOr().processVariableValueEquals("var", Variables.numberValue(MAX_DOUBLE_VALUE)).endOr().count());
    assertEquals(1, taskService.createTaskQuery().startOr().processVariableValueEquals("var", Variables.numberValue(Long.MAX_VALUE)).endOr().count());
  }

  @Deployment(resources = "org/camunda/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  public void testVariableEqualsNumberLongValueOverflowOr() throws Exception {
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
      Collections.<String, Object>singletonMap("var", MAX_DOUBLE_VALUE));

    // this results in an overflow
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
      Collections.<String, Object>singletonMap("var", (long) MAX_DOUBLE_VALUE));

    // the query should not find the long variable
    assertEquals(1, taskService.createTaskQuery().startOr().processVariableValueEquals("var", Variables.numberValue(MAX_DOUBLE_VALUE)).endOr().count());
  }

  @Deployment(resources = "org/camunda/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  public void testVariableEqualsNumberNonIntegerDoubleShouldNotMatchIntegerOr() throws Exception {
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
      Variables.createVariables().putValue("var", 42).putValue("var2", 52.4d));

    // querying by 42.4 should not match the integer variable 42
    assertEquals(0, taskService.createTaskQuery().startOr().processVariableValueEquals("var", Variables.numberValue(42.4d)).endOr().count());

    runtimeService.startProcessInstanceByKey("oneTaskProcess",
      Collections.<String, Object>singletonMap("var", 42.4d));

    // querying by 52 should not find the double variable 52.4
    assertEquals(0, taskService.createTaskQuery().startOr().processVariableValueEquals("var", Variables.numberValue(52)).endOr().count());
  }
  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByNullCaseInstanceVariableValueEqualsOr() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aNullValue", null)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueEquals("aNullValue", null);

    verifyQueryResults(query, 1);
  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByStringCaseInstanceVariableValueEqualsOr() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aStringValue", "abc")
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueEquals("aStringValue", "abc").endOr();

    verifyQueryResults(query, 1);
  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByBooleanCaseInstanceVariableValueEqualsOr() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aBooleanValue", true)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueEquals("aBooleanValue", true).endOr();

    verifyQueryResults(query, 1);
  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByShortCaseInstanceVariableValueEqualsOr() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aShortValue", (short) 123)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueEquals("aShortValue", (short) 123).endOr();

    verifyQueryResults(query, 1);
  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByIntegerCaseInstanceVariableValueEqualsOr() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("anIntegerValue", 456)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueEquals("anIntegerValue", 456).endOr();

    verifyQueryResults(query, 1);
  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByLongCaseInstanceVariableValueEqualsOr() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aLongValue", (long) 789)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueEquals("aLongValue", (long) 789).endOr();

    verifyQueryResults(query, 1);
  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByDateCaseInstanceVariableValueEqualsOr() {
    String caseDefinitionId = getCaseDefinitionId();

    Date now = new Date();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aDateValue", now)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueEquals("aDateValue", now).endOr();

    verifyQueryResults(query, 1);
  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByDoubleCaseInstanceVariableValueEqualsOr() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aDoubleValue", 1.5)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueEquals("aDoubleValue", 1.5).endOr();

    verifyQueryResults(query, 1);
  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByByteArrayCaseInstanceVariableValueEqualsOr() {
    String caseDefinitionId = getCaseDefinitionId();

    byte[] bytes = "somebytes".getBytes();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aByteArrayValue", bytes)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    try {
      query.startOr().caseInstanceVariableValueEquals("aByteArrayValue", bytes).endOr().list();
      fail();
    } catch (ProcessEngineException e) {}
  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryBySerializableCaseInstanceVariableValueEqualsOr() {
    String caseDefinitionId = getCaseDefinitionId();

    List<String> serializable = new ArrayList<String>();
    serializable.add("one");
    serializable.add("two");
    serializable.add("three");

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aSerializableValue", serializable)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    try {
      query.startOr().caseInstanceVariableValueEquals("aSerializableValue", serializable).endOr().list();
      fail();
    } catch (ProcessEngineException e) {}
  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByFileCaseInstanceVariableValueEqualsOr() {
    FileValue fileValue = createDefaultFileValue();
    String variableName = "aFileValue";

    startDefaultCaseWithVariable(fileValue, variableName);
    TaskQuery query = taskService.createTaskQuery();

    try {
      query.startOr().caseInstanceVariableValueEquals(variableName, fileValue).endOr().list();
      fail();
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage(), containsString("Variables of type File cannot be used to query"));
    }
  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByStringCaseInstanceVariableValueNotEqualsOr() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aStringValue", "abc")
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueNotEquals("aStringValue", "abd").endOr();

    verifyQueryResults(query, 1);
  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByBooleanCaseInstanceVariableValueNotEqualsOr() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aBooleanValue", true)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueNotEquals("aBooleanValue", false).endOr();

    verifyQueryResults(query, 1);
  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByShortCaseInstanceVariableValueNotEqualsOr() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aShortValue", (short) 123)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueNotEquals("aShortValue", (short) 124).endOr();

    verifyQueryResults(query, 1);
  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByIntegerCaseInstanceVariableValueNotEqualsOr() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("anIntegerValue", 456)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueNotEquals("anIntegerValue", 457).endOr();

    verifyQueryResults(query, 1);
  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByLongCaseInstanceVariableValueNotEqualsOr() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aLongValue", (long) 789)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueNotEquals("aLongValue", (long) 790).endOr();

    verifyQueryResults(query, 1);
  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByDateCaseInstanceVariableValueNotEqualsOr() {
    String caseDefinitionId = getCaseDefinitionId();

    Date now = new Date();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aDateValue", now)
      .create();

    Date before = new Date(now.getTime() - 100000);

    TaskQuery query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueNotEquals("aDateValue", before).endOr();

    verifyQueryResults(query, 1);
  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByDoubleCaseInstanceVariableValueNotEqualsOr() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aDoubleValue", 1.5)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueNotEquals("aDoubleValue", 1.6).endOr();

    verifyQueryResults(query, 1);
  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByFileCaseInstanceVariableValueNotEqualsOr() {
    FileValue fileValue = createDefaultFileValue();
    String variableName = "aFileValue";

    startDefaultCaseWithVariable(fileValue, variableName);
    TaskQuery query = taskService.createTaskQuery();
    try {
      query.startOr().caseInstanceVariableValueNotEquals(variableName, fileValue).endOr().list();
      fail();
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage(), containsString("Variables of type File cannot be used to query"));
    }
  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryBySerializableCaseInstanceVariableValueNotEqualsOr() {
    String caseDefinitionId = getCaseDefinitionId();

    List<String> serializable = new ArrayList<String>();
    serializable.add("one");
    serializable.add("two");
    serializable.add("three");

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aSerializableValue", serializable)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    try {
      query.startOr().caseInstanceVariableValueNotEquals("aSerializableValue", serializable).endOr().list();
      fail();
    } catch (ProcessEngineException e) {}
  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByByteArrayCaseInstanceVariableValueNotEqualsOr() {
    String caseDefinitionId = getCaseDefinitionId();

    byte[] bytes = "somebytes".getBytes();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aByteArrayValue", bytes)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    try {
      query.startOr().caseInstanceVariableValueNotEquals("aByteArrayValue", bytes).endOr().list();
      fail();
    } catch (ProcessEngineException e) {}
  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByNullCaseInstanceVariableValueGreaterThanOr() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aNullValue", null)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    try {
      query.startOr().caseInstanceVariableValueGreaterThan("aNullValue", null).endOr().list();
      fail();
    } catch (ProcessEngineException e) {}

  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByStringCaseInstanceVariableValueGreaterThanOr() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aStringValue", "abc")
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueGreaterThan("aStringValue", "ab").endOr();

    verifyQueryResults(query, 1);

  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByBooleanCaseInstanceVariableValueGreaterThanOr() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aBooleanValue", true)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    try {
      query.startOr().caseInstanceVariableValueGreaterThan("aBooleanValue", false).endOr().list();
      fail();
    } catch (ProcessEngineException e) {}

  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByShortCaseInstanceVariableValueGreaterThanOr() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aShortValue", (short) 123)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueGreaterThan("aShortValue", (short) 122).endOr();

    verifyQueryResults(query, 1);

  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByIntegerCaseInstanceVariableValueGreaterThanOr() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("anIntegerValue", 456)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueGreaterThan("anIntegerValue", 455).endOr();

    verifyQueryResults(query, 1);

  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByLongCaseInstanceVariableValueGreaterThanOr() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aLongValue", (long) 789)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueGreaterThan("aLongValue", (long) 788).endOr();

    verifyQueryResults(query, 1);

  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByDateCaseInstanceVariableValueGreaterThanOr() {
    String caseDefinitionId = getCaseDefinitionId();

    Date now = new Date();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aDateValue", now)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    Date before = new Date(now.getTime() - 100000);

    query.startOr().caseInstanceVariableValueGreaterThan("aDateValue", before).endOr();

    verifyQueryResults(query, 1);

  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByDoubleCaseInstanceVariableValueGreaterThanOr() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aDoubleValue", 1.5)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueGreaterThan("aDoubleValue", 1.4).endOr();

    verifyQueryResults(query, 1);

  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByByteArrayCaseInstanceVariableValueGreaterThanOr() {
    String caseDefinitionId = getCaseDefinitionId();

    byte[] bytes = "somebytes".getBytes();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aByteArrayValue", bytes)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    try {
      query.startOr().caseInstanceVariableValueGreaterThan("aByteArrayValue", bytes).endOr().list();
      fail();
    } catch (ProcessEngineException e) {}
  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryBySerializableCaseInstanceVariableGreaterThanOr() {
    String caseDefinitionId = getCaseDefinitionId();

    List<String> serializable = new ArrayList<String>();
    serializable.add("one");
    serializable.add("two");
    serializable.add("three");

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aSerializableValue", serializable)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    try {
      query.startOr().caseInstanceVariableValueGreaterThan("aSerializableValue", serializable).endOr().list();
      fail();
    } catch (ProcessEngineException e) {}
  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn"})
  public void testQueryByFileCaseInstanceVariableValueGreaterThanOr() {
    FileValue fileValue = createDefaultFileValue();
    String variableName = "aFileValue";

    startDefaultCaseWithVariable(fileValue, variableName);
    startDefaultCaseExecutionManually();
    TaskQuery query = taskService.createTaskQuery();

    try {
      query.startOr().caseInstanceVariableValueGreaterThan(variableName, fileValue).endOr().list();
      fail();
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage(), containsString("Variables of type File cannot be used to query"));
    }
  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByNullCaseInstanceVariableValueGreaterThanOrEqualOr() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aNullValue", null)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    try {
      query.startOr().caseInstanceVariableValueGreaterThanOrEquals("aNullValue", null).endOr().list();
      fail();
    } catch (ProcessEngineException e) {}

  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByStringCaseInstanceVariableValueGreaterThanOrEqualOr() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aStringValue", "abc")
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueGreaterThanOrEquals("aStringValue", "ab").endOr();

    verifyQueryResults(query, 1);

    query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueGreaterThanOrEquals("aStringValue", "abc").endOr();

    verifyQueryResults(query, 1);

  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByBooleanCaseInstanceVariableValueGreaterThanOrEqualOr() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aBooleanValue", true)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    try {
      query.startOr().caseInstanceVariableValueGreaterThanOrEquals("aBooleanValue", false).endOr().list();
      fail();
    } catch (ProcessEngineException e) {}

  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByShortCaseInstanceVariableValueGreaterThanOrEqualOr() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aShortValue", (short) 123)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueGreaterThanOrEquals("aShortValue", (short) 122).endOr();

    verifyQueryResults(query, 1);

    query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueGreaterThanOrEquals("aShortValue", (short) 123).endOr();

    verifyQueryResults(query, 1);

  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByIntegerCaseInstanceVariableValueGreaterThanOrEqualsOr() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("anIntegerValue", 456)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueGreaterThanOrEquals("anIntegerValue", 455).endOr();

    verifyQueryResults(query, 1);

    query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueGreaterThanOrEquals("anIntegerValue", 456).endOr();

    verifyQueryResults(query, 1);

  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByLongCaseInstanceVariableValueGreaterThanOrEqualOr() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aLongValue", (long) 789)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueGreaterThanOrEquals("aLongValue", (long) 788).endOr();

    verifyQueryResults(query, 1);

    query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueGreaterThanOrEquals("aLongValue", (long) 789).endOr();

    verifyQueryResults(query, 1);

  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByDateCaseInstanceVariableValueGreaterThanOrEqualOr() {
    String caseDefinitionId = getCaseDefinitionId();

    Date now = new Date();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aDateValue", now)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    Date before = new Date(now.getTime() - 100000);

    query.startOr().caseInstanceVariableValueGreaterThanOrEquals("aDateValue", before).endOr();

    verifyQueryResults(query, 1);

    query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueGreaterThanOrEquals("aDateValue", now).endOr();

    verifyQueryResults(query, 1);

  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByDoubleCaseInstanceVariableValueGreaterThanOrEqualOr() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aDoubleValue", 1.5)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueGreaterThanOrEquals("aDoubleValue", 1.4).endOr();

    verifyQueryResults(query, 1);

    query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueGreaterThanOrEquals("aDoubleValue", 1.5).endOr();

    verifyQueryResults(query, 1);

  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByByteArrayCaseInstanceVariableValueGreaterThanOrEqualOr() {
    String caseDefinitionId = getCaseDefinitionId();

    byte[] bytes = "somebytes".getBytes();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aByteArrayValue", bytes)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    try {
      query.startOr().caseInstanceVariableValueGreaterThanOrEquals("aByteArrayValue", bytes).endOr().list();
      fail();
    } catch (ProcessEngineException e) {}
  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryBySerializableCaseInstanceVariableGreaterThanOrEqualOr() {
    String caseDefinitionId = getCaseDefinitionId();

    List<String> serializable = new ArrayList<String>();
    serializable.add("one");
    serializable.add("two");
    serializable.add("three");

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aSerializableValue", serializable)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    try {
      query.startOr().caseInstanceVariableValueGreaterThanOrEquals("aSerializableValue", serializable).endOr().list();
      fail();
    } catch (ProcessEngineException e) {}
  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByFileCaseInstanceVariableValueGreaterThanOrEqualOr() {
    FileValue fileValue = createDefaultFileValue();
    String variableName = "aFileValue";

    startDefaultCaseWithVariable(fileValue, variableName);
    TaskQuery query = taskService.createTaskQuery();

    try {
      query.startOr().caseInstanceVariableValueGreaterThanOrEquals(variableName, fileValue).endOr().list();
      fail();
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage(), containsString("Variables of type File cannot be used to query"));
    }
  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByNullCaseInstanceVariableValueLessThanOr() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aNullValue", null)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    try {
      query.startOr().caseInstanceVariableValueLessThan("aNullValue", null).endOr().list();
      fail();
    } catch (ProcessEngineException e) {}

  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByStringCaseInstanceVariableValueLessThanOr() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aStringValue", "abc")
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueLessThan("aStringValue", "abd").endOr();

    verifyQueryResults(query, 1);

  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByBooleanCaseInstanceVariableValueLessThanOr() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aBooleanValue", true)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    try {
      query.startOr().caseInstanceVariableValueLessThan("aBooleanValue", false).endOr().list();
      fail();
    } catch (ProcessEngineException e) {}

  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByShortCaseInstanceVariableValueLessThanOr() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aShortValue", (short) 123)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueLessThan("aShortValue", (short) 124).endOr();

    verifyQueryResults(query, 1);

  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByIntegerCaseInstanceVariableValueLessThanOr() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("anIntegerValue", 456)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueLessThan("anIntegerValue", 457).endOr();

    verifyQueryResults(query, 1);

  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByLongCaseInstanceVariableValueLessThanOr() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aLongValue", (long) 789)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueLessThan("aLongValue", (long) 790).endOr();

    verifyQueryResults(query, 1);

  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByDateCaseInstanceVariableValueLessThanOr() {
    String caseDefinitionId = getCaseDefinitionId();

    Date now = new Date();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aDateValue", now)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    Date after = new Date(now.getTime() + 100000);

    query.startOr().caseInstanceVariableValueLessThan("aDateValue", after).endOr();

    verifyQueryResults(query, 1);

  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByDoubleCaseInstanceVariableValueLessThanOr() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aDoubleValue", 1.5)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueLessThan("aDoubleValue", 1.6).endOr();

    verifyQueryResults(query, 1);

  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByByteArrayCaseInstanceVariableValueLessThanOr() {
    String caseDefinitionId = getCaseDefinitionId();

    byte[] bytes = "somebytes".getBytes();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aByteArrayValue", bytes)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    try {
      query.startOr().caseInstanceVariableValueLessThan("aByteArrayValue", bytes).endOr().list();
      fail();
    } catch (ProcessEngineException e) {}
  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryBySerializableCaseInstanceVariableLessThanOr() {
    String caseDefinitionId = getCaseDefinitionId();

    List<String> serializable = new ArrayList<String>();
    serializable.add("one");
    serializable.add("two");
    serializable.add("three");

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aSerializableValue", serializable)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    try {
      query.startOr().caseInstanceVariableValueLessThan("aSerializableValue", serializable).endOr().list();
      fail();
    } catch (ProcessEngineException e) {}
  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByFileCaseInstanceVariableValueLessThanOr() {
    FileValue fileValue = createDefaultFileValue();
    String variableName = "aFileValue";

    startDefaultCaseWithVariable(fileValue, variableName);
    TaskQuery query = taskService.createTaskQuery();
    try {
      query.startOr().caseInstanceVariableValueLessThan(variableName, fileValue).endOr().list();
      fail();
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage(), containsString("Variables of type File cannot be used to query"));
    }
  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByNullCaseInstanceVariableValueLessThanOrEqualOr() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aNullValue", null)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    try {
      query.startOr().caseInstanceVariableValueLessThanOrEquals("aNullValue", null).endOr().list();
      fail();
    } catch (ProcessEngineException e) {}

  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByStringCaseInstanceVariableValueLessThanOrEqualOr() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aStringValue", "abc")
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueLessThanOrEquals("aStringValue", "abd").endOr();

    verifyQueryResults(query, 1);

    query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueLessThanOrEquals("aStringValue", "abc").endOr();

    verifyQueryResults(query, 1);

  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByBooleanCaseInstanceVariableValueLessThanOrEqualOr() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aBooleanValue", true)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    try {
      query.startOr().caseInstanceVariableValueLessThanOrEquals("aBooleanValue", false).endOr().list();
      fail();
    } catch (ProcessEngineException e) {}

  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByShortCaseInstanceVariableValueLessThanOrEqualOr() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aShortValue", (short) 123)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueLessThanOrEquals("aShortValue", (short) 124).endOr();

    verifyQueryResults(query, 1);

    query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueLessThanOrEquals("aShortValue", (short) 123).endOr();

    verifyQueryResults(query, 1);

  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByIntegerCaseInstanceVariableValueLessThanOrEqualsOr() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("anIntegerValue", 456)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueLessThanOrEquals("anIntegerValue", 457).endOr();

    verifyQueryResults(query, 1);

    query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueLessThanOrEquals("anIntegerValue", 456).endOr();

    verifyQueryResults(query, 1);

  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByLongCaseInstanceVariableValueLessThanOrEqualOr() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aLongValue", (long) 789)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueLessThanOrEquals("aLongValue", (long) 790).endOr();

    verifyQueryResults(query, 1);

    query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueLessThanOrEquals("aLongValue", (long) 789).endOr();

    verifyQueryResults(query, 1);

  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByDateCaseInstanceVariableValueLessThanOrEqualOr() {
    String caseDefinitionId = getCaseDefinitionId();

    Date now = new Date();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aDateValue", now)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    Date after = new Date(now.getTime() + 100000);

    query.startOr().caseInstanceVariableValueLessThanOrEquals("aDateValue", after).endOr();

    verifyQueryResults(query, 1);

    query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueLessThanOrEquals("aDateValue", now).endOr();

    verifyQueryResults(query, 1);

  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByDoubleCaseInstanceVariableValueLessThanOrEqualOr() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aDoubleValue", 1.5)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueLessThanOrEquals("aDoubleValue", 1.6).endOr();

    verifyQueryResults(query, 1);

    query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueLessThanOrEquals("aDoubleValue", 1.5).endOr();

    verifyQueryResults(query, 1);

  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByByteArrayCaseInstanceVariableValueLessThanOrEqualOr() {
    String caseDefinitionId = getCaseDefinitionId();

    byte[] bytes = "somebytes".getBytes();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aByteArrayValue", bytes)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    try {
      query.startOr().caseInstanceVariableValueLessThanOrEquals("aByteArrayValue", bytes).endOr().list();
      fail();
    } catch (ProcessEngineException e) {}
  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryBySerializableCaseInstanceVariableLessThanOrEqualOr() {
    String caseDefinitionId = getCaseDefinitionId();

    List<String> serializable = new ArrayList<String>();
    serializable.add("one");
    serializable.add("two");
    serializable.add("three");

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aSerializableValue", serializable)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    try {
      query.startOr().caseInstanceVariableValueLessThanOrEquals("aSerializableValue", serializable).endOr().list();
      fail();
    } catch (ProcessEngineException e) {}
  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByFileCaseInstanceVariableValueLessThanOrEqualOr() {
    FileValue fileValue = createDefaultFileValue();
    String variableName = "aFileValue";

    startDefaultCaseWithVariable(fileValue, variableName);
    TaskQuery query = taskService.createTaskQuery();
    try {
      query.startOr().caseInstanceVariableValueLessThanOrEquals(variableName, fileValue).endOr().list();
      fail();
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage(), containsString("Variables of type File cannot be used to query"));
    }
  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByNullCaseInstanceVariableValueLikeOr() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aNullValue", null)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    try {
      query.startOr().caseInstanceVariableValueLike("aNullValue", null).endOr().list();
      fail();
    } catch (ProcessEngineException e) {}

  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByStringCaseInstanceVariableValueLikeOr() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aStringValue", "abc")
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueLike("aStringValue", "ab%").endOr();

    verifyQueryResults(query, 1);

    query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueLike("aStringValue", "%bc").endOr();

    verifyQueryResults(query, 1);

    query = taskService.createTaskQuery();

    query.startOr().caseInstanceVariableValueLike("aStringValue", "%b%").endOr();

    verifyQueryResults(query, 1);
  }

  @Deployment(resources="org/camunda/bpm/engine/test/api/task/TaskQueryTest.testQueryByVariableInParallelBranch.bpmn20.xml")
  public void testQueryByVariableInParallelBranchOr() throws Exception {
    runtimeService.startProcessInstanceByKey("parallelGateway");

    // when there are two process variables of the same name but different types
    Execution task1Execution = runtimeService.createExecutionQuery().activityId("task1").singleResult();
    runtimeService.setVariableLocal(task1Execution.getId(), "var", 12345L);
    Execution task2Execution = runtimeService.createExecutionQuery().activityId("task2").singleResult();
    runtimeService.setVariableLocal(task2Execution.getId(), "var", 12345);

    // then the task query should be able to filter by both variables and return both tasks
    assertEquals(2, taskService.createTaskQuery().startOr().processVariableValueEquals("var", 12345).endOr().count());
    assertEquals(2, taskService.createTaskQuery().startOr().processVariableValueEquals("var", 12345L).endOr().count());
  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/task/oneTaskWithFormKeyProcess.bpmn20.xml"})
  public void testInitializeFormKeysOr() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");

    // if initializeFormKeys
    Task task = taskService.createTaskQuery()
      .startOr()
      .processInstanceId(processInstance.getId())
      .initializeFormKeys()
      .endOr()
      .singleResult();

    // then the form key is present
    assertEquals("exampleFormKey", task.getFormKey());

    // if NOT initializeFormKeys
    task = taskService.createTaskQuery()
      .startOr()
      .processInstanceId(processInstance.getId())
      .endOr()
      .singleResult();

    try {
      // then the form key is not retrievable
      task.getFormKey();
      fail("exception expected.");
    } catch (BadUserRequestException e) {
      assertEquals("ENGINE-03052 The form key is not initialized. You must call initializeFormKeys() on the task query before you can retrieve the form key.", e.getMessage());
    }
  }

  public void testQueryWithCandidateUsersOr() {
    BpmnModelInstance process = Bpmn.createExecutableProcess("process")
      .startEvent()
      .userTask()
      .camundaCandidateUsers("anna")
      .endEvent()
      .done();

    deployment(process);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    List<Task> tasks = taskService.createTaskQuery()
      .processInstanceId(processInstance.getId())
      .startOr()
      .withCandidateUsers()
      .endOr()
      .list();
    assertEquals(1, tasks.size());
  }

  public void testQueryWithOrWithoutCandidateUsers() {
    BpmnModelInstance processDefinitionWithoutCandidateUser = Bpmn.createExecutableProcess("processDefinitionWithoutCandidateUser")
      .startEvent()
      .userTask()
      .camundaCandidateGroups("sales")
      .endEvent()
      .done();

    deployment(processDefinitionWithoutCandidateUser);

    BpmnModelInstance processDefinitionWithCandidateUser = Bpmn.createExecutableProcess("processDefinitionWithCandidateUser")
      .startEvent()
      .userTask()
      .camundaCandidateUsers("anna")
      .endEvent()
      .done();

    deployment(processDefinitionWithCandidateUser);

    runtimeService.startProcessInstanceByKey("processDefinitionWithoutCandidateUser");
    runtimeService.startProcessInstanceByKey("processDefinitionWithCandidateUser");

    List<Task> tasks = taskService.createTaskQuery()
      .processDefinitionKeyIn("processDefinitionWithoutCandidateUser", "processDefinitionWithCandidateUser")
      .startOr()
      .withCandidateUsers()
      .withoutCandidateUsers()
      .endOr()
      .list();

    assertEquals(2, tasks.size());
  }

  public void testQueryWithoutCandidateUsersOr() {
    BpmnModelInstance process = Bpmn.createExecutableProcess("process")
      .startEvent()
      .userTask()
      .camundaCandidateGroups("sales")
      .endEvent()
      .done();

    deployment(process);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    List<Task> tasks = taskService.createTaskQuery()
      .processInstanceId(processInstance.getId())
      .startOr()
      .withoutCandidateUsers()
      .endOr()
      .list();
    assertEquals(1, tasks.size());
  }

  public void testQueryAssignedTasksWithCandidateUsersOr() {
    BpmnModelInstance process = Bpmn.createExecutableProcess("process")
      .startEvent()
      .userTask()
      .camundaCandidateGroups("sales")
      .endEvent()
      .done();

    deployment(process);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    try{
      taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .startOr()
        .includeAssignedTasks()
        .withCandidateUsers()
        .endOr()
        .list();
      fail("exception expected");
    } catch (ProcessEngineException e) {}
  }

  public void testQueryAssignedTasksWithoutCandidateUsersOr() {
    BpmnModelInstance process = Bpmn.createExecutableProcess("process")
      .startEvent()
      .userTask()
      .camundaCandidateGroups("sales")
      .endEvent()
      .done();

    deployment(process);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    try{
      taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .startOr()
        .includeAssignedTasks()
        .withoutCandidateUsers()
        .endOr()
        .list();
      fail("exception expected");
    } catch (ProcessEngineException e) {}
  }

  public void testQueryAssignedTasksOrWithoutCandidateUsers() {
    BpmnModelInstance processDefinitionWithoutCandidateUser = Bpmn.createExecutableProcess("processDefinitionWithoutCandidateUser")
      .startEvent()
      .userTask()
      .camundaAssignee("tassilo")
      .endEvent()
      .done();

    deployment(processDefinitionWithoutCandidateUser);

    BpmnModelInstance processDefinitionWithCandidateUser = Bpmn.createExecutableProcess("processDefinitionWithCandidateUser")
      .startEvent()
      .userTask()
      .camundaCandidateGroups("sales")
      .endEvent()
      .done();

    deployment(processDefinitionWithCandidateUser);

    runtimeService.startProcessInstanceByKey("processDefinitionWithoutCandidateUser");
    runtimeService.startProcessInstanceByKey("processDefinitionWithCandidateUser");

    List<Task> tasks = taskService.createTaskQuery()
      .processDefinitionKeyIn("processDefinitionWithoutCandidateUser", "processDefinitionWithCandidateUser")
      .startOr()
      .taskAssigned()
      .withoutCandidateUsers()
      .endOr()
      .list();
    System.out.println(tasks);
    assertEquals(2, tasks.size());
  }

  public void testQueryTaskIds() {
    BpmnModelInstance processDefinition1 = Bpmn.createExecutableProcess("processDefinition1")
      .startEvent()
      .userTask()
      .endEvent()
      .done();

    deployment(processDefinition1);

    BpmnModelInstance processDefinition2 = Bpmn.createExecutableProcess("processDefinition2")
      .startEvent()
      .userTask()
      .endEvent()
      .done();

    deployment(processDefinition2);

    runtimeService.startProcessInstanceByKey("processDefinition1");
    runtimeService.startProcessInstanceByKey("processDefinition2");

    List<Task> tasks = taskService.createTaskQuery()
      .startOr()
        .taskId(taskService.createTaskQuery().processDefinitionKey("processDefinition1").singleResult().getId())
        .taskId(taskService.createTaskQuery().processDefinitionKey("processDefinition2").singleResult().getId())
      .endOr()
      .list();

    assertEquals(2, tasks.size());
  }

  public void testQueryTaskNames() {
    BpmnModelInstance processDefinition1 = Bpmn.createExecutableProcess("processDefinition1")
      .startEvent()
        .userTask()
          .name("Task 1")
      .endEvent()
      .done();

    deployment(processDefinition1);

    BpmnModelInstance processDefinition2 = Bpmn.createExecutableProcess("processDefinition2")
      .startEvent()
        .userTask()
          .name("Task 2")
      .endEvent()
      .done();

    deployment(processDefinition2);

    runtimeService.startProcessInstanceByKey("processDefinition1");
    runtimeService.startProcessInstanceByKey("processDefinition2");

    List<Task> tasks = taskService.createTaskQuery()
      .startOr()
        .taskName(taskService.createTaskQuery().processDefinitionKey("processDefinition1").singleResult().getName())
        .taskName(taskService.createTaskQuery().processDefinitionKey("processDefinition2").singleResult().getName())
      .endOr()
      .list();

    assertEquals(2, tasks.size());
  }

  public void testQueryTaskNamesNotEqual() {
    BpmnModelInstance processDefinition1 = Bpmn.createExecutableProcess("processDefinition1")
      .startEvent()
        .userTask()
          .name("Task 1")
      .endEvent()
      .done();

    deployment(processDefinition1);

    BpmnModelInstance processDefinition2 = Bpmn.createExecutableProcess("processDefinition2")
      .startEvent()
        .userTask()
          .name("Task 2")
      .endEvent()
      .done();

    deployment(processDefinition2);

    runtimeService.startProcessInstanceByKey("processDefinition1");
    runtimeService.startProcessInstanceByKey("processDefinition2");

    List<Task> tasks = taskService.createTaskQuery()
      .processDefinitionKeyIn("processDefinition1", "processDefinition2")
      .startOr()
        .taskNameNotEqual(taskService.createTaskQuery().processDefinitionKey("processDefinition1").singleResult().getName())
        .taskNameNotEqual(taskService.createTaskQuery().processDefinitionKey("processDefinition2").singleResult().getName())
      .endOr()
      .list();

    assertEquals(0, tasks.size());
  }

  public void testQueryTaskNamesLike() {
    BpmnModelInstance processDefinition1 = Bpmn.createExecutableProcess("processDefinition1")
      .startEvent()
        .userTask()
          .name("Task 1 waits for user")
      .endEvent()
      .done();

    deployment(processDefinition1);

    BpmnModelInstance processDefinition2 = Bpmn.createExecutableProcess("processDefinition2")
      .startEvent()
        .userTask()
          .name("Task 2 waits for user")
      .endEvent()
      .done();

    deployment(processDefinition2);

    runtimeService.startProcessInstanceByKey("processDefinition1");
    runtimeService.startProcessInstanceByKey("processDefinition2");

    List<Task> tasks = taskService.createTaskQuery()
      .processDefinitionKeyIn("processDefinition1", "processDefinition2")
      .startOr()
        .taskNameLike("Task 1%")
        .taskNameLike("Task 2%")
      .endOr()
      .list();

    assertEquals(2, tasks.size());
  }

  public void testQueryTaskNamesNotLike() {
    BpmnModelInstance processDefinition1 = Bpmn.createExecutableProcess("processDefinition1")
      .startEvent()
        .userTask()
          .name("Task 1 waits for user")
      .endEvent()
      .done();

    deployment(processDefinition1);

    BpmnModelInstance processDefinition2 = Bpmn.createExecutableProcess("processDefinition2")
      .startEvent()
        .userTask()
          .name("Task 2 waits for user")
      .endEvent()
      .done();

    deployment(processDefinition2);

    runtimeService.startProcessInstanceByKey("processDefinition1");
    runtimeService.startProcessInstanceByKey("processDefinition2");

    List<Task> tasks = taskService.createTaskQuery()
      .processDefinitionKeyIn("processDefinition1", "processDefinition2")
      .startOr()
        .taskNameNotLike("Task 1%")
        .taskNameNotLike("Task 2%")
      .endOr()
      .list();

    assertEquals(2, tasks.size());
  }

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

  public void testQueryTaskProcessInstanceIds() {
    // given
    BpmnModelInstance processDefinition1 = Bpmn.createExecutableProcess("processDefinition1")
      .startEvent()
        .userTask()
      .endEvent()
      .done();

    deployment(processDefinition1);

    BpmnModelInstance processDefinition2 = Bpmn.createExecutableProcess("processDefinition2")
      .startEvent()
        .userTask()
      .endEvent()
      .done();

    deployment(processDefinition2);

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
  }

  public void testQueryTaskProcessInstanceBusinessKeys() {
    // given
    BpmnModelInstance processDefinition1 = Bpmn.createExecutableProcess("processDefinition1")
      .startEvent()
        .userTask()
      .endEvent()
      .done();

    deployment(processDefinition1);

    BpmnModelInstance processDefinition2 = Bpmn.createExecutableProcess("processDefinition2")
      .startEvent()
        .userTask()
      .endEvent()
      .done();

    deployment(processDefinition2);

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
  }

  public void testQueryTaskProcessInstanceBusinessKeysLike() {
    // given
    BpmnModelInstance processDefinition1 = Bpmn.createExecutableProcess("processDefinition1")
      .startEvent()
        .userTask()
      .endEvent()
      .done();

    deployment(processDefinition1);

    BpmnModelInstance processDefinition2 = Bpmn.createExecutableProcess("processDefinition2")
      .startEvent()
        .userTask()
      .endEvent()
      .done();

    deployment(processDefinition2);

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
  }

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

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn"})
  public void testQueryTaskCaseInstanceBusinessKey() {
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

  public void testQueryCreateTimes() throws ParseException {
    // given
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");
    ClockUtil.setCurrentTime(sdf.parse("06/06/2006 06:06:06.000"));

    Task task1 = taskService.newTask();
    taskService.saveTask(task1);
    taskIds.add(task1.getId());

    Task task2 = taskService.newTask();
    taskService.saveTask(task2);
    taskIds.add(task2.getId());

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

    taskService.deleteTask(parentTaskId1, true);
    taskService.deleteTask(parentTaskId2, true);
  }

  public void testQueryTaskProcessDefinitionIds() {
    // given
    BpmnModelInstance processDefinition1 = Bpmn.createExecutableProcess("processDefinition1")
      .startEvent()
        .userTask()
      .endEvent()
      .done();

    deployment(processDefinition1);

    BpmnModelInstance processDefinition2 = Bpmn.createExecutableProcess("processDefinition2")
      .startEvent()
        .userTask()
      .endEvent()
      .done();

    deployment(processDefinition2);

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
  }

  public void testQueryTaskProcessDefinitionKeys() {
    // given
    BpmnModelInstance processDefinition1 = Bpmn.createExecutableProcess("processDefinition1")
      .startEvent()
        .userTask()
      .endEvent()
      .done();

    deployment(processDefinition1);

    BpmnModelInstance processDefinition2 = Bpmn.createExecutableProcess("processDefinition2")
      .startEvent()
        .userTask()
      .endEvent()
      .done();

    deployment(processDefinition2);

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
  }

  public void testQueryTaskProcessDefinitionNames() {
    // given
    BpmnModelInstance processDefinition1 = Bpmn.createExecutableProcess("processDefinition1")
      .name("process def name 1")
      .startEvent()
        .userTask()
      .endEvent()
      .done();

    deployment(processDefinition1);

    BpmnModelInstance processDefinition2 = Bpmn.createExecutableProcess("processDefinition2")
      .name("process def name 2")
      .startEvent()
        .userTask()
      .endEvent()
      .done();

    deployment(processDefinition2);

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
  }

  public void testQueryTaskProcessDefinitionNamesLike() {
    // given
    BpmnModelInstance processDefinition1 = Bpmn.createExecutableProcess("processDefinition1")
      .name("process def name 1")
      .startEvent()
        .userTask()
      .endEvent()
      .done();

    deployment(processDefinition1);

    BpmnModelInstance processDefinition2 = Bpmn.createExecutableProcess("processDefinition2")
      .name("process def name 2")
      .startEvent()
        .userTask()
      .endEvent()
      .done();

    deployment(processDefinition2);

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
  }

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
    taskService.deleteTask(task1.getId());
    taskService.deleteTask(task2.getId());
  }

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

  public void testQueryTaskInvolvedUsers() throws ParseException {
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

  @Deployment(resources={
    "org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn",
    "org/camunda/bpm/engine/test/api/cmmn/oneTaskCase2.cmmn"})
  public void testQueryLongCaseInstanceVariableValueEquals() {
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

  public void testExtendTaskQueryList_ProcessDefinitionKeyIn() {
    // given
    String processDefinitionKey = "invoice";
    TaskQuery query = taskService
      .createTaskQuery()
      .startOr()
      .processDefinitionKeyIn(processDefinitionKey);

    TaskQuery extendingQuery = taskService.createTaskQuery().endOr();

    // when
    TaskQuery result = ((TaskQueryImpl)query).extend(extendingQuery);

    // then
    String[] processDefinitionKeys = ((TaskQueryImpl) result).getProcessDefinitionKeys();
    assertEquals(1, processDefinitionKeys.length);
    assertEquals(processDefinitionKey, processDefinitionKeys[0]);
  }

  public void testQueryOr() {
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

    assertEquals(3, tasks.size());
  }

  public void testLogicalExpressionQuery() {
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

  /**
   * Starts the one deployed case at the point of the manual activity PI_HumanTask_1
   * with the given variable.
   */
  protected void startDefaultCaseWithVariable(Object variableValue, String variableName) {
    String caseDefinitionId = getCaseDefinitionId();
    createCaseWithVariable(caseDefinitionId, variableValue, variableName);
  }

  /**
   * @return the case definition id if only one case is deployed.
   */
  protected String getCaseDefinitionId() {
    String caseDefinitionId = repositoryService
      .createCaseDefinitionQuery()
      .singleResult()
      .getId();
    return caseDefinitionId;
  }

  protected void createCaseWithVariable(String caseDefinitionId, Object variableValue, String variableName) {
    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable(variableName, variableValue)
      .create();
  }


  private void verifyQueryResults(TaskQuery query, int countExpected) {
    assertEquals(countExpected, query.list().size());
    assertEquals(countExpected, query.count());

    if (countExpected == 1) {
      assertNotNull(query.singleResult());
    } else if (countExpected > 1){
      verifySingleResultFails(query);
    } else if (countExpected == 0) {
      assertNull(query.singleResult());
    }
  }

  private void verifySingleResultFails(TaskQuery query) {
    try {
      query.singleResult();
      fail();
    } catch (ProcessEngineException e) {}
  }

  /**
   * @return
   */
  protected FileValue createDefaultFileValue() {
    FileValue fileValue = Variables.fileValue("tst.txt").file("somebytes".getBytes()).create();
    return fileValue;
  }

  /**
   * Generates some test tasks.
   * - 6 tasks where kermit is a candidate
   * - 1 tasks where gonzo is assignee and kermit and gonzo are candidates
   * - 2 tasks assigned to management group
   * - 2 tasks assigned to accountancy group
   * - 1 task assigned to fozzie and to both the management and accountancy group
   */
  private List<String> generateTestTasks() throws Exception {
    List<String> ids = new ArrayList<String>();

    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");
    // 6 tasks for kermit
    ClockUtil.setCurrentTime(sdf.parse("01/01/2001 01:01:01.000"));
    for (int i = 0; i < 6; i++) {
      Task task = taskService.newTask();
      task.setName("testTask");
      task.setDescription("testTask description");
      task.setPriority(3);
      taskService.saveTask(task);
      ids.add(task.getId());
      taskService.addCandidateUser(task.getId(), "kermit");
    }

    ClockUtil.setCurrentTime(sdf.parse("02/02/2002 02:02:02.000"));
    // 1 task for gonzo
    Task task = taskService.newTask();
    task.setName("gonzo_Task");
    task.setDescription("gonzo_description");
    task.setPriority(4);
    taskService.saveTask(task);
    taskService.setAssignee(task.getId(), "gonzo_");
    taskService.setVariable(task.getId(), "testVar", "someVariable");
    taskService.addCandidateUser(task.getId(), "kermit");
    taskService.addCandidateUser(task.getId(), "gonzo");
    ids.add(task.getId());

    ClockUtil.setCurrentTime(sdf.parse("03/03/2003 03:03:03.000"));
    // 2 tasks for management group
    for (int i = 0; i < 2; i++) {
      task = taskService.newTask();
      task.setName("managementTask");
      task.setPriority(10);
      taskService.saveTask(task);
      taskService.addCandidateGroup(task.getId(), "management");
      ids.add(task.getId());
    }

    ClockUtil.setCurrentTime(sdf.parse("04/04/2004 04:04:04.000"));
    // 2 tasks for accountancy group
    for (int i = 0; i < 2; i++) {
      task = taskService.newTask();
      task.setName("accountancyTask");
      task.setName("accountancy description");
      taskService.saveTask(task);
      taskService.addCandidateGroup(task.getId(), "accountancy");
      ids.add(task.getId());
    }

    ClockUtil.setCurrentTime(sdf.parse("05/05/2005 05:05:05.000"));
    // 1 task assigned to management and accountancy group
    task = taskService.newTask();
    task.setName("managementAndAccountancyTask");
    taskService.saveTask(task);
    taskService.setAssignee(task.getId(), "fozzie");
    taskService.addCandidateGroup(task.getId(), "management");
    taskService.addCandidateGroup(task.getId(), "accountancy");
    ids.add(task.getId());

    return ids;
  }

  /**
   * Starts the case execution for oneTaskCase.cmmn<p>
   * Only works for testcases, which deploy that process.
   *
   * @return the execution id for the activity PI_HumanTask_1
   */
  protected String startDefaultCaseExecutionManually() {
    String humanTaskExecutionId = caseService
      .createCaseExecutionQuery()
      .activityId("PI_HumanTask_1")
      .singleResult()
      .getId();

    caseService
      .withCaseExecution(humanTaskExecutionId)
      .manualStart();
    return humanTaskExecutionId;
  }

}
