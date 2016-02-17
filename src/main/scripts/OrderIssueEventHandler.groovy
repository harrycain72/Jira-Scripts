/**
 * Created by roland on 14.02.16.
 */
import com.atlassian.jira.event.issue.IssueEvent
import util.Helper
import com.atlassian.jira.issue.Issue


def handleOrderIssueWorkflowEvent(Issue issue, Helper hp){

    hp.addComment(issue,issue.getKey())

}


//retrieves the current issue i.e. for a listener
def Issue getCurrentIssue(String flag){

    def myIssue

    if(flag == "WF"){
        myIssue =(Issue)issue
    }

    if(flag == "EV"){
        def event = event as IssueEvent
        myIssue = event.getIssue()
    }

    return myIssue
}

//


hp = new Helper()


handleOrderIssueWorkflowEvent(getCurrentIssue("EV"),hp)



