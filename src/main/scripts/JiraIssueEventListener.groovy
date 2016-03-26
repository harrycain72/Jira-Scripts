// R.Wangemann
import com.atlassian.jira.bc.issue.link.DefaultRemoteIssueLinkService
import com.atlassian.jira.bc.project.component.ProjectComponent
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.issue.IssueEvent
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.issue.link.DefaultRemoteIssueLinkManager
import com.atlassian.jira.issue.link.RemoteIssueLink
import com.atlassian.jira.issue.link.RemoteIssueLinkBuilder
import com.atlassian.jira.issue.search.SearchProvider
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.web.bean.PagerFilter
import org.apache.log4j.Category
import util.Helper


//method retrieves the current application user
def getCurrentApplicationUser() {
    //determine current user

    //Security
    jac = ComponentAccessor.getJiraAuthenticationContext()

    def CurrentUser

    CurrentUser = jac.getUser()


    return CurrentUser
}

def getCurrentUser() {

    //Security
    jac = ComponentAccessor.getJiraAuthenticationContext()

    currentUser = jac.getUser().getDirectoryUser()
}

// This method retrieves the issue based on its key
def getIssueByKey(String myIssueKey){


    Issue issue = ComponentAccessor.getIssueManager().getIssueByCurrentKey(myIssueKey);

    return issue
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

    else if(linkType=="relates_to"){

        query = jqlQueryParser.parseQuery("issueFunction in linkedIssuesOfRecursiveLimited(\"issue =" + issueId + "\"," + traversalDepth + ",\"relates to\")  ORDER BY issuetype DESC")

    }

    else if(linkType=="has_Epic"){

        query = jqlQueryParser.parseQuery("issueFunction in linkedIssuesOfRecursiveLimited(\"issue =" + issueId + "\"," + traversalDepth + ",\"has Epic\")  ORDER BY issuetype DESC")

    }

    else if(linkType=="is_Epic_of"){

        query = jqlQueryParser.parseQuery("issueFunction in linkedIssuesOfRecursiveLimited(\"issue =" + issueId + "\"," + traversalDepth + ",\"is Epic of\")  ORDER BY issuetype DESC")

    }

    else if(linkType=="is_validated_by"){

        query = jqlQueryParser.parseQuery("issueFunction in linkedIssuesOfRecursiveLimited(\"issue =" + issueId + "\"," + traversalDepth + ",\"is validated by\")  ORDER BY issuetype DESC")

    }

    else if(linkType=="is_tested_by"){

        query = jqlQueryParser.parseQuery("issueFunction in linkedIssuesOfRecursiveLimited(\"issue =" + issueId + "\"," + traversalDepth + ",\"is tested by\")  ORDER BY issuetype DESC")

    }

    else if(linkType=="tests"){

        query = jqlQueryParser.parseQuery("issueFunction in linkedIssuesOfRecursiveLimited(\"issue =" + issueId + "\"," + traversalDepth + ",\"tests\")  ORDER BY issuetype DESC")

    }

    else if(linkType=="validats"){

        query = jqlQueryParser.parseQuery("issueFunction in linkedIssuesOfRecursiveLimited(\"issue =" + issueId + "\"," + traversalDepth + ",\"validates\")  ORDER BY issuetype DESC")

    }


    else {
        query = jqlQueryParser.parseQuery("issueFunction in linkedIssuesOfRecursiveLimited(\"issue =" + issueId + "\"," + traversalDepth + ",\"" + linkType + "\")  ORDER BY issuetype DESC")
    }

    def issues = searchProvider.search(query, user, PagerFilter.getUnlimitedFilter())

    return issues


}

// feature copy link to confluence from one issue to the linked issues
def syncExternalLinks(Issue issue){



    //start customizing

    def issueTypeStory = "Story"
    def issueTypeEpic = "Epic"
    def issueTypeBusinessRequest = "Business Request"
    def issueTypeRequirement = "Requirement"
    def issueTypeTestCase = "Test Case"
    def issueTypePKE = "PKE"
    def issueTypeBug = "Bug"

    //end customizing



    //we need the IssueManager in order to create the issue of the the result of the query
    IssueManager issueManager = ComponentAccessor.getIssueManager()


    def currentIssueType = issue.getIssueTypeObject().getName()


    //now we get all issues in the network with level 0 = current issue
    // consider max three levels in each direction of current issue
    // we don't limit the result based on type of relationship
    // we consider all types of issues

    //List issuesInNetwork = []

    Set<Issue> issuesInNetwork = new HashSet<Issue>()
    def issueType
    List myTempList = []

    if(issue.getIssueTypeObject().getName()== issueTypeBusinessRequest){

        //gets all issues of type story and excludes the BusinessRequest
        myTempList = getIssuesOfNetwork(issue,"3","relates to").getIssues()


        for (Issue item : myTempList){
            if(item.getIssueTypeObject().getName() != issueTypeBusinessRequest ){
                issuesInNetwork.add(item)
            }
        }

        //get the requirements for each story

        myTempList = []

        for (Issue item : issuesInNetwork){
            if(item.getIssueTypeObject().getName() == issueTypeStory ){

                myTempList.addAll(getIssuesOfNetwork(item, "1", "is validated by").getIssues())

            }
        }

        issuesInNetwork.addAll(myTempList)

        myTempList = []
        //get the test cases for each requirement
        for (Issue item : issuesInNetwork){
            if(item.getIssueTypeObject().getName() == issueTypeRequirement ){

                myTempList.addAll(getIssuesOfNetwork(item, "1", "is tested by").getIssues())

            }
        }

        issuesInNetwork.addAll(myTempList)


        //get the bugs for each test case
        myTempList = []
        for (Issue item : issuesInNetwork){
            if(item.getIssueTypeObject().getName() == issueTypeTestCase ){

                myTempList.addAll(getIssuesOfNetwork(item, "1", "is blocked by").getIssues())

            }
        }
        issuesInNetwork.addAll(myTempList)

    }


    if(issue.getIssueTypeObject().getName()== issueTypePKE){

        //gets all issues of type story and excludes the PKE
        myTempList =[]
        for (Issue item : getIssuesOfNetwork(issue,"3","relates to").getIssues()){
            if(item.getIssueTypeObject().getName() != issueTypePKE ){
                myTempList.add(item)
            }
        }

        issuesInNetwork.addAll(myTempList)



        //get the requirements for each story
        myTempList = []

        for (Issue item : issuesInNetwork){
            if(item.getIssueTypeObject().getName() == issueTypeStory ){
                myTempList.addAll(getIssuesOfNetwork(item, "1", "is validated by").getIssues())
            }
        }

        issuesInNetwork.addAll(myTempList)



        //get the testcases for each requirement
        myTempList = []
        for (Issue item : issuesInNetwork){
            if(item.getIssueTypeObject().getName() == issueTypeRequirement ){
                myTempList.addAll(getIssuesOfNetwork(item, "1", "is tested by").getIssues())
            }
        }

        issuesInNetwork.addAll(myTempList)


        //get the bugs for each testcase
        myTempList = []
        for (Issue item : issuesInNetwork){
            if(item.getIssueTypeObject().getName() == issueTypeTestCase ){
                myTempList.addAll(getIssuesOfNetwork(item, "1", "is blocked by").getIssues())
            }
        }

        issuesInNetwork.addAll(myTempList)
    }





    if(issue.getIssueTypeObject().getName()== issueTypeStory){

        //gets all issues of type PKE or Business Request
        myTempList = []
        for (Issue item : getIssuesOfNetwork(issue,"3","relates to").getIssues()){
            if(item.getIssueTypeObject().getName() != issueTypeStory ){
                myTempList.add(item)
            }
        }

        issuesInNetwork.addAll(myTempList)


        //get the requirements for the story
        myTempList = getIssuesOfNetwork(issue, "1", "is validated by").getIssues()
        for (Issue item : myTempList){
            issuesInNetwork.add(item)
        }



        //get the testcases for each requirement
        myTempList = []
        for (Issue item : issuesInNetwork){
            if(item.getIssueTypeObject().getName() == issueTypeRequirement ){
                myTempList.addAll(getIssuesOfNetwork(item, "1", "is tested by").getIssues())
            }
        }

        issuesInNetwork.addAll(myTempList)


        //get the bugs for each requirement
        myTempList = []
        for (Issue item : issuesInNetwork){
            if(item.getIssueTypeObject().getName() == issueTypeTestCase ){
                myTempList.addAll(getIssuesOfNetwork(item, "1", "is blocked by").getIssues())
            }
        }

        issuesInNetwork.addAll(myTempList)
    }


    if(issue.getIssueTypeObject().getName()== issueTypeRequirement){

        //get the test cases
        myTempList = getIssuesOfNetwork(issue, "1", "is tested by").getIssues()
        for(Issue item : myTempList){
            issuesInNetwork.add(item)
        }


        //get the bugs for each test case
        myTempList = []
        for (Issue item : issuesInNetwork){
            if(item.getIssueTypeObject().getName() == issueTypeTestCase ){
                myTempList.addAll(getIssuesOfNetwork(item, "1", "is blocked by").getIssues())
            }
        }

        issuesInNetwork.addAll(myTempList)

        // get the stories
        myTempList = getIssuesOfNetwork(issue, "1", "validates").getIssues()
        for(Issue item : myTempList){
            issuesInNetwork.add(item)
        }


        // get the business requests or PKEs
        myTempList = []
        for (Issue item : issuesInNetwork){
            if(item.getIssueTypeObject().getName() == issueTypeStory ){
                myTempList.addAll(getIssuesOfNetwork(item, "2", "is related").getIssues())
            }
        }

        issuesInNetwork.addAll(myTempList)

    }

    if(issue.getIssueTypeObject().getName()== issueTypeTestCase){

        //get the bugs
        myTempList = getIssuesOfNetwork(issue, "1", "is blocked").getIssues()
        for (Issue item : myTempList){
            issuesInNetwork.add(item)
        }


        // get the requirements
        myTempList = getIssuesOfNetwork(issue, "1", "tests").getIssues()
        for(Issue item : myTempList){
            issuesInNetwork.add(item)
        }


        // get the stories
        myTempList = []
        for (Issue item : issuesInNetwork){
            if(item.getIssueTypeObject().getName() == issueTypeRequirement ){
                myTempList.addAll(getIssuesOfNetwork(item, "1", "validates").getIssues())
            }
        }

        issuesInNetwork.addAll(myTempList)


        // get the business requests or PKEs
        myTempList = []
        for (Issue item : issuesInNetwork){
            if(item.getIssueTypeObject().getName() == issueTypeStory ){
                myTempList.addAll(getIssuesOfNetwork(item, "2", "is related").getIssues())
            }
        }

        issuesInNetwork.addAll(myTempList)

    }


    if(issue.getIssueTypeObject().getName()== issueTypeEpic){

        myTempList = getIssuesOfNetwork(issue,"2","is_Epic_of").getIssues()
        for (Issue item : myTempList){
            issuesInNetwork.add(item)
        }
    }


    //
    //sort all issues depending on their issueType

    List stories = []
    List epics = []
    List businessRequests = []
    List requirements = []
    List testCases = []
    List pkes = []
    List bugs = []

    List remainingIssueTypes = []





    for(Issue item : issuesInNetwork){

        def myIssue = issueManager.getIssueObject(item.getId())

        def myIssueType = item.getIssueTypeObject().getName()

        //we need the issueLinkMngr in order to be able to retrieve the internal links for all stories
        //as we only want to have those stories that have a relationship to an epic
        def issueLinkManager = ComponentAccessor.getIssueLinkManager()


        if (myIssue.getIssueTypeObject().getName()==issueTypeStory){

            stories.add(item)
        }

        else if (myIssue.getIssueTypeObject().getName() == issueTypeEpic){
            epics.add(item)
        }

        else if (myIssue.getIssueTypeObject().getName() == issueTypeBusinessRequest){
            businessRequests.add(item)
        }

        else if (myIssue.getIssueTypeObject().getName() == issueTypeRequirement){

            requirements.add(item)
        }

        else if (myIssue.getIssueTypeObject().getName() == issueTypeTestCase){

            testCases.add(item)
        }

        else if (myIssue.getIssueTypeObject().getName() == issueTypePKE){

            pkes.add(item)
        }

        else if (myIssue.getIssueTypeObject().getName() == issueTypeBug){

            bugs.add(item)
        }



    }

    configureSync(issue,businessRequests,epics,stories,requirements,testCases,pkes,bugs)

}

def configureSync(Issue issue,List businessRequests, List Epics, List stories, List requirements, List testCases, List pkes, List bugs){


    //start customizing

    def issueTypeStory = "Story"
    def issueTypeEpic = "Epic"
    def issueTypeBusinessRequest = "Business Request"
    def issueTypeRequirement = "Requirement"
    def issueTypeTestCase = "Test Case"
    def issueTypePke = "PKE"
    def issueTypeBug = "Bug"

    //end customizing


    //now prepare the list of issues to which we have to sync the links depending on the issue type of the
    // current issue.


    def issueTypeOfCurrentIssue = issue.getIssueTypeObject().getName()

    //to all issues in this list we will have to sync the external links of the current issue
    List relevantIssuesToCopyLinksTo = []



    //If the current issue is of type business request, then we have to copy or delete the external links, which belong
    // to the business request to all linked stories, requirements and test cases.

    if (issueTypeOfCurrentIssue == issueTypeBusinessRequest){



        for (Issue item : stories){
            relevantIssuesToCopyLinksTo.add(item)
        }

        for (Issue item : requirements){
            relevantIssuesToCopyLinksTo.add(item)
        }


        for (Issue item : testCases){
            relevantIssuesToCopyLinksTo.add(item)
        }

        for (Issue item : bugs){
            relevantIssuesToCopyLinksTo.add(item)
        }

        // we trigger here that all external links are copied or deleted



        copyAndDeleteExternalLinks(issue,relevantIssuesToCopyLinksTo)


    }


    else if (issueTypeOfCurrentIssue == issueTypePke){



        for (Issue item : stories){
            relevantIssuesToCopyLinksTo.add(item)
        }



        for (Issue item : requirements){
            relevantIssuesToCopyLinksTo.add(item)
        }


        for (Issue item : testCases){
            relevantIssuesToCopyLinksTo.add(item)
        }

        for (Issue item : bugs){
            relevantIssuesToCopyLinksTo.add(item)
        }

        // we trigger here that all external links are copied or deleted



        copyAndDeleteExternalLinks(issue,relevantIssuesToCopyLinksTo)


    }

    else if (issueTypeOfCurrentIssue == issueTypeStory){


        //first we trigger to copy the process for the related higher level issues
        //be aware not to change the order. It is necessary to trigger the methods in this sorting order


        for(Issue item: businessRequests){
            syncExternalLinks(item)
        }


        for(Issue item: pkes){
            syncExternalLinks(item)
        }


        //-----------


        for (Issue item : requirements){
            relevantIssuesToCopyLinksTo.add(item)
        }


        for (Issue item : testCases){
            relevantIssuesToCopyLinksTo.add(item)
        }

        for (Issue item : bugs){
            relevantIssuesToCopyLinksTo.add(item)
        }


        copyAndDeleteExternalLinks(issue,relevantIssuesToCopyLinksTo)




    }

    else if (issueTypeOfCurrentIssue == issueTypeRequirement){


        //and now we trigger to copy the process for the related higher level issues


        for(Issue item: stories){
            syncExternalLinks(item)
        }



        for (Issue item : testCases){
            relevantIssuesToCopyLinksTo.add(item)
        }

        for (Issue item : bugs){
            relevantIssuesToCopyLinksTo.add(item)
        }


        copyAndDeleteExternalLinks(issue,relevantIssuesToCopyLinksTo)





    }

    else if (issueTypeOfCurrentIssue == issueTypeTestCase){




        //and now we trigger to copy the process for the related higher level issues



        for(Issue item: requirements){
            syncExternalLinks(item)
        }

        for (Issue item : bugs){
            relevantIssuesToCopyLinksTo.add(item)
        }


        copyAndDeleteExternalLinks(issue,relevantIssuesToCopyLinksTo)
    }



    else {
        println "z"
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
        //we need this list later in order to determine if we have to trigger a deletion

        if (sourceLinks.size() != 0) {


            for (RemoteIssueLink item : sourceLinks){

                def Url = item.getUrl()

                sourceURLs.add(Url)
            }

        }



        for (Issue itemInList : issuesToCopyLinksTo){


            //get the first issue for which we want the link to be copied to
            def newIssue = getIssueByKey(itemInList.getKey())



            //get all remote links = external links for the target issue
            List targetLinks = remoteIssueLinkManager.getRemoteIssueLinksForIssue(newIssue)


            //get for the target issue all existing URLS of the existing external links
            List targetURLsWithOriginCurrentIssue = []

            List allTargetURLs = []

            //create a list of all available targetURLS with origin current issue.
            // all other external links we do not touch

            if (targetLinks.size() != 0) {


                for (RemoteIssueLink item : targetLinks){

                    def Url = item.getUrl()
                    def sourceID = item.getSummary()


                    //We  onl want to check links, which were created by this current issue
                    if(sourceID == currentIssue.getKey()){

                        targetURLsWithOriginCurrentIssue.add(Url)

                    }

                    allTargetURLs.add(Url)

                }

            }


            //create a list for only the links that do not exist in the target issue
            //For this we check if in the list of all URLs existing in the target issue,
            //the URLs of all available links in the source issue exist.
            List relevantLinks = []

            for (RemoteIssueLink item : sourceLinks){

                def found = allTargetURLs.find{it == item.getUrl()}

                if (found == null) {

                    relevantLinks.add(item)
                }

            }



            //create a list for those links, that have been deleted in the original issue (type story) and still exist
            //in the target issues. Remember, we want to have the links synchronized.


            List linksToBeDeletedInTargetIssue = []

            for (RemoteIssueLink item : targetLinks){

                //here we check if current issue was origin for the external link in target issue
                if (item.getSummary()==currentIssue.getKey()){

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
                linkBuilder.summary(currentIssue.getKey())



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

def setReleaseSprint(Issue issue, Helper hp){


    //begin customizing

        def customFieldNameRelease = ".Release"
        def customFieldNameSprint = ".Sprint"


    //end customizing


        def issueType = issue.getIssueTypeObject().getName()


        //copy to --> .Release
        hp.setLabelCustomField(issue,hp.getFirstReleaseName(issue),customFieldNameRelease)


        //copy to --> .Sprint
        hp.setLabelCustomField(issue,hp.getSprintName(issue),customFieldNameSprint)


}

def setReleaseSrintAlmSubjectforAllRequirementsAndTestCases(Issue issue,Helper hp){

    //begin customizing

    def customFieldNameDeveloper = ".Developer"
    def nameOfPrefix = "DEV"
    def issueTypeNameSubTasks = "Sub-task"
    def issueTypeNameStory = "Story"
    def issueTypeNameRequirement = "Requirement"
    def issueTypeNameTestCase = "Test Case"
    def issueTypeNameBug = "Bug"

    //end customizing

    //make sure we copy the .developer if this exists

    Set<Issue> issuesInNetwork = new HashSet<Issue>()
    List myTempListOfIssues = []

    //get the requirements
    myTempListOfSubTasks.addAll(hp.getIssuesOfNetworkByLinkType(issue, "1", "is validated by").getIssues())

    issuesInNetwork.addAll(myTempListOfSubTasks)


    //get the test cases for all requirements
    for(Issue item : issuesInNetwork){

        myTempListOfSubTasks = []

        myTempListOfSubTasks.addAll(hp.getIssuesOfNetworkByLinkType(item,"1","is tested by").getIssues())

        issuesInNetwork.addAll(myTempListOfSubTasks)

    }

    // for all found issues set now .release .sprint and .alm_subject

    for(Issue item : issuesInNetwork){

        setReleaseSprint(issue)

    }


}



def setDeveloperNameForIssue(Issue issue, Helper hp){

    //begin customizing

        def customFieldNameDeveloper = ".Developer"
        def nameOfPrefix = "DEV"
        def issueTypeNameSubTasks = "Sub-task"
        def issueTypeNameStory = "Story"
        def issueTypeNameRequirement = "Requirement"
        def issueTypeNameTestCase = "Test Case"
        def issueTypeNameBug = "Bug"

    //end customizing


    //make sure we copy the .developer if this exists

    Set<Issue> issuesInNetwork = new HashSet<Issue>()
    List myTempListOfSubTasks = []
    def nameDeveloper = ""


    //if a new requirement is created we need to get the developer name of the
    //related user story

    if(issue.getIssueTypeObject().getName() == issueTypeNameRequirement) {

        //get the name of the developer

        //get the user story based on link type "validates"
        myTempListOfSubTasks.addAll(hp.getIssuesOfNetworkByLinkType(issue, "1", "validates").getIssues())

        issuesInNetwork.addAll(myTempListOfSubTasks)

        //get the sub tasks
        for(Issue item : issuesInNetwork){

            if(item.getIssueTypeObject().getName() == issueTypeNameStory) {

                myTempListOfSubTasks = []

                myTempListOfSubTasks.addAll(hp.getIssuesOfNetworkByIssueTypeAndLinkType(item,issueTypeNameSubTasks,"1","").getIssues())

                //get the subtask beginning with DEV
                for(Issue subTask : myTempListOfSubTasks){

                    if (subTask.getSummary().substring(0,3)== nameOfPrefix) {

                        nameDeveloper = subTask.getAssignee().getName()

                    }
                }
            }



        }

        hp.setCustomFieldValueUserPicker(issue,nameDeveloper,hp.getCustomField(customFieldNameDeveloper))

    }





    // if a test case is created, then this test case has to be assigned the developer name

    if(issue.getIssueTypeObject().getName() == issueTypeNameTestCase) {

        //get the name of the developer


        Set<Issue> myListOfStories = new HashSet<Issue>()
        Set<Issue> myListOfRequirements = new HashSet<Issue>()


        //get the requirements which are tested by the the test case
        myListOfRequirements.addAll(hp.getIssuesOfNetworkByLinkType(issue, "1", "tests").getIssues())


        //get the stories for each requirement
        for(Issue item : myListOfRequirements){

            if(item.getIssueTypeObject().getName()== issueTypeNameRequirement){

                myListOfStories = []
                myListOfStories.addAll(hp.getIssuesOfNetworkByLinkType(item, "1", "validates").getIssues())

            }

        }


        //get the sub tasks for the user stories and the name of the developer assigned to subtask with prefix DEV
        for(Issue item : myListOfStories){

            if(item.getIssueTypeObject().getName() == issueTypeNameStory) {

                myTempListOfSubTasks = []

                myTempListOfSubTasks.addAll(hp.getIssuesOfNetworkByIssueTypeAndLinkType(item,issueTypeNameSubTasks,"1","").getIssues())

                //get the subtask beginning with DEV
                for(Issue item2 : myTempListOfSubTasks){

                    if (item2.getSummary().substring(0,3)== nameOfPrefix) {

                        nameDeveloper = item2.getAssignee().getName()

                    }
                }
            }



        }


        hp.setCustomFieldValueUserPicker(issue,nameDeveloper,hp.getCustomField(customFieldNameDeveloper))


    }


    //if a bug is created, then the name of the developer of the related story has to be assigned
    if(issue.getIssueTypeObject().getName() == issueTypeNameBug) {

        //get the name of the developer and add this to the Bug


        Set<Issue> myListOfStories = new HashSet<Issue>()
        Set<Issue> myListOfRequirements = new HashSet<Issue>()
        Set<Issue> myListOfTestCases = new HashSet<Issue>()



        //get the test cases
        myListOfTestCases.addAll(hp.getIssuesOfNetworkByLinkType(issue, "1", "blocks").getIssues())

        //get the requirements
        for(Issue item : myListOfTestCases) {
            if(item.getIssueTypeObject().getName() == issueTypeNameTestCase){

                myListOfRequirements.addAll(hp.getIssuesOfNetworkByLinkType(item, "1", "tests").getIssues())
            }

        }

        //get the stories for each requirement
        for(Issue item : myListOfRequirements){

            if(item.getIssueTypeObject().getName()== issueTypeNameRequirement){

                myListOfStories = []
                myListOfStories.addAll(hp.getIssuesOfNetworkByLinkType(item, "1", "validates").getIssues())

            }

        }


        //get the sub tasks for the user stories and the name of the developer assigned to subtask with prefix DEV
        for(Issue item : myListOfStories){

            if(item.getIssueTypeObject().getName() == issueTypeNameStory) {

                myTempListOfSubTasks = []

                myTempListOfSubTasks.addAll(hp.getIssuesOfNetworkByIssueTypeAndLinkType(item,issueTypeNameSubTasks,"1","").getIssues())

                //get the subtask beginning with DEV
                for(Issue item2 : myTempListOfSubTasks){

                    if (item2.getSummary().substring(0,3)== nameOfPrefix) {

                        nameDeveloper = item2.getAssignee().getName()

                    }
                }
            }



        }

        hp.setCustomFieldValueUserPicker(issue,nameDeveloper,hp.getCustomField(customFieldNameDeveloper))



    }
}

def setWorkflowStatusForRequirement(Issue issue, Helper hp, String environment){


    //.TestStatus comes fromHP-ALM
    def customfieldNameTestStatus = ".TestStatus"

    // set workflow transition
    def testStatus = hp.getCustomFieldValue(issue,customfieldNameTestStatus)

    //set workflowtransition using the transition id
    //in Progress = 21
    //To Do = 11
    //DONE = 31


    if(environment == "PROD"){



            if(testStatus == "Failed"){
                hp.setWorkflowTransition(issue,11)
            }

            else if(testStatus == "Blocked"){
                hp.setWorkflowTransition(issue,21)
            }

            else if(testStatus == "No Run"){
                hp.setWorkflowTransition(issue,31)
            }

            else if(testStatus == "Not Completed"){
                hp.setWorkflowTransition(issue,41)
            }

            else if(testStatus == "Not Covered"){
                hp.setWorkflowTransition(issue,51)
            }

            else if(testStatus == "Passed"){
                hp.setWorkflowTransition(issue,61)
            }

            else if(testStatus == "Flagged for deletion"){
                hp.setWorkflowTransition(issue,71)
            }

    }

    if(environment == "DEV"){



        if(testStatus == "Failed"){
            hp.setWorkflowTransition(issue,11)
        }

        else if(testStatus == "Blocked"){
            hp.setWorkflowTransition(issue,21)
        }

        else if(testStatus == "No Run"){
            hp.setWorkflowTransition(issue,31)
        }

        else if(testStatus == "Not Completed"){
            hp.setWorkflowTransition(issue,41)
        }

        else if(testStatus == "Not Covered"){
            hp.setWorkflowTransition(issue,51)
        }

        else if(testStatus == "Passed"){
            hp.setWorkflowTransition(issue,61)
        }

        else if(testStatus == "Flagged for deletion"){
            hp.setWorkflowTransition(issue,71)
        }

    }
}



def linkIssues(Issue issue, Helper hp, String environment){

    //Beginn customizing
    def customFieldNameRequirementID = ".Requirement-ID"
    //End customizing

    def issueIDsToLink = hp.getCustomFieldValue(issue,customFieldNameRequirementID)

    def issueIDs = []

    issueIDs = hp.retrieveTokens(issueIDsToLink,",")


    for(String  issueKey : issueIDs){

        hp.linkIssue(issue,hp.getIssueByKey(issueKey),"Tests",environment)

    }
}


def main(Issue issue, Category log, Helper hp, String environment){



    log.info("Entering handleIssueUpdateAndAssignEvents() ")
    //hp.showLinkTypes(log)


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
    def listOfFieldNames = ["description","Component", "Fix Version", "Sprint", "assignee",".IT-App_Module",".TestStatus",".Release",".Sprint"]
    def searchResult
    def field


    //only true if we have an update
    if(issue.created != issue.updated){

        //just relevant for testing purposes in order to check the name of JIRA-fields
        def test = event.getChangeLog().getRelated('ChildChangeItem')


        // we loop over all field names, for which we want to check if an update has happened.
        for (item in listOfFieldNames) {

           // def test = event.getChangeLog().getRelated('ChildChangeItem')

            log.debug("Entering event.getChangeLog().getRelated('ChildChangeItem') and looking for " + item)

            def check = event.getChangeLog().getRelated('ChildChangeItem').find { it.field == item }


            //is null if not update was found for the field
            if(check != null) {

                searchResult = check
                field = searchResult.field



                log.debug("Found a change in field " + field +" containing following data: "+ searchResult)

                break

            }

            log.debug("No update found for field " + item)
        }


        //make sure, that all the subtasks assigned to a story allways have the same components assigned to them.
        //We need this functionality as the relationshipt between story and subtas is not a "relates to" relationship.
        //Therefore we can not use the plugin (Exocert" functionality to copy the value along the "relates to" relationship.
        if (searchResult != null && field == "Component") {


            //get all components for the issue
            Collection<ProjectComponent> myComponents = issue.getComponentObjects()

            // only a story has subtasks
            if (issueType == issueTypeNameStory) {

                // get all SubTasks for the story

                def subTasks = hp.getIssuesOfNetworkByIssueTypeAndLinkType(issue,issueTypeNameSubTasks,"1","").getIssues()

                //we need the IssueManager in order to create the issue of the the result of the query
                IssueManager issueManager = ComponentAccessor.getIssueManager()

                //every subtask is updated will all components of the story
                subTasks.each {

                    //we create an issue
                    def myIssue = issueManager.getIssueObject(it.getId())

                    hp.updateComponents(myIssue,myComponents)

                }


            }


        }//end of handling of components


        //if the field "fix version" or sprint was updated then, we copy the value to the defined customfields

        if (searchResult != null && field == "Fix Version" || field == "Sprint"){


            if(issue.getIssueTypeObject().getName() == issueTypeNameStory){

                //retrieve sprint and fixversion and copy the values to the customfields .Release and .Alm_Subject and .Sprint
                setReleaseSprint(issue,hp)

                //if the release is changed but the sprint remains - which should not really be the case
                //then we must make sure, that this change is also available for the relevant business requests

                hp.setReleaseAndSprintNamesInBusinessRequest(issue,customFieldNameSprintAndReleaseNames)

                hp.setReleaseAndSprintNamesInPKE(issue,customFieldNameSprintAndReleaseNames)


            }




        }

        if (searchResult != null && field == ".Release" || field == ".Sprint" ){

            if(issueType == issueTypeNameTestCase){


                //determine the origin of the testcase. We differ HP-ALM and JIRA as source for test cases.
                //This value is set via Exocert plugin or via Tasktop Sync
                def origin = hp.getCustomFieldValue(issue,customfieldNameTestCaseOrigin)


                //True for testcase created in Jira
                if (origin ==  constantJIRA){

                    //handling of  .ALM_subject
                    //ALM subject is where the testcase is stored within HP ALM
                    def storyKey = hp.getCustomFieldValue(issue,customFieldNameStoryID)

                    def almsubject = hp.getAlmSubject(hp.getIssueByKey(storyKey))

                    // set .ALM_subject
                    hp.setLabelCustomField(issue,almsubject,customFieldNameAlmSubject)
                }

            }



        }

        if (searchResult != null && field == "description"){


            //TODO refactoring necessary
            syncExternalLinks(issue)

        }


        if (searchResult != null && field == ".TestStatus"){

            //set the workflow status based on customfield  .TestStatus
            setWorkflowStatusForRequirement(issue,hp)

        }



        if (searchResult != null && field == "assignee") {

            def issueSummary = issue.getSummary()
            //we get the first 3 characters of the summary in order the check if it is a DEV task
            def keyWord = issueSummary.substring(0,3)




            //we only copy the name of the developer if the subtasks begins witn prefix DEV
            //set developer name for subTask, story, requirements and test cases

            if(issueType == issueTypeNameSubTasks && keyWord == nameOfPrefix){

                def newAssignee = searchResult.newstring
                def userName



                // set for this issue of type sub task the customfield .Developer t

                if(newAssignee != null) {

                    userName = hp.getAssigneeUserName(issue)

                }

                else {
                    userName = ""
                }



                //set for the parent issue of type story the customfield .Developer
                //setLabel(issue,newAssignee,customFieldNameDeveloper,log)

                hp.setCustomFieldValueUserPicker(issue,userName,hp.getCustomField(customFieldNameDeveloper))


                //get the user story based on link type "validates" and set .developername
                def  myTempListOfStories =[]
                myTempListOfStories.addAll(hp.getIssuesOfNetworkByIssueTypeAndLinkType(issue,"Story","1", "").getIssues())

                hp.setCustomFieldValueUserPicker(myTempListOfStories.get(0),userName,hp.getCustomField(customFieldNameDeveloper))



                //now we want to copy the name of the developer to all to the story related requirements and test cases and bugs

                for(Issue issue1 : hp.getAllRequirementTestCasesBugForStory(myTempListOfStories[0])){

                   hp.setCustomFieldValueUserPicker(issue1,userName,hp.getCustomField(customFieldNameDeveloper))



                }

            }


        }



        //if the field "fix version" or sprint was updated then, we copy the value to the defined customfields

        if (searchResult != null && field == ".IT-App_Module"){


            if(issueType == issueTypeNameTestCase){

                //determine the origin of the testcase. We differ HP-ALM and JIRA as source for test cases.
                //This value is set via Exocert plugin or via Tasktop Sync
                def origin = hp.getCustomFieldValue(issue,customfieldNameTestCaseOrigin)


                //True for testcase created in Jira
                if (origin ==  constantJIRA){

                    //handling of  .ALM_subject
                    //ALM subject is where the testcase is stored within HP ALM
                    def storyKey = hp.getCustomFieldValue(issue,customFieldNameStoryID)

                    def almsubject = hp.getAlmSubject(hp.getIssueByKey(storyKey))

                    // set .ALM_subject
                    hp.setLabelCustomField(issue,almsubject,customFieldNameAlmSubject)
                }

            }



        }


        if(issue.getIssueTypeObject().getName() == issueTypeNameBusinessRequest){

            hp.setReleaseAndSprintNamesInBusinessRequest(issue,customFieldNameSprintAndReleaseNames)

        }
    }





    //we handle here the issue created event
    // issue.created = issue.updated
    else {

        if(issue.getIssueTypeObject().getName() == issueTypeNameStory){

            setReleaseSprint(issue,hp)

        }





        if(issue.getIssueTypeObject().getName() == issueTypeNameRequirement){


            //set the workflow status based on customfield  .TestStatus
            setWorkflowStatusForRequirement(issue,hp,environment)

        }


        if(issue.getIssueTypeObject().getName() == issueTypeNameTestCase){

            //determine the origin of the testcase. We differ HP-ALM and JIRA as source for test cases.
            //This value is set via Exocert plugin or via Tasktop Sync
            def origin = hp.getCustomFieldValue(issue,customfieldNameTestCaseOrigin)


            //True for testcase created in Jira
            if (origin ==  constantJIRA){

                //handling of  .ALM_subject
                //ALM subject is where the testcase is stored within HP ALM
                def storyKey = hp.getCustomFieldValue(issue,customFieldNameStoryID)

                def almsubject = hp.getAlmSubject(hp.getIssueByKey(storyKey))

                // set .ALM_subject
                hp.setLabelCustomField(issue,almsubject,customFieldNameAlmSubject)
            }


            //True for testcase created in HP-ALM
            if (origin == constantHPALM){

                //link all requirements as defined in HP-ALM via UploadExcel
                linkIssues(issue,hp,environment)



                //set .Release and .Sprint
                def story = hp.getStoryFromTestcase(issue,log)

                hp.setLabelCustomField(issue,hp.removeFirstAndLastCharacterFromString(hp.getCustomFieldValue(story,customFieldNameRelease)),customFieldNameRelease)

                hp.setLabelCustomField(issue,hp.removeFirstAndLastCharacterFromString(hp.getCustomFieldValue(story,customFieldNameSprint)),customFieldNameSprint)

                // set .ALM_subject
                // use the value with origin HP
                hp.setLabelCustomField(issue, hp.getCustomFieldValue(issue,customfieldNameAlmSubjectHP),customFieldNameAlmSubject)

                //retrieve and set .ITApp_Module
                hp.setLabelCustomField(issue,hp.removeFirstAndLastCharacterFromString(hp.getCustomFieldValue(story,customFieldNameITApp_Module)),customFieldNameITApp_Module)

                //retrieve and set .Story-ID
                hp.setCustomFieldValue(issue,story.getKey(),hp.getCustomField(customFieldNameStoryID))


            }
        }

                //set the .DeveloperName as assigned to subtask with prefix DEV
            if(issue.getIssueTypeObject().getName() == issueTypeNameRequirement || issueTypeNameTestCase || issueTypeNameBug){

                 setDeveloperNameForIssue(issue,hp)

            }



        if(issue.getIssueTypeObject().getName() == issueTypeNameBusinessRequest){

            hp.setReleaseAndSprintNamesInBusinessRequest(issue,customFieldNameSprintAndReleaseNames)

        }

    }




}


// READ ME:

//The development is based on JIRA 6.4.8.
//In case of an upgrade the script must also be upgraded. It is expected, that the code must be changed, as some methods do not exist anymore



def Category log = Category.getInstance("com.onresolve.jira.groovy")

log.setLevel(org.apache.log4j.Level.OFF)

def constDEV = "DEV"
def constPROD = "PROD"

hp = new Helper()

main(getCurrentIssue("EV"),log,hp,constDEV)








