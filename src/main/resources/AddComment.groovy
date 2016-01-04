import com.atlassian.jira.component.ComponentAccessor
import com.opensymphony.workflow.WorkflowContext

/**
 * Created by roland on 04.01.16.
 */
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