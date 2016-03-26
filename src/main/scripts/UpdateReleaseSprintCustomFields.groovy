import com.atlassian.jira.event.issue.IssueEvent
import com.atlassian.jira.issue.DocumentIssueImpl
import com.atlassian.jira.issue.Issue
import util.Helper
import com.atlassian.jira.project.Project

/**
 * Created by roland on 25.03.16.
 */

//retrieves the current issue i.e. for a listener
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



def main(Issue issue, Helper hp) {

    //begin customizing

    def customFieldNameRelease = ".Release"
    def customFieldNameSprint = ".Sprint"
    def customFieldNameSprintAndReleaseNames = ".Sprints"
    def customFieldNameDeveloper = ".Developer"
    def customFieldNameAlmSubject = ".Alm_Subject"
    def customFieldNameStoryID = ".Story-ID"
    def customFieldNameITApp_Module = ".IT-App_Module"
    def customFieldNameRequirementID = ".Requirement-ID"
    def customfieldNameTestCaseOrigin = ".TestCaseOrigin"
    def customfieldNameAlmSubjectHP = ".ALM_Subject_HP"
    def customfieldNameTestStatus = ".TestStatus"
    def nameOfPrefix = "DEV"
    def issueTypeNameSubTasks = "Sub-task"
    def issueTypeNameStory = "Story"
    def issueTypeNameRequirement = "Requirement"
    def issueTypeNameTestCase = "Test Case"
    def issueTypeNameBug = "Bug"
    def issueTypeNameBusinessRequest = "Business Request"
    def constantJIRA = "JIRA"
    def constantHPALM = "HP-ALM"

    //end customizing
    def issueType = issue.getIssueTypeObject().getName()

    // These names should be standard in JIRA and not change from release to release
    def listOfFieldNames = [".refresh",".Release",".Sprint"]
    def searchResult
    def field

    //only true if we have an update
    if(issue.summary == "REFRESH") {

        if (issue.created != issue.updated) {

            //just relevant for testing purposes in order to check the name of JIRA-fields
            def test = event.getChangeLog().getRelated('ChildChangeItem')

            // we loop over all field names, for which we want to check if an update has happened.
            for (item in listOfFieldNames) {

                def check = event.getChangeLog().getRelated('ChildChangeItem').find { it.field == item }

                //is null if not update was found for the field
                if (check != null) {

                    searchResult = check
                    field = searchResult.field

                    break

                }

            }


            if (searchResult != null && field == ".refresh") {


                if (issue.getIssueTypeObject().getName() == issueTypeNameStory) {

                    def refreshFlag = hp.getCustomFieldValue(issue, ".refresh")

                    def stories = []

                    def query = "project = demo and issuetype = Story and summary !~ \"REFRESH\""
                    stories = hp.getIssuesByQuery(query).getIssues()


                    for (Issue anIssue : stories) {

                        if (anIssue instanceof DocumentIssueImpl) {
                            myIssue = hp.getIssueByKey(anIssue.getKey())
                        }

                        hp.deleteLabelCustomField(anIssue, customFieldNameAlmSubject)
                        hp.deleteLabelCustomField(anIssue, customFieldNameRelease)
                        hp.deleteLabelCustomField(anIssue, customFieldNameSprint)

                        //retrieve sprint and fixversion and copy the values to the customfields .Release and .Alm_Subject and .Sprint
                        hp.setReleaseSprint(anIssue, hp)

                        //if the release is changed but the sprint remains - which should not really be the case
                        //then we must make sure, that this change is also available for the relevant business requests

                        hp.setReleaseAndSprintNamesInBusinessRequest(anIssue, customFieldNameSprintAndReleaseNames)

                        hp.setReleaseAndSprintNamesInPKE(anIssue, customFieldNameSprintAndReleaseNames)

                    }


                }

            }

        }

        def option = hp.setCustomFieldValueOption(issue,".refresh","None")

    }
}


hp = new Helper()


main(getCurrentIssue("EV"),hp)
