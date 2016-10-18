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
package org.camunda.bpm.engine.impl.bpmn.behavior;

import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;



/**
 * Parent class for all BPMN 2.0 task types such as ServiceTask, ScriptTask, UserTask, etc.
 *
 * When used on its own, it behaves just as a pass-through activity.
 *
 * @author Joram Barrez
 */
public class TaskActivityBehavior extends AbstractBpmnActivityBehavior {

  /**
   * Activity instance id before execution.
   */
  protected String activityInstanceId;

  /**
   * The method which should be overridden by the sub classes to perform an execution.
   *
   * @param execution the execution which is used during performing the execution
   * @throws Exception
   */
  protected void performExecution(ActivityExecution execution) throws Exception {
    leave(execution);
  }

  @Override
  public void execute(ActivityExecution execution) throws Exception {
    performExecution(execution);
  }

  /**
   * Method which should be called in the performExecution method. Since in
   * this execution method we have to check if we can leave the activity.
   * In signal method we have to call the normal leave method, since we
   * are triggered from outside.
   *
   * @param execution the execution which should be left
   */
  protected void tryToLeave(ActivityExecution execution) {
//    //if execution is not active after delegated execution the tree was expand
//    //we have to check out replacedExecution (in case of non interrupting events)
//    if (!execution.isActive()) {
//      ExecutionEntity replacedExecution = ((ExecutionEntity) execution).getReplacedBy();
//      if (replacedExecution != null) {
//        execution = replacedExecution;
//      }
//    }
//
//    //in both cases (interrupting and non interrupting events)
//    //we have to check if activity instance was changed -> if yes, leave is not ok
//    //leave was already triggered (for example for conditional events)
//    if (activityInstanceId != null && activityInstanceId.equals(execution.getActivityInstanceId())) {
//      super.leave(execution);
//    }

    leave(execution);
  }



}
