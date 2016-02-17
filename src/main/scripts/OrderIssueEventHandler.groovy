/**
 * Created by roland on 14.02.16.
 */


import util.Helper
import com.atlassian.jira.issue.Issue


def handleOrderIssueWorkflowEvent(Issue issue){

    println issue.getKey()
}


//

def hp = new Helper()

hp.getCurrentIssue("EV")


