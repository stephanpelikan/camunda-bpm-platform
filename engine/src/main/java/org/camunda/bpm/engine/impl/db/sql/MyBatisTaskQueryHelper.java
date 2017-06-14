package org.camunda.bpm.engine.impl.db.sql;

import org.camunda.bpm.engine.impl.TaskQueryImpl;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by tasso on 31.05.17.
 */
public class MyBatisTaskQueryHelper {
  public static List<String> closingParenthesesList(List<TaskQueryImpl> logicalExpressionQueryList) {
    ArrayList<TaskQueryImpl> processedQueries = new ArrayList<TaskQueryImpl>();
    ArrayList<Integer> closingParenthesesCounterList = new ArrayList<Integer>();
    int[] closingParenthesesCounter = {0};
    boolean[] isExpressionClosed = {true};
    
    buildClosingParenthesesAmountList(logicalExpressionQueryList, processedQueries, closingParenthesesCounterList,
      closingParenthesesCounter, isExpressionClosed);

    List<String> closingParentheses = new LinkedList<String>();

    for (Integer closingParenthesis: closingParenthesesCounterList) {
      if (closingParenthesis == 0) {
        closingParentheses.add(null);
      } else {
        StringBuilder closingParenthesisString = new StringBuilder();
        for (int i = 0; i < closingParenthesis; i++) {
          closingParenthesisString.append(")");
        }

        closingParentheses.add(closingParenthesisString.toString());
      }
    }

    return closingParentheses;
  }

  private static void buildClosingParenthesesAmountList(List<TaskQueryImpl> queryList, List<TaskQueryImpl> processedQueries,
    List<Integer> closingParenthesesCounterList, int[] closingParenthesesCounter, boolean[] isExpressionClosed) {
    for (TaskQueryImpl query : queryList) {
      if(!isExpressionClosed[0]) {
        closingParenthesesCounterList.remove(closingParenthesesCounterList.size()-1);
        closingParenthesesCounterList.add(closingParenthesesCounter[0]);
        closingParenthesesCounter[0] = 0;
        isExpressionClosed[0] = true;
      }

      if (!processedQueries.contains(query)) {
        closingParenthesesCounterList.add(0);
        if (!query.getLogicalExpressionQueryChildren().isEmpty()) {
          buildClosingParenthesesAmountList(query.getLogicalExpressionQueryChildren(), processedQueries,
            closingParenthesesCounterList, closingParenthesesCounter, isExpressionClosed);
        } else {
          isExpressionClosed[0] = false;
        }

        closingParenthesesCounter[0]++;
        processedQueries.add(query);
      }
    }
  }

}
