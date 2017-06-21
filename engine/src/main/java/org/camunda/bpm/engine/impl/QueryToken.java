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

import org.camunda.bpm.engine.task.TaskQuery;

/**
 * @author smirnov
 *
 */
public class QueryToken {

  protected boolean openingBracket;
  protected boolean closingBracket;

  protected boolean isOr;
  protected boolean isAnd;

  protected TaskQuery taskQuery;

  public QueryToken(TaskQuery taskQuery) {
    this.taskQuery = taskQuery;
  }

  public QueryToken() {
  }

  public TaskQuery getTaskQuery() {
    return taskQuery;
  }

  /**
   * @return the openingBracket
   */
  public boolean isOpeningBracket() {
    return openingBracket;
  }

  /**
   * @param openingBracket the openingBracket to set
   */
  public void setOpeningBracket(boolean openingBracket) {
    this.openingBracket = openingBracket;
  }

  /**
   * @return the closingBracket
   */
  public boolean isClosingBracket() {
    return closingBracket;
  }

  /**
   * @param closingBracket the closingBracket to set
   */
  public void setClosingBracket(boolean closingBracket) {
    this.closingBracket = closingBracket;
  }

  /**
   * @return the isOr
   */
  public boolean isOr() {
    return isOr;
  }

  /**
   * @param isOr the isOr to set
   */
  public void setOr(boolean isOr) {
    this.isOr = isOr;
  }

  /**
   * @return the isAnd
   */
  public boolean isAnd() {
    return isAnd;
  }

  /**
   * @param isAnd the isAnd to set
   */
  public void setAnd(boolean isAnd) {
    this.isAnd = isAnd;
  }

}
