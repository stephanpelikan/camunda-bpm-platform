package org.camunda.bpm.engine.impl.db.sql;

import org.camunda.bpm.engine.impl.TaskQueryImpl;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by tasso on 31.05.17.
 */
public class MyBatisTaskQueryHelper {
  private static List<TaskQueryImpl> processedQueries;
  private static int closingParenthesesCounter;
  private static boolean expressionClosed;
  private static List<Integer> closingParenthesesCounterList;

  public static List<String> closingParenthesesList(List<TaskQueryImpl> logicalExpressionQueryList) {
    processedQueries = new ArrayList<TaskQueryImpl>();
    closingParenthesesCounterList = new ArrayList<Integer>();
    closingParenthesesCounter = 0;
    expressionClosed = true;
    
    buildClosingParenthesesAmountList(logicalExpressionQueryList);

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

  private static void buildClosingParenthesesAmountList(List<TaskQueryImpl> queryList) {
    for (TaskQueryImpl query : queryList) {
      if(!expressionClosed) {
        closingParenthesesCounterList.remove(closingParenthesesCounterList.size()-1);
        closingParenthesesCounterList.add(closingParenthesesCounter);
        closingParenthesesCounter = 0;
        expressionClosed = true;
      }

      if (!processedQueries.contains(query)) {
        closingParenthesesCounterList.add(0);
        if (!query.getLogicalExpressionQueryChildren().isEmpty()) {
          buildClosingParenthesesAmountList(query.getLogicalExpressionQueryChildren());
        } else {
          expressionClosed = false;
        }

        closingParenthesesCounter++;
        processedQueries.add(query);
      }
    }
  }

}
