import com.atlassian.jira.event.issue.IssueEvent
import com.atlassian.jira.issue.Issue
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


def main(Issue issue, Helper hp){

    //Beginn customizing
        def projectKey = "DEMO"
        def issueType = "Story"
    //End customizing

    def myIssue
    def myIssueSummary
    def myIssueDescription
    def myIssueReporter


    myIssueSummary = "Summary of my Issue"
    myIssueDescription = "Description of my Issue"
    myIssueReporter = "harry.cain72@gmail.com"

    myIssue = hp.createIssue(projectKey,issueType,myIssueSummary,myIssueDescription,myIssueReporter)
}

//---------

hp = new Helper()
main(getCurrentIssue("WF"),hp)



