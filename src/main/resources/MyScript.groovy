import com.atlassian.greenhopper.service.sprint.Sprint
import com.atlassian.jira.bc.project.component.ProjectComponent
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.issue.IssueEvent
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.issue.ModifiedValue
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem
import com.atlassian.jira.issue.label.LabelManager
import com.atlassian.jira.issue.search.SearchProvider
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.web.bean.PagerFilter

//method retrieves the current user
def getCurrentUser() {
    //determine current user

    //Security
    jac = ComponentAccessor.getJiraAuthenticationContext()

    def CurrentUser

    CurrentUser = jac.getUser()


    return CurrentUser
}

//this method creates a comment
def addComment(Issue issue, String myComment) {

    cmm = ComponentAccessor.getCommentManager()

    //cmm.create(issue,getCurrentUser(),myComment,true)
    cmm.create(issue,getCurrentUser(),myComment,true)


}


//this method gets the value of a customfield value by its name
def getCustomFieldValue(Issue issue, String myCustomField) {

    cfm = ComponentAccessor.getCustomFieldManager()

    CustomField customField = cfm.getCustomFieldObjectByName(myCustomField);
    return  (String)customField.getValue(issue);

}

// this method returns a customfield
def getCustomField(String myCustomFieldName) {

    cfm = ComponentAccessor.getCustomFieldManager()

    CustomField myCustomField = cfm.getCustomFieldObjectByName(myCustomFieldName);
    return  myCustomField

}


//this method gets a list of subtasks of an issue, retrieves their summary and checks if a defined one exists in this list.
def checkIfSubTaskSummaryExists(Issue issue, String mySummaryToBeChecked) {

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
def addSubTask(Issue issue, String subTaskName, String subTaskDescription) {

    //Instanzierung der factories
    isf = ComponentAccessor.getIssueFactory()

    //IssueFactory: we create her a generic issue
    def issueObject = isf.getIssue()

    issueObject.setProjectObject(issue.getProjectObject())

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
    checkResult = checkIfSubTaskSummaryExists(issue,toBeCreatedSubTaskSummary)

    // we only create our new SubTask if the the value of summary does not exist in any already defined subtask
    if (!checkResult) {

        //the issue gets created with the IssueMngr
        ism = ComponentAccessor.getIssueManager()

        //Security
        //jac = ComponentAccessor.getJiraAuthenticationContext()

        currentUser = getCurrentUser()

        subTask = ism.createIssueObject(currentUser, issueObject)


        //the created subtask is linked to the issue.This is done through the SubTaskMngr

        stm = ComponentAccessor.getSubTaskManager()
        stm.createSubTaskIssueLink(issue, subTask, currentUser)


        // disable the watcher using the WatcherManager
        wtm = ComponentAccessor.getWatcherManager()
        wtm.stopWatching(currentUser, subTask)

    }

}



//Method retrieves the Fixed Version name of the current issue
def getReleaseName(Issue issue){

   // MutableIssue myMutableIssue = (MutableIssue)issue;// Flag EV is necessary to be able to be triggered by an event / listener

    ArrayList myListReleases = (ArrayList)issue.getFixVersions()

    def release = ""

    if(myListReleases!=null){



        //we only consider getting the first item, even though more fix versions can be assigned to an issue
        release = (String)myListReleases[0]

    }

    return release
}

// method reetrieves the assigend sprint of an issue
def getSprintName(Issue issue){

    ArrayList<Sprint> listOfSprints = (ArrayList<Sprint>) ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Sprint").getValue(issue);

    def SprintName =""

   if(listOfSprints!=null){

       //we only consider getting the first sprint in the list, event though more sprints can be assigened to an issue
            SprintName = (String)listOfSprints[0].getName()
        }

   else {
        //do something else

    }

    return SprintName
}

def setCustomFieldValue(Issue issue, String myValueToSave, CustomField myCustomField){

    def MutableIssue myMutableIssue = (MutableIssue)issue

    myMutableIssue.setCustomFieldValue(myCustomField,myValueToSave)


    Map<String,ModifiedValue> modifiedfields = myMutableIssue.getModifiedFields()

    FieldLayoutItem myFieldLayoutItem = ComponentAccessor.getFieldLayoutManager().getFieldLayout(myMutableIssue).getFieldLayoutItem(myCustomField)

    DefaultIssueChangeHolder myDefaultIssueChangeHolder = new DefaultIssueChangeHolder()

    final ModifiedValue myModifiedValue = modifiedfields.get(myCustomField.getId())

    myCustomField.updateValue(myFieldLayoutItem,myMutableIssue,myModifiedValue,myDefaultIssueChangeHolder)

}

def setLabel(Issue issue, String newValue, String fieldName){

    LabelManager labelManager = ComponentAccessor.getComponent(LabelManager.class)
    customFieldManager = ComponentAccessor.getCustomFieldManager()

    CustomField customField = customFieldManager.getCustomFieldObjectByName(fieldName)


    Set<String> set = convertToSetForLabels((String) newValue)


    labelManager.setLabels(getCurrentUser(),issue.getId(),customField.getIdAsLong(),set,false,true)

}

def convertToSetForLabels(String newValue){

    Set<String> set = new HashSet<String>();
    StringTokenizer st = new StringTokenizer(newValue,"  ")
    while(st.hasMoreTokens()) {
        set.add(st.nextToken())
    }
    return set

}


// This method retrieves the issue based on its key
def getIssueByKey(String myIssueKey){


    MutableIssue myMutableIssue = ComponentAccessor.getIssueManager().getIssueByCurrentKey(myIssueKey);

    return myMutableIssue
}

def getComponentName(Issue myIssue){

    def MutableIssue myMutableIssue = (MutableIssue)myIssue

    ArrayList<ProjectComponent> myComponents = (ArrayList<ProjectComponent>)myMutableIssue.getComponentObjects()

    def myComponentName = ""

    if (myComponents!=null){

        //we only retrieve the first assigned component.
        myComponentName = (String)myComponents[0].getName()

    }


    return myComponentName

}

def getTodaysDate(){
    def today = new Date()
    return today.toString()
}

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




def setSprintName(Issue issue){

    def sprint = getSprintName(issue)

    def release = getReleaseName(issue)

    def sprintName

    if(sprint != "" && release != null){

        sprintName = release + "_" + sprint
    }

    if(sprint != "" && release == null) {

        sprintName = "_"+sprint
    }

    if(sprint == "" && release != null) {

        sprintName = ""
    }

    return sprintName
}


def getIssuesOfNetwork(Issue issue, String issueType,String traversalDepth){

    def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser)
    def searchProvider = ComponentAccessor.getComponent(SearchProvider)
    def issueManager = ComponentAccessor.getIssueManager()
    def user = getCurrentUser()
    def issueId = issue.getKey()




// edit this query to suit
    def query = jqlQueryParser.parseQuery("issueFunction in linkedIssuesOfRecursiveLimited(\"issue =" + issueId + "\"," + traversalDepth + ") AND issuetype =" + issueType + "  ORDER BY issuetype DESC")

    def issues = searchProvider.search(query, user, PagerFilter.getUnlimitedFilter())

    return issues


}




def main(Issue issue){

    def myList = ["Fix Version","Sprint","assignee"]

    def fix_version_update
    def sprint_update
    def assignee_update


    //def test = event.getChangeLog().getRelated('ChildChangeItem')

    for (item in myList) {

        def field = event.getChangeLog().getRelated('ChildChangeItem').find{it.field == item};

        if (field != null && item == "Fix Version"){
            fix_version_update = true

            setLabel(issue,getReleaseName(issue),".Release")
            setLabel(issue,setSprintName(issue),".Sprint")

        }

        else if (field != null && item == "Sprint"){
            sprint_update = true

            setLabel(issue,setSprintName(issue),".Sprint")
        }

        else if (field != null && item == "assignee") {
            assignee_update = true

            def issueType = issue.getIssueTypeObject().getName()
            def issueSummary = issue.getSummary()
            def keyWord = issueSummary.substring(0,3)



                if(issueType == "Unteraufgabe" && keyWord == "DEV"){

                    def newAssignee = field.newstring


                    // set for this issue of type sub task the customfield .Developer t
                    if (newAssignee == null) {newAssignee = ""}

                    setLabel(issue,newAssignee,".Developer")



                            //set for the parent issue of type story the customfield .Developer

                            // get my parent. For this look in the network of linked issues, from my point of view 1 level deep
                            def queryResult = getIssuesOfNetwork(issue,"story","1").getIssues()

                            //we need the IssueManager in order to create the issue of the the result of the query
                            IssueManager issueManager = ComponentAccessor.getIssueManager()

                        //every sub task should have only one parent
                        if (queryResult.size()== 1){


                            //we create an issue
                            def myIssue = issueManager.getIssueObject(queryResult.get(0).getId())

                            def issueKey = myIssue.getKey()

                            setLabel(myIssue,newAssignee,".Developer")
                        }



                }

                println ""
        }

       else println ""
    }


}




// EV = Verwendung in Listerners, WV = Verwendung in Workflows

main(getCurrentIssue("EV"))


//****************

//addComment(getCurrentIssue("EV"),'great comment')

//addComment(getCurrentIssue("EV"),getCurrentUser().getName())

//addComment(getCurrentIssue("EV"),getCustomFieldValue(getCurrentIssue("EV"),".BusinessRequestor"))

//addSubTask(getCurrentIssue("WF"),'b','my b description')

//addComment(getCurrentIssue("EV"),getReleaseName(getCurrentIssue("EV")))

//addComment(getCurrentIssue("EV"),getSprintName(getCurrentIssue("EV")))

//setCustomFieldValue(getCurrentIssue("EV"),getReleaseName(getCurrentIssue("EV")),getCustomField('.BusinessRequestor'))

//addComment(getCurrentIssue("EV"),getComponentName(getCurrentIssue("EV")))

//addComment(getIssueByKey("DEMO-1"),getComponentName(getCurrentIssue("EV")))

//addComment(getCurrentIssue("EV"),getTodaysDate())

//setCustomFieldValue(getCurrentIssue("EV"),"R16.3",getCustomField(".Release"))

//getIssuesOfNetwork(getCurrentIssue("EV"),"story","1")

//setLabel(getIssueByKey("DEMO-1"),"roland",".Developer")
