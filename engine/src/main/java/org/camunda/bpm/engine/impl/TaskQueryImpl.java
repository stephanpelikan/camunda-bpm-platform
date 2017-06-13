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
package org.camunda.bpm.engine.impl;

import static org.camunda.bpm.engine.impl.util.EnsureUtil.ensureNotEmpty;
import static org.camunda.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

import java.util.*;

import org.apache.ibatis.exceptions.PersistenceException;
import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.interceptor.CommandExecutor;
import org.camunda.bpm.engine.impl.persistence.entity.SuspensionState;
import org.camunda.bpm.engine.impl.persistence.entity.TaskEntity;
import org.camunda.bpm.engine.impl.variable.serializer.VariableSerializers;
import org.camunda.bpm.engine.task.DelegationState;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.task.TaskQuery;
import org.camunda.bpm.engine.variable.type.ValueType;

/**
 * @author Joram Barrez
 * @author Tom Baeyens
 * @author Falko Menge
 */
public class TaskQueryImpl extends AbstractQuery<TaskQuery, Task> implements TaskQuery {

  private static final long serialVersionUID = 1L;
  protected ArrayList<String> taskIds;
  protected ArrayList<String> names;
  protected ArrayList<String> namesNotEqual;
  protected ArrayList<String> namesLike;
  protected ArrayList<String> namesNotLike;
  protected ArrayList<String> descriptions;
  protected ArrayList<String> descriptionsLike;
  protected ArrayList<Integer> priorities;
  protected Integer minPriority;
  protected Integer maxPriority;
  protected ArrayList<String> assignees;
  protected ArrayList<String> assigneesLike;
  protected ArrayList<String> involvedUsers;
  protected ArrayList<String> owners;

  // Todo
  protected ArrayList<Boolean> unassignedTasks;
  protected ArrayList<Boolean> assignedTasks;

  protected ArrayList<DelegationState> delegationStates;
  protected String candidateUser;
  protected String candidateGroup;
  protected List<String> candidateGroups;
  protected Boolean withCandidateGroups;
  protected Boolean withoutCandidateGroups;
  protected Boolean withCandidateUsers;
  protected Boolean withoutCandidateUsers;
  protected Boolean includeAssignedTasks;
  protected ArrayList<String> processInstanceIds;
  protected ArrayList<String> executionIds;
  protected ArrayList<String> activityInstanceIds;
  protected ArrayList<Date> createTimes;
  protected Date createTimeBefore;
  protected Date createTimeAfter;
  protected ArrayList<String> keys;
  protected ArrayList<String> keysLike;
  protected ArrayList<String> taskDefinitionKeys;
  protected ArrayList<String> processDefinitionKeys;
  protected ArrayList<String> processDefinitionKeysIn;
  protected ArrayList<String> processDefinitionIds;
  protected ArrayList<String> processDefinitionNames;
  protected ArrayList<String> processDefinitionNamesLike;
  protected ArrayList<String> processInstanceBusinessKeys;
  protected ArrayList<String> processInstanceBusinessKeysIn;
  protected ArrayList<String> processInstanceBusinessKeysLike;
  protected List<TaskQueryVariableValue> variables = new ArrayList<TaskQueryVariableValue>();
  protected ArrayList<Date> dueDates;
  protected Date dueBefore;
  protected Date dueAfter;
  protected ArrayList<Date> followUpDates;
  protected Date followUpBefore;
  protected boolean followUpNullAccepted=false;
  protected Date followUpAfter;
  protected boolean excludeSubtasks = false;
  protected ArrayList<SuspensionState> suspensionStates;
  protected boolean initializeFormKeys = false;
  protected boolean taskNameCaseInsensitive = false;

  protected ArrayList<String> parentTaskIds;
  protected boolean isTenantIdSet = false;
  protected ArrayList<String> tenantIds;

  // case management /////////////////////////////
  protected ArrayList<String> caseDefinitionKeys;
  protected ArrayList<String> caseDefinitionIds;
  protected ArrayList<String> caseDefinitionNames;
  protected ArrayList<String> caseDefinitionNamesLike;
  protected ArrayList<String> caseInstanceIds;
  protected ArrayList<String> caseInstanceBusinessKeys;
  protected ArrayList<String> caseInstanceBusinessKeysLike;
  protected ArrayList<String> caseExecutionIds;

  // logical expression query /////////////////////////////
  protected ArrayList<TaskQueryImpl> logicalExpressionQueryList = null;
  protected TaskQueryImpl logicalExpressionQueryRootNode = null;
  protected List<TaskQueryImpl> logicalExpressionQueryChildren = new ArrayList<TaskQueryImpl>();
  protected TaskQueryImpl parentLogicalExpressionQuery = null;
  protected logicalExpressionEnum logicalExpression = logicalExpressionEnum.and;
  protected enum logicalExpressionEnum {and, or}

  public TaskQueryImpl() {
    logicalExpressionQueryRootNode = this;
    logicalExpressionQueryRootNode.logicalExpressionQueryList = new ArrayList<TaskQueryImpl>();
    logicalExpressionQueryRootNode.logicalExpressionQueryList.add(this);
  }

  public TaskQueryImpl(CommandExecutor commandExecutor) {
    super(commandExecutor);

    logicalExpressionQueryRootNode = this;
    logicalExpressionQueryRootNode.logicalExpressionQueryList = new ArrayList<TaskQueryImpl>();
    logicalExpressionQueryRootNode.logicalExpressionQueryList.add(this);
  }

  public TaskQueryImpl(logicalExpressionEnum logicalExpression, TaskQueryImpl parentLogicalExpressionQuery) {
    this.logicalExpression = logicalExpression;
    this.parentLogicalExpressionQuery = parentLogicalExpressionQuery;
    logicalExpressionQueryRootNode = parentLogicalExpressionQuery.logicalExpressionQueryRootNode;
    logicalExpressionQueryRootNode.logicalExpressionQueryList.add(this);
  }

  @Override
  public TaskQueryImpl taskId(String taskId) {
    ensureNotNull("Task id", taskId);

    if (taskIds == null) {
      taskIds = new ArrayList<String>();
    }

    taskIds.add(taskId);

    return this;
  }

  @Override
  public TaskQueryImpl taskName(String name) {
    if (names == null) {
      names = new ArrayList<String>();
    }

    names.add(name);

    return this;
  }

  @Override
  public TaskQueryImpl taskNameLike(String nameLike) {
    ensureNotNull("Task nameLike", nameLike);

    if (namesLike == null) {
      namesLike = new ArrayList<String>();
    }

    namesLike.add(nameLike);

    return this;
  }

  @Override
  public TaskQueryImpl taskDescription(String description) {
    ensureNotNull("Description", description);

    if (descriptions == null) {
      descriptions = new ArrayList<String>();
    }

    descriptions.add(description);

    return this;
  }

  @Override
  public TaskQuery taskDescriptionLike(String descriptionLike) {
    ensureNotNull("Task descriptionLike", descriptionLike);

    if (descriptionsLike == null) {
      descriptionsLike = new ArrayList<String>();
    }

    descriptionsLike.add(descriptionLike);

    return this;
  }

  @Override
  public TaskQuery taskPriority(Integer priority) {
    ensureNotNull("Priority", priority);

    if (priorities == null) {
      priorities = new ArrayList<Integer>();
    }

    priorities.add(priority);

    return this;
  }

  @Override
  public TaskQuery taskMinPriority(Integer minPriority) {
    ensureNotNull("Min Priority", minPriority);
    this.minPriority = minPriority;
    return this;
  }

  @Override
  public TaskQuery taskMaxPriority(Integer maxPriority) {
    ensureNotNull("Max Priority", maxPriority);
    this.maxPriority = maxPriority;
    return this;
  }

  @Override
  public TaskQueryImpl taskAssignee(String assignee) {
    ensureNotNull("Assignee", assignee);

    if (assignees == null) {
      assignees = new ArrayList<String>();
    }

    assignees.add(assignee);

    expressions.remove("taskAssignee");
    return this;
  }

  @Override
  public TaskQuery taskAssigneeExpression(String assigneeExpression) {
    ensureNotNull("Assignee expression", assigneeExpression);
    expressions.put("taskAssignee", assigneeExpression);
    return this;
  }

  @Override
  public TaskQuery taskAssigneeLike(String assignee) {
    ensureNotNull("Assignee", assignee);

    if (assigneesLike == null) {
      assigneesLike = new ArrayList<String>();
    }

    assigneesLike.add(assignee);

    expressions.remove("taskAssigneeLike");
    return this;
  }

  @Override
  public TaskQuery taskAssigneeLikeExpression(String assigneeLikeExpression) {
    ensureNotNull("Assignee like expression", assigneeLikeExpression);
    expressions.put("taskAssigneeLike", assigneeLikeExpression);
    return this;
  }

  @Override
  public TaskQueryImpl taskOwner(String owner) {
    ensureNotNull("Owner", owner);

    if (owners == null) {
      owners = new ArrayList<String>();
    }

    owners.add(owner);

    expressions.remove("taskOwner");
    return this;
  }

  @Override
  public TaskQuery taskOwnerExpression(String ownerExpression) {
    ensureNotNull("Owner expression", ownerExpression);
    expressions.put("taskOwner", ownerExpression);
    return this;
  }

  /** @see {@link #taskUnassigned} */
  @Override
  @Deprecated
  public TaskQuery taskUnnassigned() {
    return taskUnassigned();
  }

  @Override
  public TaskQuery taskUnassigned() {
    if (unassignedTasks == null) {
      unassignedTasks = new ArrayList<Boolean>();
    }

    unassignedTasks.add(true);

    return this;
  }

  @Override
  public TaskQuery taskAssigned() {
    if (assignedTasks == null) {
      assignedTasks = new ArrayList<Boolean>();
    }

    assignedTasks.add(true);

    return this;
  }

  @Override
  public TaskQuery taskDelegationState(DelegationState delegationState) {
    if (delegationStates == null) {
      delegationStates = new ArrayList<DelegationState>();
    }

    delegationStates.add(delegationState);

    return this;
  }

  @Override
  public TaskQueryImpl taskCandidateUser(String candidateUser) {
    ensureNotNull("Candidate user", candidateUser);

    if(logicalExpression == logicalExpressionEnum.and) {
      if (candidateGroup != null || expressions.containsKey("taskCandidateGroup")) {
        throw new ProcessEngineException("Invalid query usage: cannot set both candidateUser and candidateGroup");
      }
      if (candidateGroups != null || expressions.containsKey("taskCandidateGroupIn")) {
        throw new ProcessEngineException("Invalid query usage: cannot set both candidateUser and candidateGroupIn");
      }
    }
    this.candidateUser = candidateUser;
    expressions.remove("taskCandidateUser");
    return this;
  }

  @Override
  public TaskQuery taskCandidateUserExpression(String candidateUserExpression) {
    ensureNotNull("Candidate user expression", candidateUserExpression);

    if(logicalExpression == logicalExpressionEnum.and) {
      if (candidateGroup != null || expressions.containsKey("taskCandidateGroup")) {
        throw new ProcessEngineException("Invalid query usage: cannot set both candidateUser and candidateGroup");
      }
      if (candidateGroups != null || expressions.containsKey("taskCandidateGroupIn")) {
        throw new ProcessEngineException("Invalid query usage: cannot set both candidateUser and candidateGroupIn");
      }
    }

    expressions.put("taskCandidateUser", candidateUserExpression);
    return this;
  }

  @Override
  public TaskQueryImpl taskInvolvedUser(String involvedUser) {
    ensureNotNull("Involved user", involvedUser);

    if (involvedUsers == null) {
      involvedUsers = new ArrayList<String>();
    }

    involvedUsers.add(involvedUser);

    expressions.remove("taskInvolvedUser");
    return this;
  }

  @Override
  public TaskQuery taskInvolvedUserExpression(String involvedUserExpression) {
    ensureNotNull("Involved user expression", involvedUserExpression);
    expressions.put("taskInvolvedUser", involvedUserExpression);
    return this;
  }

  @Override
  public TaskQuery withCandidateGroups() {
    this.withCandidateGroups = true;
    return this;
  }

  @Override
  public TaskQuery withoutCandidateGroups() {
    this.withoutCandidateGroups = true;
    return this;
  }

  @Override
  public TaskQuery withCandidateUsers() {
    this.withCandidateUsers = true;
    return this;
  }

  @Override
  public TaskQuery withoutCandidateUsers() {
    this.withoutCandidateUsers = true;
    return this;
  }

  @Override
  public TaskQueryImpl taskCandidateGroup(String candidateGroup) {
    ensureNotNull("Candidate group", candidateGroup);

    if(logicalExpression == logicalExpressionEnum.and) {
      if (candidateUser != null || expressions.containsKey("taskCandidateUser")) {
        throw new ProcessEngineException("Invalid query usage: cannot set both candidateGroup and candidateUser");
      }
      if (candidateGroups != null || expressions.containsKey("taskCandidateGroupIn")) {
        throw new ProcessEngineException("Invalid query usage: cannot set both candidateGroup and candidateGroupIn");
      }
    }

    this.candidateGroup = candidateGroup;
    expressions.remove("taskCandidateGroup");
    return this;
  }

  @Override
  public TaskQuery taskCandidateGroupExpression(String candidateGroupExpression) {
    ensureNotNull("Candidate group expression", candidateGroupExpression);

    if (candidateUser != null || expressions.containsKey("taskCandidateUser")) {
      throw new ProcessEngineException("Invalid query usage: cannot set both candidateGroup and candidateUser");
    }
    if (candidateGroups != null || expressions.containsKey("taskCandidateGroupIn")) {
      throw new ProcessEngineException("Invalid query usage: cannot set both candidateGroup and candidateGroupIn");
    }

    expressions.put("taskCandidateGroup", candidateGroupExpression);
    return this;
  }

  @Override
  public TaskQuery taskCandidateGroupIn(List<String> candidateGroups) {
    ensureNotEmpty("Candidate group list", candidateGroups);

    if(logicalExpression == logicalExpressionEnum.and) {
      if (candidateUser != null || expressions.containsKey("taskCandidateUser")) {
        throw new ProcessEngineException("Invalid query usage: cannot set both candidateGroupIn and candidateUser");
      }
      if (candidateGroup != null || expressions.containsKey("taskCandidateGroup")) {
        throw new ProcessEngineException("Invalid query usage: cannot set both candidateGroupIn and candidateGroup");
      }
    }

    this.candidateGroups = candidateGroups;
    expressions.remove("taskCandidateGroupIn");
    return this;
  }

  @Override
  public TaskQuery taskCandidateGroupInExpression(String candidateGroupsExpression) {
    ensureNotEmpty("Candidate group list expression", candidateGroupsExpression);

    if (candidateUser != null || expressions.containsKey("taskCandidateUser")) {
      throw new ProcessEngineException("Invalid query usage: cannot set both candidateGroupIn and candidateUser");
    }
    if (candidateGroup != null || expressions.containsKey("taskCandidateGroup")) {
      throw new ProcessEngineException("Invalid query usage: cannot set both candidateGroupIn and candidateGroup");
    }

    expressions.put("taskCandidateGroupIn", candidateGroupsExpression);
    return this;
  }

  @Override
  public TaskQuery includeAssignedTasks() {
    if (candidateUser == null && candidateGroup == null && candidateGroups == null && !isWithCandidateGroups() && !isWithoutCandidateGroups() && !isWithCandidateUsers() && !isWithoutCandidateUsers()
        && !expressions.containsKey("taskCandidateUser") && !expressions.containsKey("taskCandidateGroup")
        && !expressions.containsKey("taskCandidateGroupIn")) {
      throw new ProcessEngineException("Invalid query usage: candidateUser, candidateGroup, candidateGroupIn, withCandidateGroups, withoutCandidateGroups, withCandidateUsers, withoutCandidateUsers has to be called before 'includeAssignedTasks'.");
    }

    includeAssignedTasks = true;
    return this;
  }

  public TaskQuery includeAssignedTasksInternal() {
    includeAssignedTasks = true;
    return this;
  }

  @Override
  public TaskQueryImpl processInstanceId(String processInstanceId) {
    if (processInstanceIds == null) {
      processInstanceIds = new ArrayList<String>();
    }

    processInstanceIds.add(processInstanceId);

    return this;
  }

  @Override
  public TaskQueryImpl processInstanceBusinessKey(String processInstanceBusinessKey) {
    if (processInstanceBusinessKeys == null) {
      processInstanceBusinessKeys = new ArrayList<String>();
    }

    processInstanceBusinessKeys.add(processInstanceBusinessKey);

    return this;
  }

  @Override
  public TaskQuery processInstanceBusinessKeyIn(String... processInstanceBusinessKeys) {
    if (processInstanceBusinessKeysIn == null) {
      processInstanceBusinessKeysIn = new ArrayList<String>();
    }

    processInstanceBusinessKeysIn.addAll(Arrays.asList(processInstanceBusinessKeys));

    return this;
  }

  @Override
  public TaskQuery processInstanceBusinessKeyLike(String processInstanceBusinessKey) {
    if (processInstanceBusinessKeysLike == null) {
      processInstanceBusinessKeysLike = new ArrayList<String>();
    }

    processInstanceBusinessKeysLike.add(processInstanceBusinessKey);

    return this;
  }

  @Override
  public TaskQueryImpl executionId(String executionId) {
    if (executionIds == null) {
      executionIds = new ArrayList<String>();
    }

    executionIds.add(executionId);

    return this;
  }

  @Override
  public TaskQuery activityInstanceIdIn(String... activityInstanceIds) {
    if (activityInstanceIds == null) {
      this.activityInstanceIds = new ArrayList<String>();
    }

    this.activityInstanceIds.addAll(Arrays.asList(activityInstanceIds));

    return this;
  }

  @Override
  public TaskQuery tenantIdIn(String... tenantIds) {
    ensureNotNull("tenantIds", (Object[]) tenantIds);
    if (this.tenantIds == null) {
      this.tenantIds = new ArrayList<String>();
    }

    this.tenantIds.addAll(Arrays.asList(tenantIds));
    this.isTenantIdSet = true;
    return this;
  }

  @Override
  public TaskQuery withoutTenantId() {
    this.tenantIds = null;
    this.isTenantIdSet = true;
    return this;
  }

  @Override
  public TaskQueryImpl taskCreatedOn(Date createTime) {
    if (createTimes == null) {
      createTimes = new ArrayList<Date>();
    }

    createTimes.add(createTime);

    expressions.remove("taskCreatedOn");
    return this;
  }

  @Override
  public TaskQuery taskCreatedOnExpression(String createTimeExpression) {
    expressions.put("taskCreatedOn", createTimeExpression);
    return this;
  }

  @Override
  public TaskQuery taskCreatedBefore(Date before) {
    this.createTimeBefore = before;
    expressions.remove("taskCreatedBefore");
    return this;
  }

  @Override
  public TaskQuery taskCreatedBeforeExpression(String beforeExpression) {
    expressions.put("taskCreatedBefore", beforeExpression);
    return this;
  }

  @Override
  public TaskQuery taskCreatedAfter(Date after) {
    this.createTimeAfter = after;
    expressions.remove("taskCreatedAfter");
    return this;
  }

  @Override
  public TaskQuery taskCreatedAfterExpression(String afterExpression) {
    expressions.put("taskCreatedAfter", afterExpression);
    return this;
  }

  @Override
  public TaskQuery taskDefinitionKey(String key) {
    if (keys == null) {
      keys = new ArrayList<String>();
    }

    keys.add(key);

    return this;
  }

  @Override
  public TaskQuery taskDefinitionKeyLike(String keyLike) {
    if (keysLike == null) {
      keysLike = new ArrayList<String>();
    }

    keysLike.add(keyLike);

    return this;
  }

  @Override
  public TaskQuery taskDefinitionKeyIn(String... taskDefinitionKeys) {
    if (this.taskDefinitionKeys == null) {
      this.taskDefinitionKeys = new ArrayList<String>();
    }

    this.taskDefinitionKeys.addAll(Arrays.asList(taskDefinitionKeys));

    return this;
  }

  @Override
  public TaskQuery taskParentTaskId(String taskParentTaskId) {
    if (parentTaskIds == null) {
      parentTaskIds = new ArrayList<String>();
    }

    parentTaskIds.add(taskParentTaskId);

    return this;
  }

  @Override
  public TaskQuery caseInstanceId(String caseInstanceId) {
    ensureNotNull("caseInstanceId", caseInstanceId);

    if (caseInstanceIds == null) {
      caseInstanceIds = new ArrayList<String>();
    }

    caseInstanceIds.add(caseInstanceId);

    return this;
  }

  @Override
  public TaskQuery caseInstanceBusinessKey(String caseInstanceBusinessKey) {
    ensureNotNull("caseInstanceBusinessKey", caseInstanceBusinessKey);

    if (caseInstanceBusinessKeys == null) {
      caseInstanceBusinessKeys = new ArrayList<String>();
    }

    caseInstanceBusinessKeys.add(caseInstanceBusinessKey);

    return this;
  }

  @Override
  public TaskQuery caseInstanceBusinessKeyLike(String caseInstanceBusinessKeyLike) {
    ensureNotNull("caseInstanceBusinessKeyLike", caseInstanceBusinessKeyLike);

    if (caseInstanceBusinessKeysLike == null) {
      caseInstanceBusinessKeysLike = new ArrayList<String>();
    }

    caseInstanceBusinessKeysLike.add(caseInstanceBusinessKeyLike);

    return this;
  }

  @Override
  public TaskQuery caseExecutionId(String caseExecutionId) {
    ensureNotNull("caseExecutionId", caseExecutionId);

    if (caseExecutionIds == null) {
      caseExecutionIds = new ArrayList<String>();
    }

    caseExecutionIds.add(caseExecutionId);

    return this;
  }

  @Override
  public TaskQuery caseDefinitionId(String caseDefinitionId) {
    ensureNotNull("caseDefinitionId", caseDefinitionId);

    if (caseDefinitionIds == null) {
      caseDefinitionIds = new ArrayList<String>();
    }

    caseDefinitionIds.add(caseDefinitionId);

    return this;
  }

  @Override
  public TaskQuery caseDefinitionKey(String caseDefinitionKey) {
    ensureNotNull("caseDefinitionKey", caseDefinitionKey);

    if (caseDefinitionKeys == null) {
      caseDefinitionKeys = new ArrayList<String>();
    }

    caseDefinitionKeys.add(caseDefinitionKey);

    return this;
  }

  @Override
  public TaskQuery caseDefinitionName(String caseDefinitionName) {
    ensureNotNull("caseDefinitionName", caseDefinitionName);

    if (caseDefinitionNames == null) {
      caseDefinitionNames = new ArrayList<String>();
    }

    caseDefinitionNames.add(caseDefinitionName);

    return this;
  }

  @Override
  public TaskQuery caseDefinitionNameLike(String caseDefinitionNameLike) {
    ensureNotNull("caseDefinitionNameLike", caseDefinitionNameLike);

    if (caseDefinitionNamesLike == null) {
      caseDefinitionNamesLike = new ArrayList<String>();
    }

    caseDefinitionNamesLike.add(caseDefinitionNameLike);

    return this;
  }

  @Override
  public TaskQuery taskVariableValueEquals(String variableName, Object variableValue) {
    addVariable(variableName, variableValue, QueryOperator.EQUALS, true, false);
    return this;
  }

  @Override
  public TaskQuery taskVariableValueNotEquals(String variableName, Object variableValue) {
    addVariable(variableName, variableValue, QueryOperator.NOT_EQUALS, true, false);
    return this;
  }

  @Override
  public TaskQuery taskVariableValueLike(String variableName, String variableValue) {
    addVariable(variableName, variableValue, QueryOperator.LIKE, true, false);
  	return this;
  }

  @Override
  public TaskQuery taskVariableValueGreaterThan(String variableName, Object variableValue) {
    addVariable(variableName, variableValue, QueryOperator.GREATER_THAN, true, false);
  	return this;
  }

  @Override
  public TaskQuery taskVariableValueGreaterThanOrEquals(String variableName, Object variableValue) {
    addVariable(variableName, variableValue, QueryOperator.GREATER_THAN_OR_EQUAL, true, false);
  	return this;
  }

  @Override
  public TaskQuery taskVariableValueLessThan(String variableName, Object variableValue) {
    addVariable(variableName, variableValue, QueryOperator.LESS_THAN, true, false);
  	return this;
  }

  @Override
  public TaskQuery taskVariableValueLessThanOrEquals(String variableName, Object variableValue) {
    addVariable(variableName, variableValue, QueryOperator.LESS_THAN_OR_EQUAL, true, false);
  	return this;
  }

  @Override
  public TaskQuery processVariableValueEquals(String variableName, Object variableValue) {
    addVariable(variableName, variableValue, QueryOperator.EQUALS, false, true);
    return this;
  }

  @Override
  public TaskQuery processVariableValueNotEquals(String variableName, Object variableValue) {
    addVariable(variableName, variableValue, QueryOperator.NOT_EQUALS, false, true);
    return this;
  }

  @Override
  public TaskQuery processVariableValueLike(String variableName, String variableValue) {
    addVariable(variableName, variableValue, QueryOperator.LIKE, false, true);
  	return this;
  }

  @Override
  public TaskQuery processVariableValueGreaterThan(String variableName, Object variableValue) {
    addVariable(variableName, variableValue, QueryOperator.GREATER_THAN, false, true);
  	return this;
  }

  @Override
  public TaskQuery processVariableValueGreaterThanOrEquals(String variableName, Object variableValue) {
    addVariable(variableName, variableValue, QueryOperator.GREATER_THAN_OR_EQUAL, false, true);
  	return this;
  }

  @Override
  public TaskQuery processVariableValueLessThan(String variableName, Object variableValue) {
    addVariable(variableName, variableValue, QueryOperator.LESS_THAN, false, true);
  	return this;
  }

  @Override
  public TaskQuery processVariableValueLessThanOrEquals(String variableName, Object variableValue) {
    addVariable(variableName, variableValue, QueryOperator.LESS_THAN_OR_EQUAL, false, true);
  	return this;
  }

  @Override
  public TaskQuery caseInstanceVariableValueEquals(String variableName, Object variableValue) {
    addVariable(variableName, variableValue, QueryOperator.EQUALS, false, false);
    return this;
  }

  @Override
  public TaskQuery caseInstanceVariableValueNotEquals(String variableName, Object variableValue) {
    addVariable(variableName, variableValue, QueryOperator.NOT_EQUALS, false, false);
    return this;
  }

  @Override
  public TaskQuery caseInstanceVariableValueLike(String variableName, String variableValue) {
    addVariable(variableName, variableValue, QueryOperator.LIKE, false, false);
    return this;
  }

  @Override
  public TaskQuery caseInstanceVariableValueGreaterThan(String variableName, Object variableValue) {
    addVariable(variableName, variableValue, QueryOperator.GREATER_THAN, false, false);
    return this;
  }

  @Override
  public TaskQuery caseInstanceVariableValueGreaterThanOrEquals(String variableName, Object variableValue) {
    addVariable(variableName, variableValue, QueryOperator.GREATER_THAN_OR_EQUAL, false, false);
    return this;
  }

  @Override
  public TaskQuery caseInstanceVariableValueLessThan(String variableName, Object variableValue) {
    addVariable(variableName, variableValue, QueryOperator.LESS_THAN, false, false);
    return this;
  }

  @Override
  public TaskQuery caseInstanceVariableValueLessThanOrEquals(String variableName, Object variableValue) {
    addVariable(variableName, variableValue, QueryOperator.LESS_THAN_OR_EQUAL, false, false);
    return this;
  }

  @Override
  public TaskQuery processDefinitionKey(String processDefinitionKey) {
    if (processDefinitionKeys == null) {
      processDefinitionKeys = new ArrayList<String>();
    }

    processDefinitionKeys.add(processDefinitionKey);

    return this;
  }

  @Override
  public TaskQuery processDefinitionKeyIn(String... processDefinitionKeysIn) {
    if (this.processDefinitionKeysIn == null) {
      this.processDefinitionKeysIn = new ArrayList<String>();
    }

    this.processDefinitionKeysIn.addAll(Arrays.asList(processDefinitionKeysIn));

    return this;
  }

  @Override
  public TaskQuery processDefinitionId(String processDefinitionId) {
    if (processDefinitionIds == null) {
      processDefinitionIds = new ArrayList<String>();
    }

    processDefinitionIds.add(processDefinitionId);

    return this;
  }

  @Override
  public TaskQuery processDefinitionName(String processDefinitionName) {
    if (processDefinitionNames == null) {
      processDefinitionNames = new ArrayList<String>();
    }

    processDefinitionNames.add(processDefinitionName);

    return this;
  }

  @Override
  public TaskQuery processDefinitionNameLike(String processDefinitionNameLike) {
    if (processDefinitionNamesLike == null) {
      processDefinitionNamesLike = new ArrayList<String>();
    }

    processDefinitionNamesLike.add(processDefinitionNameLike);

    return this;
  }

  @Override
  public TaskQuery dueDate(Date dueDate) {
    if (dueDates == null) {
      dueDates = new ArrayList<Date>();
    }

    dueDates.add(dueDate);

    expressions.remove("dueDate");
    return this;
  }

  @Override
  public TaskQuery dueDateExpression(String dueDateExpression) {
    expressions.put("dueDate", dueDateExpression);
    return this;
  }

  @Override
  public TaskQuery dueBefore(Date dueBefore) {
    this.dueBefore = dueBefore;
    expressions.remove("dueBefore");
    return this;
  }

  @Override
  public TaskQuery dueBeforeExpression(String dueDate) {
    expressions.put("dueBefore", dueDate);
    return this;
  }

  @Override
  public TaskQuery dueAfter(Date dueAfter) {
    this.dueAfter = dueAfter;
    expressions.remove("dueAfter");
    return this;
  }

  @Override
  public TaskQuery dueAfterExpression(String dueDateExpression) {
    expressions.put("dueAfter", dueDateExpression);
    return this;
  }

  @Override
  public TaskQuery followUpDate(Date followUpDate) {
    if (followUpDates == null) {
      followUpDates = new ArrayList<Date>();
    }

    followUpDates.add(followUpDate);

    expressions.remove("followUpDate");
    return this;
  }

  @Override
  public TaskQuery followUpDateExpression(String followUpDateExpression) {
    expressions.put("followUpDate", followUpDateExpression);
    return this;
  }

  @Override
  public TaskQuery followUpBefore(Date followUpBefore) {
    this.followUpBefore = followUpBefore;
    this.followUpNullAccepted = false;
    expressions.remove("followUpBefore");
    return this;
  }

  @Override
  public TaskQuery followUpBeforeExpression(String followUpBeforeExpression) {
    this.followUpNullAccepted = false;
    expressions.put("followUpBefore", followUpBeforeExpression);
    return this;
  }

  @Override
  public TaskQuery followUpBeforeOrNotExistent(Date followUpDate) {
    this.followUpBefore = followUpDate;
    this.followUpNullAccepted = true;
    expressions.remove("followUpBeforeOrNotExistent");
    return this;
  }

  @Override
  public TaskQuery followUpBeforeOrNotExistentExpression(String followUpDateExpression) {
    expressions.put("followUpBeforeOrNotExistent", followUpDateExpression);
    this.followUpNullAccepted = true;
    return this;
  }

  public void setFollowUpNullAccepted(boolean followUpNullAccepted) {
    this.followUpNullAccepted = followUpNullAccepted;
  }

  @Override
  public TaskQuery followUpAfter(Date followUpAfter) {
    this.followUpAfter = followUpAfter;
    expressions.remove("followUpAfter");
    return this;
  }

  @Override
  public TaskQuery followUpAfterExpression(String followUpAfterExpression) {
    expressions.put("followUpAfter", followUpAfterExpression);
    return this;
  }

  @Override
  public TaskQuery excludeSubtasks() {
    excludeSubtasks = true;
    return this;
  }

  @Override
  public TaskQuery active() {
    if (suspensionStates == null) {
      suspensionStates = new ArrayList<SuspensionState>();
    }

    suspensionStates.add(SuspensionState.ACTIVE);

    return this;
  }

  @Override
  public TaskQuery suspended() {
    if (suspensionStates == null) {
      suspensionStates = new ArrayList<SuspensionState>();
    }

    suspensionStates.add(SuspensionState.SUSPENDED);

    return this;
  }

  @Override
  public TaskQuery initializeFormKeys() {
    this.initializeFormKeys = true;
    return this;
  }

  public TaskQuery taskNameCaseInsensitive() {
    this.taskNameCaseInsensitive = true;
    return this;
  }

  @Override
  protected boolean hasExcludingConditions() {
    return super.hasExcludingConditions()/*
      || CompareUtil.areNotInAscendingOrder(minPriority, priority, maxPriority)
      || CompareUtil.areNotInAscendingOrder(dueAfter, dueDate, dueBefore)
      || CompareUtil.areNotInAscendingOrder(followUpAfter, followUpDate, followUpBefore)
      || CompareUtil.areNotInAscendingOrder(createTimeAfter, createTime, createTimeBefore)
      || CompareUtil.elementIsNotContainedInArray(key, taskDefinitionKeys)
      || CompareUtil.elementIsNotContainedInArray(processDefinitionKey, processDefinitionKeys)
      || CompareUtil.elementIsNotContainedInArray(processInstanceBusinessKey, processInstanceBusinessKeys)*/;
  }

  public List<String> getCandidateGroups() {
    if (candidateGroup!=null) {
      ArrayList result = new ArrayList();
      result.add(candidateGroup);
      return result;
    } else if (candidateUser != null) {
      return getGroupsForCandidateUser(candidateUser);
    } else if(candidateGroups != null) {
      return candidateGroups;
    }
    return null;
  }

  public Boolean isWithCandidateGroups() {
    if (withCandidateGroups == null) {
      return false;
    } else {
      return withCandidateGroups;
    }
  }

  public Boolean isWithCandidateUsers() {
    if (withCandidateUsers == null) {
      return false;
    } else {
      return withCandidateUsers;
    }
  }

  public Boolean isWithCandidateGroupsInternal() {
    return withCandidateGroups;
  }

  public Boolean isWithoutCandidateGroups() {
    if (withoutCandidateGroups == null) {
      return false;
    } else {
      return withoutCandidateGroups;
    }
  }

  public Boolean isWithoutCandidateUsers() {
    if (withoutCandidateUsers == null) {
      return false;
    } else {
      return withoutCandidateUsers;
    }
  }

  public Boolean isWithoutCandidateGroupsInternal() {
    return withoutCandidateGroups;
  }

  public List<String> getCandidateGroupsInternal() {
    return candidateGroups;
  }

  protected List<String> getGroupsForCandidateUser(String candidateUser) {
    List<Group> groups = Context
      .getCommandContext()
      .getReadOnlyIdentityProvider()
      .createGroupQuery()
      .groupMember(candidateUser)
      .list();
    List<String> groupIds = new ArrayList<String>();
    for (Group group : groups) {
      groupIds.add(group.getId());
    }
    return groupIds;
  }

  protected void ensureVariablesInitialized() {
    VariableSerializers types = Context.getProcessEngineConfiguration().getVariableSerializers();
    for(TaskQueryImpl query: logicalExpressionQueryRootNode.logicalExpressionQueryList) {
      for (QueryVariableValue var : query.variables) {
        var.initialize(types);
      }
    }
  }

  public void addVariable(String name, Object value, QueryOperator operator, boolean isTaskVariable, boolean isProcessInstanceVariable) {
    ensureNotNull("name", name);

    if(value == null || isBoolean(value)) {
      // Null-values and booleans can only be used in EQUALS and NOT_EQUALS
      switch(operator) {
      case GREATER_THAN:
        throw new ProcessEngineException("Booleans and null cannot be used in 'greater than' condition");
      case LESS_THAN:
        throw new ProcessEngineException("Booleans and null cannot be used in 'less than' condition");
      case GREATER_THAN_OR_EQUAL:
        throw new ProcessEngineException("Booleans and null cannot be used in 'greater than or equal' condition");
      case LESS_THAN_OR_EQUAL:
        throw new ProcessEngineException("Booleans and null cannot be used in 'less than or equal' condition");
      case LIKE:
        throw new ProcessEngineException("Booleans and null cannot be used in 'like' condition");
      default:
        break;
      }
    }
    addVariable(new TaskQueryVariableValue(name, value, operator, isTaskVariable, isProcessInstanceVariable));
  }

  protected void addVariable(TaskQueryVariableValue taskQueryVariableValue) {
    variables.add(taskQueryVariableValue);
  }

  private boolean isBoolean(Object value) {
  	if (value == null) {
  	  return false;
  	}
  	return Boolean.class.isAssignableFrom(value.getClass()) || boolean.class.isAssignableFrom(value.getClass());
	}

  //ordering ////////////////////////////////////////////////////////////////

  @Override
  public TaskQuery orderByTaskId() {
    if (this != logicalExpressionQueryRootNode) {
      throw new ProcessEngineException("Ordering is not allowed inside a nested query");
    }

    return orderBy(TaskQueryProperty.TASK_ID);
  }

  @Override
  public TaskQuery orderByTaskName() {
    if (this != logicalExpressionQueryRootNode) {
      throw new ProcessEngineException("Ordering is not allowed inside a nested query");
    }

    return orderBy(TaskQueryProperty.NAME);
  }

  @Override
  public TaskQuery orderByTaskNameCaseInsensitive() {
    if (this != logicalExpressionQueryRootNode) {
      throw new ProcessEngineException("Ordering is not allowed inside a nested query");
    }

    taskNameCaseInsensitive();
    return orderBy(TaskQueryProperty.NAME_CASE_INSENSITIVE);
  }

  @Override
  public TaskQuery orderByTaskDescription() {
    if (this != logicalExpressionQueryRootNode) {
      throw new ProcessEngineException("Ordering is not allowed inside a nested query");
    }

    return orderBy(TaskQueryProperty.DESCRIPTION);
  }

  @Override
  public TaskQuery orderByTaskPriority() {
    if (this != logicalExpressionQueryRootNode) {
      throw new ProcessEngineException("Ordering is not allowed inside a nested query");
    }

    return orderBy(TaskQueryProperty.PRIORITY);
  }

  @Override
  public TaskQuery orderByProcessInstanceId() {
    if (this != logicalExpressionQueryRootNode) {
      throw new ProcessEngineException("Ordering is not allowed inside a nested query");
    }

    return orderBy(TaskQueryProperty.PROCESS_INSTANCE_ID);
  }

  @Override
  public TaskQuery orderByCaseInstanceId() {
    if (this != logicalExpressionQueryRootNode) {
      throw new ProcessEngineException("Ordering is not allowed inside a nested query");
    }

    return orderBy(TaskQueryProperty.CASE_INSTANCE_ID);
  }

  @Override
  public TaskQuery orderByExecutionId() {
    if (this != logicalExpressionQueryRootNode) {
      throw new ProcessEngineException("Ordering is not allowed inside a nested query");
    }

    return orderBy(TaskQueryProperty.EXECUTION_ID);
  }

  @Override
  public TaskQuery orderByTenantId() {
    if (this != logicalExpressionQueryRootNode) {
      throw new ProcessEngineException("Ordering is not allowed inside a nested query");
    }

    return orderBy(TaskQueryProperty.TENANT_ID);
  }

  @Override
  public TaskQuery orderByCaseExecutionId() {
    if (this != logicalExpressionQueryRootNode) {
      throw new ProcessEngineException("Ordering is not allowed inside a nested query");
    }

    return orderBy(TaskQueryProperty.CASE_EXECUTION_ID);
  }

  @Override
  public TaskQuery orderByTaskAssignee() {
    if (this != logicalExpressionQueryRootNode) {
      throw new ProcessEngineException("Ordering is not allowed inside a nested query");
    }

    return orderBy(TaskQueryProperty.ASSIGNEE);
  }

  @Override
  public TaskQuery orderByTaskCreateTime() {
    if (this != logicalExpressionQueryRootNode) {
      throw new ProcessEngineException("Ordering is not allowed inside a nested query");
    }

    return orderBy(TaskQueryProperty.CREATE_TIME);
  }

  @Override
  public TaskQuery orderByDueDate() {
    if (this != logicalExpressionQueryRootNode) {
      throw new ProcessEngineException("Ordering is not allowed inside a nested query");
    }

    return orderBy(TaskQueryProperty.DUE_DATE);
  }

  @Override
  public TaskQuery orderByFollowUpDate() {
    if (this != logicalExpressionQueryRootNode) {
      throw new ProcessEngineException("Ordering is not allowed inside a nested query");
    }

    return orderBy(TaskQueryProperty.FOLLOW_UP_DATE);
  }

  @Override
  public TaskQuery orderByProcessVariable(String variableName, ValueType valueType) {
    if (this != logicalExpressionQueryRootNode) {
      throw new ProcessEngineException("Ordering is not allowed inside a nested query");
    }

    ensureNotNull("variableName", variableName);
    ensureNotNull("valueType", valueType);

    orderBy(VariableOrderProperty.forProcessInstanceVariable(variableName, valueType));
    return this;
  }

  @Override
  public TaskQuery orderByExecutionVariable(String variableName, ValueType valueType) {
    if (this != logicalExpressionQueryRootNode) {
      throw new ProcessEngineException("Ordering is not allowed inside a nested query");
    }

    ensureNotNull("variableName", variableName);
    ensureNotNull("valueType", valueType);

    orderBy(VariableOrderProperty.forExecutionVariable(variableName, valueType));
    return this;
  }

  @Override
  public TaskQuery orderByTaskVariable(String variableName, ValueType valueType) {
    if (this != logicalExpressionQueryRootNode) {
      throw new ProcessEngineException("Ordering is not allowed inside a nested query");
    }

    ensureNotNull("variableName", variableName);
    ensureNotNull("valueType", valueType);

    orderBy(VariableOrderProperty.forTaskVariable(variableName, valueType));
    return this;
  }

  @Override
  public TaskQuery orderByCaseExecutionVariable(String variableName, ValueType valueType) {
    if (this != logicalExpressionQueryRootNode) {
      throw new ProcessEngineException("Ordering is not allowed inside a nested query");
    }

    ensureNotNull("variableName", variableName);
    ensureNotNull("valueType", valueType);

    orderBy(VariableOrderProperty.forCaseExecutionVariable(variableName, valueType));
    return this;
  }

  @Override
  public TaskQuery orderByCaseInstanceVariable(String variableName, ValueType valueType) {
    if (this != logicalExpressionQueryRootNode) {
      throw new ProcessEngineException("Ordering is not allowed inside a nested query");
    }

    ensureNotNull("variableName", variableName);
    ensureNotNull("valueType", valueType);

    orderBy(VariableOrderProperty.forCaseInstanceVariable(variableName, valueType));
    return this;
  }

  //results ////////////////////////////////////////////////////////////////

  @Override
  public List<Task> executeList(CommandContext commandContext, Page page) {
    if (parentLogicalExpressionQuery != null) {
      throw new ProcessEngineException("'executeList()' is not allowed inside a nested query");
    }

    ensureVariablesInitialized();
    checkQueryOk();

    List<Task> taskList = null;

    try {
      taskList = commandContext
        .getTaskManager()
        .findTasksByQueryCriteria(logicalExpressionQueryRootNode.logicalExpressionQueryList.get(0));
    } catch(PersistenceException e) {
      if(e.getMessage().contains("Data conversion error converting \"()\"")) {
        throw new ProcessEngineException("At least one filter has to be defined within a nested query");
      } else {
        throw new ProcessEngineException(e.getMessage());
      }
    }

    boolean formKeyAvailability = false;
    for (TaskQueryImpl query: logicalExpressionQueryRootNode.logicalExpressionQueryList) {
      if (query.initializeFormKeys) {
        formKeyAvailability = true;
      }
    }

    if (formKeyAvailability) {
      for (Task task : taskList) {
        // initialize the form keys of the tasks
        ((TaskEntity) task).initializeFormKey();
      }
    }

    return taskList;
  }

  @Override
  public long executeCount(CommandContext commandContext) {
    if (parentLogicalExpressionQuery != null) {
      throw new ProcessEngineException("'executeList()' is not allowed inside a nested query");
    }

    ensureVariablesInitialized();
    checkQueryOk();

    try {
      return commandContext
        .getTaskManager()
        .findTaskCountByQueryCriteria(logicalExpressionQueryRootNode.logicalExpressionQueryList.get(0));
    } catch(PersistenceException e) {
      if(e.getMessage().contains("Data conversion error converting \"()\"")) {
        throw new ProcessEngineException("At least one filter has to be defined within a nested query");
      } else {
        throw new ProcessEngineException(e.getMessage());
      }
    }
  }

  //getters ////////////////////////////////////////////////////////////////

  public String getName() {
    return names == null ? null : names.get(names.size()-1);
  }

  public ArrayList<String> getNames() {
    return names;
  }

  public String getNameNotEqual() {
    return namesNotEqual == null ? null : namesNotEqual.get(namesNotEqual.size()-1);
  }

  public ArrayList<String> getNamesNotEqual() {
    return namesNotEqual;
  }

  public String getNameLike() {
    return namesLike == null ? null : namesLike.get(namesLike.size()-1);
  }

  public ArrayList<String> getNamesLike() {
    return namesLike;
  }

  public String getNameNotLike() {
    return namesNotLike == null ? null : namesNotLike.get(namesNotLike.size()-1);
  }

  public ArrayList<String> getNamesNotLike() {
    return namesNotLike;
  }

  public String getAssignee() {
    return assignees == null ? null : assignees.get(assignees.size()-1);
  }

  public ArrayList<String> getAssignees() {
    return assignees;
  }

  public String getAssigneeLike() {
    return assigneesLike == null ? null : assigneesLike.get(assigneesLike.size()-1);
  }

  public ArrayList<String> getAssigneesLike() {
    return assigneesLike;
  }

  public String getInvolvedUser() {
    return involvedUsers == null ? null : involvedUsers.get(involvedUsers.size()-1);
  }

  public ArrayList<String> getInvolvedUsers() {
    return involvedUsers;
  }

  public String getOwner() {
    return owners == null ? null : owners.get(owners.size()-1);
  }

  public ArrayList<String> getOwners() {
    return owners;
  }

  public Boolean isAssigned() {
    return assignedTasks != null;
  }

  public Boolean isAssignedInternal() {
    return assignedTasks == null ? null : true;
  }

  public boolean isUnassigned() {
    return unassignedTasks != null;
  }

  public Boolean isUnassignedInternal() {
    return unassignedTasks == null ? null : true;
  }

  public DelegationState getDelegationState() {
    return delegationStates == null || delegationStates.isEmpty() ? null : delegationStates.get(delegationStates.size()-1);
  }

  public ArrayList<DelegationState> getDelegationStates() {
    return delegationStates;
  }

  public boolean isNoDelegationState() {
    if (delegationStates != null) {
      for (DelegationState delegationState : delegationStates) {
        if (delegationState == null) {
          return true;
        }
      }
    }

    return false;
  }

  public String getDelegationStateString() {
    return delegationStates == null ? null : delegationStates.get(delegationStates.size()-1).toString();
  }

  public String getCandidateUser() {
    return candidateUser;
  }

  public String getCandidateGroup() {
    return candidateGroup;
  }

  public boolean isIncludeAssignedTasks() {
    return includeAssignedTasks != null ? includeAssignedTasks : false;
  }

  public Boolean isIncludeAssignedTasksInternal() {
    return includeAssignedTasks;
  }

  public String getProcessInstanceId() {
    return processInstanceIds == null ? null : processInstanceIds.get(processInstanceIds.size()-1);
  }

  public ArrayList<String> getProcessInstanceIds() {
    return processInstanceIds;
  }

  public String getExecutionId() {
    return executionIds == null ? null : executionIds.get(executionIds.size()-1);
  }

  public ArrayList<String> getExecutionIds() {
    return executionIds;
  }

  public String[] getActivityInstanceIdIn() {
    return activityInstanceIds == null ? null : activityInstanceIds.toArray(new String[0]);
  }

  public ArrayList<String> getActivityInstanceIdsIn() {
    return activityInstanceIds;
  }

  public String[] getTenantIds() {
    return tenantIds == null ? null : tenantIds.toArray(new String[0]);
  }

  public String getTaskId() {
    return taskIds == null ? null : taskIds.get(taskIds.size()-1);
  }

  public ArrayList<String> getTaskIds() {
    return taskIds;
  }

  public String getDescription() {
    return descriptions == null ? null : descriptions.get(descriptions.size()-1);
  }

  public ArrayList<String> getDescriptions() {
    return descriptions;
  }

  public String getDescriptionLike() {
    return descriptionsLike == null ? null : descriptionsLike.get(descriptionsLike.size()-1);
  }

  public ArrayList<String> getDescriptionsLike() {
    return descriptionsLike;
  }

  public Integer getPriority() {
    return priorities == null ? null : priorities.get(priorities.size()-1);
  }

  public ArrayList<Integer> getPriorities() {
    return priorities;
  }

  public Integer getMinPriority() {
    return minPriority;
  }

  public Integer getMaxPriority() {
    return maxPriority;
  }

  public Date getCreateTime() {
    return createTimes == null ? null : createTimes.get(createTimes.size()-1);
  }

  public ArrayList<Date> getCreateTimes() {
    return createTimes;
  }

  public Date getCreateTimeBefore() {
    return createTimeBefore;
  }

  public Date getCreateTimeAfter() {
    return createTimeAfter;
  }

  public String getKey() {
    return keys == null ? null : keys.get(keys.size()-1);
  }

  public String[] getKeys() {
    return taskDefinitionKeys == null ? null : taskDefinitionKeys.toArray(new String[0]);
  }

  public ArrayList<String> getKeysOr() {
    return keys;
  }

  public String getKeyLike() {
    return keysLike == null ? null : keysLike.get(keysLike.size()-1);
  }

  public ArrayList<String> getKeysLike() {
    return keysLike;
  }

  public String getParentTaskId() {
    return parentTaskIds == null ? null : parentTaskIds.get(parentTaskIds.size()-1);
  }

  public ArrayList<String> getParentTaskIds() {
    return parentTaskIds;
  }

  public List<TaskQueryVariableValue> getVariables() {
    return variables;
  }

  public String getProcessDefinitionKey() {
    return processDefinitionKeys == null ? null : processDefinitionKeys.get(processDefinitionKeys.size()-1);
  }

  public ArrayList<String> getProcessDefinitionKeysOr() {
    return processDefinitionKeys;
  }

  public String[] getProcessDefinitionKeys() {
    return processDefinitionKeysIn == null ? null : processDefinitionKeysIn.toArray(new String[0]);
  }

  public String getProcessDefinitionId() {
    return processDefinitionIds == null ? null : processDefinitionIds.get(processDefinitionIds.size()-1);
  }

  public ArrayList<String> getProcessDefinitionIds() {
    return processDefinitionIds;
  }

  public String getProcessDefinitionName() {
    return processDefinitionNames == null ? null : processDefinitionNames.get(processDefinitionNames.size()-1);
  }

  public ArrayList<String> getProcessDefinitionNames() {
    return processDefinitionNames;
  }

  public String getProcessDefinitionNameLike() {
    return processDefinitionNamesLike == null ? null : processDefinitionNamesLike.get(processDefinitionNamesLike.size()-1);
  }

  public ArrayList<String> getProcessDefinitionNamesLike() {
    return processDefinitionNamesLike;
  }

  public String getProcessInstanceBusinessKey() {
    return processInstanceBusinessKeys == null ? null : processInstanceBusinessKeys.get(processInstanceBusinessKeys.size()-1);
  }

  public ArrayList<String> getProcessInstanceBusinessKeysOr() {
    return processInstanceBusinessKeys;
  }

  public String[] getProcessInstanceBusinessKeys() {
    return processInstanceBusinessKeysIn == null ? null : processInstanceBusinessKeysIn.toArray(new String[0]);
  }

  public String getProcessInstanceBusinessKeyLike() {
    return processInstanceBusinessKeysLike == null ? null : processInstanceBusinessKeysLike.get(processInstanceBusinessKeysLike.size()-1);
  }

  public ArrayList<String> getProcessInstanceBusinessKeysLike() {
    return processInstanceBusinessKeysLike;
  }

  public Date getDueDate() {
    return dueDates == null ? null : dueDates.get(dueDates.size()-1);
  }

  public ArrayList<Date> getDueDates() {
    return dueDates;
  }

  public Date getDueBefore() {
    return dueBefore;
  }

  public Date getDueAfter() {
    return dueAfter;
  }

  public Date getFollowUpDate() {
    return followUpDates == null ? null : followUpDates.get(followUpDates.size()-1);
  }

  public ArrayList<Date> getFollowUpDates() {
    return followUpDates;
  }

  public Date getFollowUpBefore() {
    return followUpBefore;
  }

  public Date getFollowUpAfter() {
    return followUpAfter;
  }

  public boolean isExcludeSubtasks() {
    return excludeSubtasks;
  }

  public SuspensionState getSuspensionState() {
    return suspensionStates == null ? null : suspensionStates.get(suspensionStates.size()-1);
  }

  public ArrayList<SuspensionState> getSuspensionStates() {
    return suspensionStates;
  }

  public String getCaseInstanceId() {
    return caseInstanceIds == null ? null : caseInstanceIds.get(caseInstanceIds.size()-1);
  }

  public ArrayList<String> getCaseInstanceIds() {
    return caseInstanceIds;
  }

  public String getCaseInstanceBusinessKey() {
    return caseInstanceBusinessKeys == null ? null : caseInstanceBusinessKeys.get(caseInstanceBusinessKeys.size()-1);
  }

  public ArrayList<String> getCaseInstanceBusinessKeys() {
    return caseInstanceBusinessKeys;
  }

  public String getCaseInstanceBusinessKeyLike() {
    return caseInstanceBusinessKeysLike == null ? null : caseInstanceBusinessKeysLike.get(caseInstanceBusinessKeysLike.size()-1);
  }

  public ArrayList<String> getCaseInstanceBusinessKeysLike() {
    return caseInstanceBusinessKeysLike;
  }

  public String getCaseExecutionId() {
    return caseExecutionIds == null ? null : caseExecutionIds.get(caseExecutionIds.size()-1);
  }

  public ArrayList<String> getCaseExecutionIds() {
    return caseExecutionIds;
  }

  public String getCaseDefinitionId() {
    return caseDefinitionIds == null ? null : caseDefinitionIds.get(caseDefinitionIds.size()-1);
  }

  public ArrayList<String> getCaseDefinitionIds() {
    return caseDefinitionIds;
  }

  public String getCaseDefinitionKey() {
    return caseDefinitionKeys == null ? null : caseDefinitionKeys.get(caseDefinitionKeys.size()-1);
  }

  public ArrayList<String> getCaseDefinitionKeys() {
    return caseDefinitionKeys;
  }

  public String getCaseDefinitionName() {
    return caseDefinitionNames == null ? null : caseDefinitionNames.get(caseDefinitionNames.size()-1);
  }

  public ArrayList<String> getCaseDefinitionNames() {
    return caseDefinitionNames;
  }

  public String getCaseDefinitionNameLike() {
    return caseDefinitionNamesLike == null ? null : caseDefinitionNamesLike.get(caseDefinitionNamesLike.size()-1);
  }

  public ArrayList<String> getCaseDefinitionNamesLike() {
    return caseDefinitionNamesLike;
  }

  public boolean isInitializeFormKeys() {
    return initializeFormKeys;
  }

  public boolean isTaskNameCaseInsensitive() {
    return taskNameCaseInsensitive;
  }

  public boolean isTenantIdSet() {
    return isTenantIdSet;
  }

  public String getLogicalExpression() {
    return logicalExpression.toString();
  }

  public List<TaskQueryImpl> getLogicalExpressionQueryChildren() {
    return logicalExpressionQueryChildren;
  }

  @Override
  public TaskQuery extend(TaskQuery extending) {
    TaskQueryImpl extendingQuery = (TaskQueryImpl) extending;
    TaskQueryImpl extendedQuery = new TaskQueryImpl();

    // only add the base query's validators to the new query;
    // this is because the extending query's validators may not be applicable to the base
    // query and should therefore be executed before extending the query
    extendedQuery.validators = new HashSet<Validator<AbstractQuery<?, ?>>>(validators);

    if (extendingQuery.getName() != null) {
      extendedQuery.taskName(extendingQuery.getName());
    }
    else if (this.getName() != null) {
      extendedQuery.taskName(this.getName());
    }

    if (extendingQuery.getNameLike() != null) {
      extendedQuery.taskNameLike(extendingQuery.getNameLike());
    }
    else if (this.getNameLike() != null) {
      extendedQuery.taskNameLike(this.getNameLike());
    }

    if (extendingQuery.getNameNotEqual() != null) {
      extendedQuery.taskNameNotEqual(extendingQuery.getNameNotEqual());
    }
    else if (this.getNameNotEqual() != null) {
      extendedQuery.taskNameNotEqual(this.getNameNotEqual());
    }

    if (extendingQuery.getNameNotLike() != null) {
      extendedQuery.taskNameNotLike(extendingQuery.getNameNotLike());
    }
    else if (this.getNameNotLike() != null) {
      extendedQuery.taskNameNotLike(this.getNameNotLike());
    }

    if (extendingQuery.getAssignee() != null) {
      extendedQuery.taskAssignee(extendingQuery.getAssignee());
    }
    else if (this.getAssignee() != null) {
      extendedQuery.taskAssignee(this.getAssignee());
    }

    if (extendingQuery.getAssigneeLike() != null) {
      extendedQuery.taskAssigneeLike(extendingQuery.getAssigneeLike());
    }
    else if (this.getAssigneeLike() != null) {
      extendedQuery.taskAssigneeLike(this.getAssigneeLike());
    }

    if (extendingQuery.getInvolvedUser() != null) {
      extendedQuery.taskInvolvedUser(extendingQuery.getInvolvedUser());
    }
    else if (this.getInvolvedUser() != null) {
      extendedQuery.taskInvolvedUser(this.getInvolvedUser());
    }

    if (extendingQuery.getOwner() != null) {
      extendedQuery.taskOwner(extendingQuery.getOwner());
    }
    else if (this.getOwner() != null) {
      extendedQuery.taskOwner(this.getOwner());
    }

    if (extendingQuery.isAssigned() || this.isAssigned()) {
      extendedQuery.taskAssigned();
    }

    if (extendingQuery.isUnassigned() || this.isUnassigned()) {
      extendedQuery.taskUnassigned();
    }

    if (extendingQuery.getDelegationState() != null) {
      extendedQuery.taskDelegationState(extendingQuery.getDelegationState());
    }
    else if (this.getDelegationState() != null) {
      extendedQuery.taskDelegationState(this.getDelegationState());
    }

    if (extendingQuery.getCandidateUser() != null) {
      extendedQuery.taskCandidateUser(extendingQuery.getCandidateUser());
    }
    else if (this.getCandidateUser() != null) {
      extendedQuery.taskCandidateUser(this.getCandidateUser());
    }

    if (extendingQuery.getCandidateGroup() != null) {
      extendedQuery.taskCandidateGroup(extendingQuery.getCandidateGroup());
    }
    else if (this.getCandidateGroup() != null) {
      extendedQuery.taskCandidateGroup(this.getCandidateGroup());
    }

    if (extendingQuery.isWithCandidateGroups() || this.isWithCandidateGroups()) {
      extendedQuery.withCandidateGroups();
    }

    if (extendingQuery.isWithCandidateUsers() || this.isWithCandidateUsers()) {
      extendedQuery.withCandidateUsers();
    }

    if (extendingQuery.isWithoutCandidateGroups() || this.isWithoutCandidateGroups()) {
      extendedQuery.withoutCandidateGroups();
    }

    if (extendingQuery.isWithoutCandidateUsers() || this.isWithoutCandidateUsers()) {
      extendedQuery.withoutCandidateUsers();
    }

    if (extendingQuery.getCandidateGroupsInternal() != null) {
      extendedQuery.taskCandidateGroupIn(extendingQuery.getCandidateGroupsInternal());
    }
    else if (this.getCandidateGroupsInternal() != null) {
      extendedQuery.taskCandidateGroupIn(this.getCandidateGroupsInternal());
    }

    if (extendingQuery.getProcessInstanceId() != null) {
      extendedQuery.processInstanceId(extendingQuery.getProcessInstanceId());
    }
    else if (this.getProcessInstanceId() != null) {
      extendedQuery.processInstanceId(this.getProcessInstanceId());
    }

    if (extendingQuery.getExecutionId() != null) {
      extendedQuery.executionId(extendingQuery.getExecutionId());
    }
    else if (this.getExecutionId() != null) {
      extendedQuery.executionId(this.getExecutionId());
    }

    if (extendingQuery.getActivityInstanceIdIn() != null) {
      extendedQuery.activityInstanceIdIn(extendingQuery.getActivityInstanceIdIn());
    }
    else if (this.getActivityInstanceIdIn() != null) {
      extendedQuery.activityInstanceIdIn(this.getActivityInstanceIdIn());
    }

    if (extendingQuery.getTaskId() != null) {
      extendedQuery.taskId(extendingQuery.getTaskId());
    }
    else if (this.getTaskId() != null) {
      extendedQuery.taskId(this.getTaskId());
    }

    if (extendingQuery.getDescription() != null) {
      extendedQuery.taskDescription(extendingQuery.getDescription());
    }
    else if (this.getDescription() != null) {
      extendedQuery.taskDescription(this.getDescription());
    }

    if (extendingQuery.getDescriptionLike() != null) {
      extendedQuery.taskDescriptionLike(extendingQuery.getDescriptionLike());
    }
    else if (this.getDescriptionLike() != null) {
      extendedQuery.taskDescriptionLike(this.getDescriptionLike());
    }

    if (extendingQuery.getPriority() != null) {
      extendedQuery.taskPriority(extendingQuery.getPriority());
    }
    else if (this.getPriority() != null) {
      extendedQuery.taskPriority(this.getPriority());
    }

    if (extendingQuery.getMinPriority() != null) {
      extendedQuery.taskMinPriority(extendingQuery.getMinPriority());
    }
    else if (this.getMinPriority() != null) {
      extendedQuery.taskMinPriority(this.getMinPriority());
    }

    if (extendingQuery.getMaxPriority() != null) {
      extendedQuery.taskMaxPriority(extendingQuery.getMaxPriority());
    }
    else if (this.getMaxPriority() != null) {
      extendedQuery.taskMaxPriority(this.getMaxPriority());
    }

    if (extendingQuery.getCreateTime() != null) {
      extendedQuery.taskCreatedOn(extendingQuery.getCreateTime());
    }
    else if (this.getCreateTime() != null) {
      extendedQuery.taskCreatedOn(this.getCreateTime());
    }

    if (extendingQuery.getCreateTimeBefore() != null) {
      extendedQuery.taskCreatedBefore(extendingQuery.getCreateTimeBefore());
    }
    else if (this.getCreateTimeBefore() != null) {
      extendedQuery.taskCreatedBefore(this.getCreateTimeBefore());
    }

    if (extendingQuery.getCreateTimeAfter() != null) {
      extendedQuery.taskCreatedAfter(extendingQuery.getCreateTimeAfter());
    }
    else if (this.getCreateTimeAfter() != null) {
      extendedQuery.taskCreatedAfter(this.getCreateTimeAfter());
    }

    if (extendingQuery.getKey() != null) {
      extendedQuery.taskDefinitionKey(extendingQuery.getKey());
    }
    else if (this.getKey() != null) {
      extendedQuery.taskDefinitionKey(this.getKey());
    }

    if (extendingQuery.getKeyLike() != null) {
      extendedQuery.taskDefinitionKeyLike(extendingQuery.getKeyLike());
    }
    else if (this.getKeyLike() != null) {
      extendedQuery.taskDefinitionKeyLike(this.getKeyLike());
    }

    if (extendingQuery.getKeys() != null) {
      extendedQuery.taskDefinitionKeyIn(extendingQuery.getKeys());
    }
    else if (this.getKeys() != null) {
      extendedQuery.taskDefinitionKeyIn(this.getKeys());
    }

    if (extendingQuery.getParentTaskId() != null) {
      extendedQuery.taskParentTaskId(extendingQuery.getParentTaskId());
    }
    else if (this.getParentTaskId() != null) {
      extendedQuery.taskParentTaskId(this.getParentTaskId());
    }

    if (extendingQuery.getProcessDefinitionKey() != null) {
      extendedQuery.processDefinitionKey(extendingQuery.getProcessDefinitionKey());
    }
    else if (this.getProcessDefinitionKey() != null) {
      extendedQuery.processDefinitionKey(this.getProcessDefinitionKey());
    }

    if (extendingQuery.getProcessDefinitionKeys() != null) {
      extendedQuery.processDefinitionKeyIn(extendingQuery.getProcessDefinitionKeys());
    }
    else if (this.getProcessDefinitionKeys() != null) {
      extendedQuery.processDefinitionKeyIn(this.getProcessDefinitionKeys());
    }

    if (extendingQuery.getProcessDefinitionId() != null) {
      extendedQuery.processDefinitionId(extendingQuery.getProcessDefinitionId());
    }
    else if (this.getProcessDefinitionId() != null) {
      extendedQuery.processDefinitionId(this.getProcessDefinitionId());
    }

    if (extendingQuery.getProcessDefinitionName() != null) {
      extendedQuery.processDefinitionName(extendingQuery.getProcessDefinitionName());
    }
    else if (this.getProcessDefinitionName() != null) {
      extendedQuery.processDefinitionName(this.getProcessDefinitionName());
    }

    if (extendingQuery.getProcessDefinitionNameLike() != null) {
      extendedQuery.processDefinitionNameLike(extendingQuery.getProcessDefinitionNameLike());
    }
    else if (this.getProcessDefinitionNameLike() != null) {
      extendedQuery.processDefinitionNameLike(this.getProcessDefinitionNameLike());
    }

    if (extendingQuery.getProcessInstanceBusinessKey() != null) {
      extendedQuery.processInstanceBusinessKey(extendingQuery.getProcessInstanceBusinessKey());
    }
    else if (this.getProcessInstanceBusinessKey() != null) {
      extendedQuery.processInstanceBusinessKey(this.getProcessInstanceBusinessKey());
    }

    if (extendingQuery.getProcessInstanceBusinessKeyLike() != null) {
      extendedQuery.processInstanceBusinessKeyLike(extendingQuery.getProcessInstanceBusinessKeyLike());
    }
    else if (this.getProcessInstanceBusinessKeyLike() != null) {
      extendedQuery.processInstanceBusinessKeyLike(this.getProcessInstanceBusinessKeyLike());
    }

    if (extendingQuery.getDueDate() != null) {
      extendedQuery.dueDate(extendingQuery.getDueDate());
    }
    else if (this.getDueDate() != null) {
      extendedQuery.dueDate(this.getDueDate());
    }

    if (extendingQuery.getDueBefore() != null) {
      extendedQuery.dueBefore(extendingQuery.getDueBefore());
    }
    else if (this.getDueBefore() != null) {
      extendedQuery.dueBefore(this.getDueBefore());
    }

    if (extendingQuery.getDueAfter() != null) {
      extendedQuery.dueAfter(extendingQuery.getDueAfter());
    }
    else if (this.getDueAfter() != null) {
      extendedQuery.dueAfter(this.getDueAfter());
    }

    if (extendingQuery.getFollowUpDate() != null) {
      extendedQuery.followUpDate(extendingQuery.getFollowUpDate());
    }
    else if (this.getFollowUpDate() != null) {
      extendedQuery.followUpDate(this.getFollowUpDate());
    }

    if (extendingQuery.getFollowUpBefore() != null) {
      extendedQuery.followUpBefore(extendingQuery.getFollowUpBefore());
    }
    else if (this.getFollowUpBefore() != null) {
      extendedQuery.followUpBefore(this.getFollowUpBefore());
    }

    if (extendingQuery.getFollowUpAfter() != null) {
      extendedQuery.followUpAfter(extendingQuery.getFollowUpAfter());
    }
    else if (this.getFollowUpAfter() != null) {
      extendedQuery.followUpAfter(this.getFollowUpAfter());
    }

    if (extendingQuery.isFollowUpNullAccepted() || this.isFollowUpNullAccepted()) {
      extendedQuery.setFollowUpNullAccepted(true);
    }

    if (extendingQuery.isExcludeSubtasks() || this.isExcludeSubtasks()) {
      extendedQuery.excludeSubtasks();
    }

    if (extendingQuery.getSuspensionState() != null) {
      if (extendingQuery.getSuspensionState().equals(SuspensionState.ACTIVE)) {
        extendedQuery.active();
      }
      else if (extendingQuery.getSuspensionState().equals(SuspensionState.SUSPENDED)) {
        extendedQuery.suspended();
      }
    }
    else if (this.getSuspensionState() != null) {
      if (this.getSuspensionState().equals(SuspensionState.ACTIVE)) {
        extendedQuery.active();
      }
      else if (this.getSuspensionState().equals(SuspensionState.SUSPENDED)) {
        extendedQuery.suspended();
      }
    }

    if (extendingQuery.getCaseInstanceId() != null) {
      extendedQuery.caseInstanceId(extendingQuery.getCaseInstanceId());
    }
    else if (this.getCaseInstanceId() != null) {
      extendedQuery.caseInstanceId(this.getCaseInstanceId());
    }

    if (extendingQuery.getCaseInstanceBusinessKey() != null) {
      extendedQuery.caseInstanceBusinessKey(extendingQuery.getCaseInstanceBusinessKey());
    }
    else if (this.getCaseInstanceBusinessKey() != null) {
      extendedQuery.caseInstanceBusinessKey(this.getCaseInstanceBusinessKey());
    }

    if (extendingQuery.getCaseInstanceBusinessKeyLike() != null) {
      extendedQuery.caseInstanceBusinessKeyLike(extendingQuery.getCaseInstanceBusinessKeyLike());
    }
    else if (this.getCaseInstanceBusinessKeyLike() != null) {
      extendedQuery.caseInstanceBusinessKeyLike(this.getCaseInstanceBusinessKeyLike());
    }

    if (extendingQuery.getCaseExecutionId() != null) {
      extendedQuery.caseExecutionId(extendingQuery.getCaseExecutionId());
    }
    else if (this.getCaseExecutionId() != null) {
      extendedQuery.caseExecutionId(this.getCaseExecutionId());
    }

    if (extendingQuery.getCaseDefinitionId() != null) {
      extendedQuery.caseDefinitionId(extendingQuery.getCaseDefinitionId());
    }
    else if (this.getCaseDefinitionId() != null) {
      extendedQuery.caseDefinitionId(this.getCaseDefinitionId());
    }

    if (extendingQuery.getCaseDefinitionKey() != null) {
      extendedQuery.caseDefinitionKey(extendingQuery.getCaseDefinitionKey());
    }
    else if (this.getCaseDefinitionKey() != null) {
      extendedQuery.caseDefinitionKey(this.getCaseDefinitionKey());
    }

    if (extendingQuery.getCaseDefinitionName() != null) {
      extendedQuery.caseDefinitionName(extendingQuery.getCaseDefinitionName());
    }
    else if (this.getCaseDefinitionName() != null) {
      extendedQuery.caseDefinitionName(this.getCaseDefinitionName());
    }

    if (extendingQuery.getCaseDefinitionNameLike() != null) {
      extendedQuery.caseDefinitionNameLike(extendingQuery.getCaseDefinitionNameLike());
    }
    else if (this.getCaseDefinitionNameLike() != null) {
      extendedQuery.caseDefinitionNameLike(this.getCaseDefinitionNameLike());
    }

    if (extendingQuery.isInitializeFormKeys() || this.isInitializeFormKeys()) {
      extendedQuery.initializeFormKeys();
    }

    if (extendingQuery.isTaskNameCaseInsensitive() || this.isTaskNameCaseInsensitive()) {
      extendedQuery.taskNameCaseInsensitive();
    }

    if (extendingQuery.isTenantIdSet()) {
      if (extendingQuery.getTenantIds() != null) {
        extendedQuery.tenantIdIn(extendingQuery.getTenantIds());
      } else {
        extendedQuery.withoutTenantId();
      }
    } else if (this.isTenantIdSet()) {
      if (this.getTenantIds() != null) {
        extendedQuery.tenantIdIn(this.getTenantIds());
      } else {
        extendedQuery.withoutTenantId();
      }
    }

    // merge variables
    mergeVariables(extendedQuery, extendingQuery);

    // merge expressions
    mergeExpressions(extendedQuery, extendingQuery);

    // include taskAssigned tasks has to be set after expression as it asserts on already set
    // candidate properties which could be expressions
    if (extendingQuery.isIncludeAssignedTasks() || this.isIncludeAssignedTasks()) {
      extendedQuery.includeAssignedTasks();
    }

    mergeOrdering(extendedQuery, extendingQuery);

    return extendedQuery;
  }

  /**
   * Simple implementation of variable merging. Variables are only overridden if they have the same name and are
   * in the same scope (ie are process instance, task or case execution variables).
   */
  protected void mergeVariables(TaskQueryImpl extendedQuery, TaskQueryImpl extendingQuery) {
    List<TaskQueryVariableValue> extendingVariables = extendingQuery.getVariables();

    Set<TaskQueryVariableValueComparable> extendingVariablesComparable = new HashSet<TaskQueryVariableValueComparable>();

    // set extending variables and save names for comparison of original variables
    for (TaskQueryVariableValue extendingVariable : extendingVariables) {
      extendedQuery.addVariable(extendingVariable);
      extendingVariablesComparable.add(new TaskQueryVariableValueComparable(extendingVariable));
    }

    for (TaskQueryVariableValue originalVariable : this.getVariables()) {
      if (!extendingVariablesComparable.contains(new TaskQueryVariableValueComparable(originalVariable))) {
        extendedQuery.addVariable(originalVariable);
      }
    }

  }

  protected class TaskQueryVariableValueComparable {

    protected TaskQueryVariableValue variableValue;

    public TaskQueryVariableValueComparable(TaskQueryVariableValue variableValue) {
      this.variableValue = variableValue;
    }

    public TaskQueryVariableValue getVariableValue() {
      return variableValue;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      TaskQueryVariableValue other = ((TaskQueryVariableValueComparable) o).getVariableValue();

      return variableValue.getName().equals(other.getName())
             && variableValue.isProcessInstanceVariable() == other.isProcessInstanceVariable()
             && variableValue.isLocal() == other.isLocal();
    }

    @Override
    public int hashCode() {
      int result = variableValue.getName() != null ? variableValue.getName().hashCode() : 0;
      result = 31 * result + (variableValue.isProcessInstanceVariable() ? 1 : 0);
      result = 31 * result + (variableValue.isLocal() ? 1 : 0);
      return result;
    }

  }

  public boolean isFollowUpNullAccepted() {
    return followUpNullAccepted;
  }

  @Override
  public TaskQuery taskNameNotEqual(String name) {
    if (namesNotEqual == null) {
      namesNotEqual = new ArrayList<String>();
    }

    namesNotEqual.add(name);

    return this;
  }

  @Override
  public TaskQuery taskNameNotLike(String nameNotLike) {
    ensureNotNull("Task nameNotLike", nameNotLike);
    if (namesNotLike == null) {
      namesNotLike = new ArrayList<String>();
    }

    namesNotLike.add(nameNotLike);

    return this;
  }

  @Override
  public TaskQuery startAnd() {
    return openLogicalOperator(logicalExpressionEnum.and);
  }

  @Override
  public TaskQuery startOr() {
    return openLogicalOperator(logicalExpressionEnum.or);
  }

  @Override
  public TaskQuery endAnd() {
    if (parentLogicalExpressionQuery == null || logicalExpression == logicalExpressionEnum.or) {
      throw new ProcessEngineException("'endAnd()' is only allowed after invocation of 'startAnd()'");
    }

    return parentLogicalExpressionQuery;
  }

  @Override
  public TaskQuery endOr() {
    if (parentLogicalExpressionQuery == null || logicalExpression == logicalExpressionEnum.and) {
      //throw new ProcessEngineException("'endOr()' is only allowed after invocation of 'startOr()'");
    }

    return parentLogicalExpressionQuery;
  }

  private TaskQuery openLogicalOperator(logicalExpressionEnum logicalExpression) {
    TaskQueryImpl queryInstance = new TaskQueryImpl(logicalExpression, this);
    logicalExpressionQueryChildren.add(queryInstance);

    return queryInstance;
  }

}
