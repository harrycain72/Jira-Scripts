import com.atlassian.crowd.embedded.api.User
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.issue.IssueEvent
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.user.util.UserUtil
import com.opensymphony.workflow.WorkflowContext
import util.Helper

/**
 * Created by roland on 17.02.16.
 */




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

def User getCurrentUserWF() {

    String currentUser = ((WorkflowContext) transientVars.get("context")).getCaller()

    //Security
    UserUtil userUtil = ComponentAccessor.getUserUtil()

    User user = userUtil.getUser(currentUser)


}

def main(Issue issue, Helper hp){

    //Beginn customizing
        def projectKey = "DEMO"
        def issueType = "Story"
    //End customizing

    def myIssue
    def myIssueSummary
    def myIssueDescription
    def myIssueReporter
    def currentUser


    myIssueSummary = "Summary of my Issue"
    myIssueDescription = "Description of my Issue"
    myIssueReporter = "harry.cain72@gmail.com"
    currentUser = getCurrentUserWF()


    myIssue = hp.createIssue(projectKey,issueType,myIssueSummary,myIssueDescription,myIssueReporter,currentUser)

    println ""
}

//---------

hp = new Helper()
main(getCurrentIssue("WF"),hp)



