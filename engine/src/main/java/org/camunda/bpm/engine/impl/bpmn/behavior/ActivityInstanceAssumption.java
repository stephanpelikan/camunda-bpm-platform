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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Callable;

import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.pvm.runtime.PvmExecutionImpl;

/**
 * @author Thorben Lindhauer
 *
 */
public class ActivityInstanceAssumption {

  protected String activityInstanceId;

  protected static ThreadLocal<Deque<ActivityInstanceAssumption>> assumptions = new ThreadLocal<Deque<ActivityInstanceAssumption>>();

  public ActivityInstanceAssumption(PvmExecutionImpl execution) {
    this.activityInstanceId = execution.getActivityInstanceId();
  }

  protected static <T> Deque<T> getDeque(ThreadLocal<Deque<T>> threadLocal) {
    Deque<T> stack = threadLocal.get();
    if (stack==null) {
      stack = new ArrayDeque<T>();
      threadLocal.set(stack);
    }
    return stack;
  }

  public boolean assume(PvmExecutionImpl execution)
  {
    return execution.getActivityId() != null && activityInstanceId.equals(execution.getActivityInstanceId());
  }

  public static ActivityInstanceAssumption create(PvmExecutionImpl execution)
  {
    return new ActivityInstanceAssumption(execution);
  }

  public static ActivityInstanceAssumption getCurrentAssumption()
  {
    return getDeque(assumptions).peekLast();
  }

  public static boolean doWithAssumption(PvmExecutionImpl execution, Callable<Void> logic)
  {
    ActivityInstanceAssumption assumption = create(execution);

    Deque<ActivityInstanceAssumption> deque = getDeque(assumptions);
    deque.add(assumption);
    try {
      try {
        logic.call();
      } catch (Exception e) {
        throw new ProcessEngineException(e);
      }
    } finally {
      deque.removeLast();
    }

    return assumption.assume(execution);
  }
}
