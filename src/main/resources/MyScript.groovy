import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.fields.CustomField
import com.opensymphony.workflow.WorkflowContext

//this method retrieves the current user
def getCurrentUser() {
    //determine current user
    String currentUser = ((WorkflowContext) transientVars.get("context")).getCaller()

    return currentUser
}

//this method creates a comment
def addComment(String myComment) {

    cmm = ComponentAccessor.getCommentManager()

    //determine current user
    String currentUser = getCurrentUser()

    //CommentMngr
    cmm.create(issue,currentUser,myComment,true)
    issue.store()
}

//this method gets the value of a customfield by its name
def getCustomFieldValue(String myCustomField) {

    cfm = ComponentAccessor.getCustomFieldManager()

    CustomField customField = cfm.getCustomFieldObjectByName(myCustomField);
    return  (String)customField.getValue(issue);

}

//this method gets a list of subtasks of an issue, retrieves their summary and checks if a defined one exists in this list.
def checkIfSubTaskSummaryExists(String mySummaryToBeChecked) {

    //we create a list of all subtasks for the active issue
    def subTasks = issue.getSubTaskObjects()


    //we create a list of all summaries of all found subtasks
    def subTasksSummaries = []

    subTasks.each {

        subTasksSummaries.add(it.getSummary())
    }

    //we check if in the list of summaries  o
    def checkResult  = subTasksSummaries.contains(mySummaryToBeChecked)

    return checkResult
}


//this method is responsible for the creation of subTask
def addSubTask(String subTaskName, String subTaskDescription) {

    //Instanzierung der factories
    isf = ComponentAccessor.getIssueFactory()

    //IssueFactory: we create her a generic issue
    def issueObject = isf.getIssue()

    issueObject.setProject(issue.getProject())

    //Possible IssueTypeValues are 10001 story, 10101 subtask, 10102 bug, 10000 epic
    // old value 5 ?
    issueObject.setIssueTypeId('10101')

    //getValues of current issue = parent
    issueObject.setParentId(issue.getId())
    issueObject.setSummary(subTaskName + ': user story ' + issue.getSummary())
    issueObject.setAssignee(issue.getAssignee())
    issueObject.setDescription(subTaskDescription)
    issueObject.setReporter(issue.getReporter())



    //here we check if the value for the summary of a subtasks has already been used. We do not want to have
    //two subtasks with the same value.
    def toBeCreatedSubTaskSummary = subTaskName + ': user story ' + issue.getSummary()
    checkResult = checkIfSubTaskSummaryExists(toBeCreatedSubTaskSummary)

    // we only create our new SubTask if the the value of summary does not exist in any already defined subtask
    if (!checkResult) {

        //the issue gets created with the IssueMngr
        ism = ComponentAccessor.getIssueManager()

        //Security
        jac = ComponentAccessor.getJiraAuthenticationContext()

        subTask = ism.createIssueObject(jac.getUser(), issueObject)


        //the created subtask is linked to the issue.This is done through the SubTaskMngr

        stm = ComponentAccessor.getSubTaskManager()
        stm.createSubTaskIssueLink(issue, subTask, jac.getUser())


        // disable the watcher using the WatcherManager
        wtm = ComponentAccessor.getWatcherManager()
        wtm.stopWatching(jac.getUser(), subTask)

    }

}


    def check(String myvalue){
    def result = ""

    if (checkIfSubTaskSummaryExists(myvalue)) {
        result = 'vorhanden'
    }

    else {

     result = 'nicht vorhanden'
    }
}


def getRelease(){

    MutableIssue missue = (MutableIssue)issue;
    ArrayList val = (ArrayList)missue.getFixVersions()
    def release = (String)val[0]

}





//addComment(getCurrentUser())
//addComment(getCustomFieldValue("BusinessRequestor"))
//addComment(check('rolipoli'))
//addSubTask('b','my b description')
addComment(getRelease())