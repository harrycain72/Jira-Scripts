// R.Wangemann
// V1.0
// 22.01.2016
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
import com.atlassian.crowd.embedded.api.User


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

    if(release == null) { release = "-"}

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

// sets n labels for a specific issue
def setLabels(Issue issue, Set labels, String fieldName){

    LabelManager labelManager = ComponentAccessor.getComponent(LabelManager.class)
    customFieldManager = ComponentAccessor.getCustomFieldManager()

    CustomField customField = customFieldManager.getCustomFieldObjectByName(fieldName)


    labelManager.setLabels(getCurrentUser(),issue.getId(),customField.getIdAsLong(),labels,false,true)

}


def convertToSetForLabels(String newValue){

    Set<String> set = new HashSet<String>();
    StringTokenizer st = new StringTokenizer(newValue," ")

    String myValue = ""


    while(st.hasMoreTokens()) {
        if(myValue == ""){

            myValue=myValue+st.nextToken()
        }

        else {
            myValue=myValue + "_" + st.nextToken()
        }


    }

    set.add(myValue)


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




def getSprintAndReleaseName(Issue issue){

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

        sprintName = "-"
    }

    return sprintName
}


def getIssuesOfNetwork(Issue issue, String issueType,String traversalDepth,String linkType){

    def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser)
    def searchProvider = ComponentAccessor.getComponent(SearchProvider)
    def issueManager = ComponentAccessor.getIssueManager()
    def user = getCurrentUser()
    def issueId = issue.getKey()
    def query






// query for sub tasks without linktype

    if(linkType==""){

        query = jqlQueryParser.parseQuery("issueFunction in linkedIssuesOfRecursiveLimited(\"issue =" + issueId + "\"," + traversalDepth + ") AND issuetype =" + issueType + "  ORDER BY issuetype DESC")

    }


    else {
        query = jqlQueryParser.parseQuery("issueFunction in linkedIssuesOfRecursiveLimited(\"issue =" + issueId + "\"," + traversalDepth + ",\"" + linkType + "\") AND issuetype =" + issueType + "  ORDER BY issuetype DESC")
    }

    def issues = searchProvider.search(query, user, PagerFilter.getUnlimitedFilter())

    return issues


}



def setReleaseAndSprintNamesInBusinesRequest(Issue issue,String customFieldName){


    //we need the IssueManager in order to create the issue of the the result of the query
    IssueManager issueManager = ComponentAccessor.getIssueManager()

    //get all stories linked by relates to
    def stories = getIssuesOfNetwork(issue,"Story","3","relates to").getIssues()

    //get the business request. We should only have one business request linked to the stories
    def businessRequests= getIssuesOfNetwork(issue,"Request","3","relates to").getIssues()

    //get the sprint name for each story. The names must be updated as labels in the business request

    Set<String> sprintNamesSet = new HashSet<String>()


    stories.each {

        //we create an issue
        def myIssue = issueManager.getIssueObject(it.getId())

        def mySprintAndReleaseName = getSprintAndReleaseName(myIssue)

        sprintNamesSet.add(mySprintAndReleaseName)

    }



    //we should have only one common business request for all found stories
    if (businessRequests.size()== 1){


        //we create an issue
        def myBusinessRequest = issueManager.getIssueObject(businessRequests.get(0).getId())

        setLabels(myBusinessRequest,sprintNamesSet,customFieldName)
    }


    // not nice
    println ""

    // end of new stuff
}

def updateComponents(Issue issue, Collection<ProjectComponent> components){

    IssueManager issueManager = ComponentAccessor.getIssueManager()

    MutableIssue mutableIssue = issue

    mutableIssue.setComponentObjects(components)

    User user = getCurrentUser().getDirectoryUser()

   issueManager.updateIssue(user,mutableIssue,com.atlassian.jira.event.type.EventDispatchOption.DO_NOT_DISPATCH,false)


}



def main(Issue issue){

    //begin customizing

    def customFieldNameRelease = ".Release"
    def customFieldNameSprint = ".Sprint"
    def customFieldNameSprintAndReleaseNames = ".Sprints"
    def customFieldNameDeveloper = ".Developer"
    //def FieldNameComponent =
    def nameOfPrefix = "DEV"
    def issueTypeNameSubTasks = "Unteraufgabe"
    def issueTypeNameStory = "Story"


    //end customizing

    // These names should be standard in JIRA and not change from release to release
    def myList = ["Component","Fix Version","Sprint","assignee"]

    def fix_version_update
    def sprint_update
    def assignee_update
    def component_update


    //def test = event.getChangeLog().getRelated('ChildChangeItem')

    for (item in myList) {

        def field = event.getChangeLog().getRelated('ChildChangeItem').find{it.field == item}


        if (field != null && item == "Component") {

            component_update = true


           Collection<ProjectComponent> myComponents = issue.getComponentObjects()

            def issueType = issue.getIssueTypeObject().getName()

            if (issueType == issueTypeNameStory) {

                // get all SubTasks for the story
                def subTasks = getIssuesOfNetwork(issue,issueTypeNameSubTasks,"1","").getIssues()

                //we need the IssueManager in order to create the issue of the the result of the query
                IssueManager issueManager = ComponentAccessor.getIssueManager()


                    subTasks.each {

                        //we create an issue
                        def myIssue = issueManager.getIssueObject(it.getId())

                        updateComponents(myIssue,myComponents)

                    }


            }


        }//end of handling of components

        if (field != null && item == "Fix Version"){
            fix_version_update = true

            setLabel(issue,getReleaseName(issue),customFieldNameRelease)
            setLabel(issue,getSprintAndReleaseName(issue),customFieldNameSprint)

            //if the release is changed but the sprint remains - which should not really be the case
            //then we must make sure, that this change is also available for the relevant business requests

            setReleaseAndSprintNamesInBusinesRequest(issue,customFieldNameSprintAndReleaseNames)

        }

        else if (field != null && item == "Sprint"){


            // set the name of the sprint name.


                setLabel(issue,getSprintAndReleaseName(issue),customFieldNameSprint)

            // every time a sprint is added or changed, we have to update the issue business request
            // with all sprint names of all stories as labels


                setReleaseAndSprintNamesInBusinesRequest(issue,customFieldNameSprintAndReleaseNames)

        }




        else if (field != null && item == "assignee") {
            assignee_update = true

            def issueType = issue.getIssueTypeObject().getName()
            def issueSummary = issue.getSummary()
            def keyWord = issueSummary.substring(0,3)



                if(issueType == issueTypeNameSubTasks && keyWord == nameOfPrefix){

                    def newAssignee = field.newstring


                    // set for this issue of type sub task the customfield .Developer t
                    if (newAssignee == null) {newAssignee = ""}

                    setLabel(issue,newAssignee,customFieldNameDeveloper)




                            //set for the parent issue of type story the customfield .Developer

                            // get my parent. For this look in the network of linked issues, from my point of view 1 level deep
                            // for a sub task the link type to its parent should be left blank
                            def queryResult = getIssuesOfNetwork(issue,"story","1","").getIssues()

                            //we need the IssueManager in order to create the issue of the the result of the query
                            IssueManager issueManager = ComponentAccessor.getIssueManager()

                        //every sub task should have only one parent
                        if (queryResult.size()== 1){


                            //we create an issue
                            def myIssue = issueManager.getIssueObject(queryResult.get(0).getId())

                            def issueKey = myIssue.getKey()

                            setLabel(myIssue,newAssignee,customFieldNameDeveloper)
                        }



                }

                println ""
        }

       else println ""
    }


}




// EV = Verwendung in Listerners, WV = Verwendung in Workflows

main(getCurrentIssue("EV"))


