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
package org.camunda.bpm.engine.impl.pvm.runtime.operation;

import static org.camunda.bpm.engine.impl.util.ActivityBehaviorUtil.getActivityBehavior;

import java.util.concurrent.Callable;

import org.camunda.bpm.engine.impl.bpmn.behavior.ActivityInstanceAssumption;
import org.camunda.bpm.engine.impl.pvm.PvmException;
import org.camunda.bpm.engine.impl.pvm.PvmLogger;
import org.camunda.bpm.engine.impl.pvm.delegate.SignallableActivityBehavior;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.impl.pvm.runtime.PvmExecutionImpl;

/**
 * @author Tom Baeyens
 */
public class PvmAtomicOperationActivitySignal implements PvmAtomicOperation {

  private final static PvmLogger LOG = PvmLogger.PVM_LOGGER;

  public boolean isAsync(PvmExecutionImpl execution) {
    return false;
  }

  public void execute(final PvmExecutionImpl execution) {
    final SignallableActivityBehavior activityBehavior = (SignallableActivityBehavior) getActivityBehavior(execution);

    // TODO: instanceofs

    ActivityInstanceAssumption.doWithAssumption(execution,
        new Callable<Void>() {

          @Override
          public Void call() throws Exception {
            ActivityImpl activity = execution.getActivity();
//          LOG.debugExecutesActivity(execution, activity, activityBehavior.getClass().getName());

            try {
              activityBehavior.signal(execution, null, null);
            } catch (RuntimeException e) {
              throw e;
            } catch (Exception e) {
              throw new PvmException("couldn't signal activity <"+activity.getProperty("type")+" id=\""+activity.getId()+"\" ...>: "+e.getMessage(), e);
            }
            return null;
          }

      }
    );


  }

  public String getCanonicalName() {
    return "activity-signal";
  }

  public boolean isAsyncCapable() {
    return false;
  }
}
