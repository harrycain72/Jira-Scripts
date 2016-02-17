package util



import com.atlassian.jira.event.issue.IssueEvent
import com.atlassian.jira.issue.Issue

/**
 * Created by roland on 14.02.16.
 * This class contains methods which are used for JIRA scripts
 */

class UtilityMngr {

    /** This method delivers the correct issue.
     * It is either a issue retrieved from a workflow or from an event.
     */

    def getCurrentIssue(String flag){

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
}