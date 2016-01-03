import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.fields.CustomField
import com.opensymphony.workflow.WorkflowContext


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


addComment(getCurrentUser())
addComment(getCustomFieldValue("BusinessRequestor"))
