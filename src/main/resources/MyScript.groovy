// R.Wangemann
// V1.1
// 24.01.2016
import com.atlassian.greenhopper.service.sprint.Sprint
import com.atlassian.jira.bc.issue.link.DefaultRemoteIssueLinkService
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
import com.atlassian.jira.issue.link.DefaultRemoteIssueLinkManager
import com.atlassian.jira.issue.link.RemoteIssueLink
import com.atlassian.jira.issue.link.RemoteIssueLinkBuilder
import com.atlassian.jira.issue.search.SearchProvider
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.crowd.embedded.api.User


//method retrieves the current user
def getCurrentApplicationUser() {
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

    //cmm.create(issue,getCurrentApplicationUser(),myComment,true)
    cmm.create(issue,getCurrentApplicationUser(),myComment,true)


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

        currentUser = getCurrentApplicationUser()

        subTask = ism.createIssueObject(currentApplicationUser, issueObject)


        //the created subtask is linked to the issue.This is done through the SubTaskMngr

        stm = ComponentAccessor.getSubTaskManager()
        stm.createSubTaskIssueLink(issue, subTask, currentApplicationUser)


        // disable the watcher using the WatcherManager
        wtm = ComponentAccessor.getWatcherManager()
        wtm.stopWatching(currentApplicationUser, subTask)

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


    labelManager.setLabels(getCurrentApplicationUser(),issue.getId(),customField.getIdAsLong(),set,false,true)

}

// sets n labels for a specific issue
def setLabels(Issue issue, Set labels, String fieldName){

    LabelManager labelManager = ComponentAccessor.getComponent(LabelManager.class)
    customFieldManager = ComponentAccessor.getCustomFieldManager()

    CustomField customField = customFieldManager.getCustomFieldObjectByName(fieldName)


    labelManager.setLabels(getCurrentApplicationUser(),issue.getId(),customField.getIdAsLong(),labels,false,true)

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


    Issue issue = ComponentAccessor.getIssueManager().getIssueByCurrentKey(myIssueKey);

    return issue
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



def getIssuesOfNetwork(Issue issue,String traversalDepth,String linkType){

    def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser)
    def searchProvider = ComponentAccessor.getComponent(SearchProvider)
    def issueManager = ComponentAccessor.getIssueManager()
    def user = getCurrentApplicationUser()
    def issueId = issue.getKey()
    def query



// query for sub tasks without linktype

    if(linkType==""){

        query = jqlQueryParser.parseQuery("issueFunction in linkedIssuesOfRecursiveLimited(\"issue =" + issueId + "\"," + traversalDepth + ")  ORDER BY issuetype DESC")

    }


    else {
        query = jqlQueryParser.parseQuery("issueFunction in linkedIssuesOfRecursiveLimited(\"issue =" + issueId + "\"," + traversalDepth + ",\"" + linkType + "\")  ORDER BY issuetype DESC")
    }

    def issues = searchProvider.search(query, user, PagerFilter.getUnlimitedFilter())

    return issues


}





def getIssuesOfNetwork(Issue issue, String issueType,String traversalDepth,String linkType){

    def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser)
    def searchProvider = ComponentAccessor.getComponent(SearchProvider)
    def issueManager = ComponentAccessor.getIssueManager()
    def user = getCurrentApplicationUser()
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

    User user = getCurrentApplicationUser().getDirectoryUser()

   issueManager.updateIssue(user,mutableIssue,com.atlassian.jira.event.type.EventDispatchOption.DO_NOT_DISPATCH,false)


}




// feature copy link to confluence from one issue to the linked issues
//


def syncExternalLinks(Issue issue){

    //we need the IssueManager in order to create the issue of the the result of the query
    IssueManager issueManager = ComponentAccessor.getIssueManager()

    //we need to know what kind of issue was updated
    // the reason is, if it was an epic, then we have to select of all stories in network
    // only those that are linked to the epic.
    // Only those stories with an link to the epic will get copied the links
    //Background: we get all issues linked directly and indirectly to the current issue.
    //If the business request should have linked stories, which are not linked to the epic,
    //then these are also included in the query result, when the update was done for an epic

    def currentIssueType = issue.getIssueTypeObject().getName()


    //now we get all issues in the network with level 0 = current issue
    // consider max three levels in each direction of current issue
    // we don't limit the result based on type of relationship
    // we consider all types of issues

    def issuesInNetwork = getIssuesOfNetwork(issue,"3","").getIssues()



    //
    //sort all issues depending on their issueType

    List stories = []
    List epics = []
    List businessRequests = []
    List requirements = []
    List testCases = []

    List remainingIssueTypes = []





    for(Issue item : issuesInNetwork){

        def myIssue = issueManager.getIssueObject(item.getId())

        def myIssueType = item.getIssueTypeObject().getName()

        //we need the issueLinkMnager in order to be able to retriev the internal links for all stories
        //as we only want to have those stories that have a relationship to an epic
        def issueLinkManager = ComponentAccessor.getIssueLinkManager()


        if (myIssue.getIssueTypeObject().getName()=="Story"){

            stories.add(item)
        }

        else if (myIssue.getIssueTypeObject().getName() == "Epic"){
            epics.add(item)
        }

        else if (myIssue.getIssueTypeObject().getName() == "Business Request"){
            businessRequests.add(item)
        }

        else if (myIssue.getIssueTypeObject().getName() == "Requirement"){

            requirements.add(item)
        }

        else if (myIssue.getIssueTypeObject().getName() == "Test Case"){

            testCases.add(item)
        }



        else {

                remainingIssueTypes.add(item)
        }


        println "z"



    }



    //now prepare the list of issues to which we have to sync the links depending on the issue type of the
    // current issue.


    def issueTypeOfCurrentIssue = issue.getIssueTypeObject().getName()

    //to all issues in this list we will have to sync the external links of the current issue
    List relevantIssuesToCopyLinksTo = []

    //we need this list if the current issue is a requirement or test case
    //in these cases we have to switch the current issue as often as we have stories, epics or business requests in the network of issues
    //Reason: we want to make sure that all links of the above mentioned issues are copied.
    List currentIssueStories = []
    List currentIssueBusinessRequests = []
    List currentIssueEpics = []


    if (issueTypeOfCurrentIssue == "Story"){


       for (Issue item : requirements){
           relevantIssuesToCopyLinksTo.add(item)
       }


       for (Issue item : testCases){
           relevantIssuesToCopyLinksTo.add(item)
       }


    }



    else if (issueTypeOfCurrentIssue == "Business Request"){

        relevantIssuesToCopyLinksTo = stories

        for (Issue item : requirements){
            relevantIssuesToCopyLinksTo.add(item)
        }


        for (Issue item : testCases){
            relevantIssuesToCopyLinksTo.add(item)
        }


    }


    else if (issueTypeOfCurrentIssue == "Epic") {


        //here we have to find a way to get rid of the stories, that are not linked to the EPIC
        relevantIssuesToCopyLinksTo = stories

        for (Issue item : requirements){
            relevantIssuesToCopyLinksTo.add(item)
        }


        for (Issue item : testCases){
            relevantIssuesToCopyLinksTo.add(item)
        }


    }

    else if (issueTypeOfCurrentIssue == "Requirement") {


        //handle stories

        currentIssueStories = stories

        for (Issue item : requirements){
            relevantIssuesToCopyLinksTo.add(item)
        }


        for (Issue item : testCases){
            relevantIssuesToCopyLinksTo.add(item)
        }

        for(Issue item : currentIssueStories){

            copyAndDeleteExternalLinks(item,relevantIssuesToCopyLinksTo)
        }

        // handle epics


        currentIssueEpics = epics

        //here we have to find a way to get rid of the stories, that are not linked to the EPIC

        //reset
        relevantIssuesToCopyLinksTo = []

        relevantIssuesToCopyLinksTo = stories

        for (Issue item : requirements){
            relevantIssuesToCopyLinksTo.add(item)
        }


        for (Issue item : testCases){
            relevantIssuesToCopyLinksTo.add(item)
        }

        for (Issue item : currentIssueEpics){
            copyAndDeleteExternalLinks(item,relevantIssuesToCopyLinksTo)

        }


        //handle business requests

        currentIssueBusinessRequests = businessRequests

        //reset
        relevantIssuesToCopyLinksTo = []

        relevantIssuesToCopyLinksTo = stories

        for (Issue item : requirements){
            relevantIssuesToCopyLinksTo.add(item)
        }


        for (Issue item : testCases){
            relevantIssuesToCopyLinksTo.add(item)
        }

        for (Issue item : currentIssueBusinessRequests){

            copyAndDeleteExternalLinks(item, relevantIssuesToCopyLinksTo)
        }

    }


    else {
        println "z"
    }



    if (issueTypeOfCurrentIssue == "Story" || "Business Request" || "Epic"){

        copyAndDeleteExternalLinks(issue,relevantIssuesToCopyLinksTo)

    }





}



def copyAndDeleteExternalLinks(Issue currentIssue, List<Issue> issuesToCopyLinksTo){




    try {

        def issueLinkManager = ComponentAccessor.getIssueLinkManager()

        def remoteIssueLinkManager = ComponentAccessor.getComponentOfType(DefaultRemoteIssueLinkManager.class)


        //get all remote links = external links for the current issue
        List sourceLinks = remoteIssueLinkManager.getRemoteIssueLinksForIssue(currentIssue)



        List sourceURLs =[]

        //create a list of all available sourceURLS
        if (sourceLinks.size() != 0) {


            for (RemoteIssueLink item : sourceLinks){

                def Url = item.getUrl()

                sourceURLs.add(Url)
            }

        }



        for (Issue itemInList : issuesToCopyLinksTo){


                //define all issues, for which we want the link to be copied to
                def newIssue = getIssueByKey(itemInList.getKey())



                //get all remote links = external links for each target issue
                List targetLinks = remoteIssueLinkManager.getRemoteIssueLinksForIssue(newIssue)


                //get for all external links of source issue the globalIDs
                List targetURLs = []

                //create a list of all available targetURLS
                if (targetLinks.size() != 0) {


                    for (RemoteIssueLink item : targetLinks){

                        def Url = item.getUrl()

                        targetURLs.add(Url)
                    }

                }


                //create a list for only the links that do not exist in the target issue
                //For this we check if in the list of all URLs existing in the target issue,
                //the URLs of all available links in the source issue exist.
                List relevantLinks = []

                for (RemoteIssueLink item : sourceLinks){

                        def found = targetURLs.find{it == item.getUrl()}

                        if (found == null) {

                            relevantLinks.add(item)
                        }

                }



                //create a list for those links, that have been deleted in the original issue (type story) and still exist
                //in the target issues. Remember, we want to have the links synchronized.


                List linksToBeDeletedInTargetIssue = []

                for (RemoteIssueLink item : targetLinks){

                    if (item.getSummary()==newIssue.getKey()){

                        def found = sourceURLs.find{it == item.getUrl()}

                        if(found == null){

                            linksToBeDeletedInTargetIssue.add(item)
                        }


                    }

                }


                //In order to create, delete or update a link we need the a remoteIssueLinkService
                def remoteIssueLinkService = ComponentAccessor.getComponentOfType(DefaultRemoteIssueLinkService.class)


                //first we have to delete the links in the target issue with origin source issue, but don't exist there anymore.

                for (RemoteIssueLink item : linksToBeDeletedInTargetIssue) {

                    def deleteValidationResult = remoteIssueLinkService.validateDelete(getCurrentApplicationUser(),item.getId())

                    remoteIssueLinkService.delete(getCurrentApplicationUser(),deleteValidationResult)

                }




                //second, we want to create the missing links
                //For all relevant links we create an exact copy of every RemoteIssueLink
                //first we need to configure our link 1:1 to the existing ones.

                def linkBuilder = new RemoteIssueLinkBuilder()

                        for (RemoteIssueLink item : relevantLinks) {



                            //we create an exact copy of the existing link

                            //We only change the id of the issue
                            linkBuilder.issueId(newIssue.getId())
                            //We need the id of the source issue, just to be able to synchronize the links
                            //When a link is deleted in the source issue, the same link should be deleted in the target issue(s)
                            linkBuilder.summary(newIssue.getKey())

                            //we copy the rest
                            linkBuilder.globalId(item.getGlobalId())
                            linkBuilder.title(item.getTitle())
                            linkBuilder.url(item.getUrl())
                            linkBuilder.iconUrl(item.getIconUrl())
                            linkBuilder.iconTitle(item.getIconTitle())
                            linkBuilder.relationship(item.getRelationship())
                            linkBuilder.applicationType(item.getApplicationType())
                            linkBuilder.applicationName(item.getApplicationName())

                            def newLink = linkBuilder.build()


                            //check if the issue already has got this link assigned to





                            def createValidationResult = remoteIssueLinkService.validateCreate(getCurrentApplicationUser(),newLink)

                            remoteIssueLinkService.create(getCurrentApplicationUser(),createValidationResult)

                        }//end for

        println "z"

        }
    }

    catch (all){

    }


}


def handelIssueUpdateAndAssignEvents(Issue issue){

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

        else {
            syncExternalLinks(issue)
        }
    }


}


//**********************************************
// EV = Verwendung in Listerners, WV = Verwendung in Workflows

handelIssueUpdateAndAssignEvents(getCurrentIssue("EV"))



