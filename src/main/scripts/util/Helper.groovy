package util

import com.atlassian.greenhopper.service.sprint.Sprint
import com.atlassian.jira.bc.issue.link.DefaultRemoteIssueLinkService
import com.atlassian.jira.bc.project.component.ProjectComponent
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.issue.DocumentIssueImpl
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.issue.ModifiedValue
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.customfields.manager.OptionsManager
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem
import com.atlassian.jira.issue.issuetype.IssueType
import com.atlassian.jira.issue.label.LabelManager
import com.atlassian.jira.issue.link.DefaultRemoteIssueLinkManager
import com.atlassian.jira.issue.link.IssueLinkType
import com.atlassian.jira.issue.link.IssueLinkTypeManager
import com.atlassian.jira.issue.link.RemoteIssueLink
import com.atlassian.jira.issue.link.RemoteIssueLinkBuilder
import com.atlassian.jira.issue.search.SearchProvider
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.user.util.UserUtil
import com.atlassian.jira.util.JiraUtils
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.crowd.embedded.api.User
import com.atlassian.jira.workflow.WorkflowTransitionUtil
import com.atlassian.jira.workflow.WorkflowTransitionUtilImpl
import com.opensymphony.workflow.WorkflowContext
import com.atlassian.jira.project.Project
import org.apache.log4j.Category

import java.sql.Connection
import java.sql.Driver
import java.sql.PreparedStatement
import java.sql.ResultSet


/**
 * Created by roland on 16.02.16.
 */

class Helper {



//method retrieves the current application user

    Helper() {
    }

    def ApplicationUser getCurrentApplicationUser() {

        def jac

        //determine current user

        //Security
        jac = ComponentAccessor.getJiraAuthenticationContext()

        def currentUser

        currentUser = jac.getUser()

        return currentUser
    }


    def User getCurrentUserWF() {

        try {
            String currentUser = ((WorkflowContext) transientVars.get("context")).getCaller()

            //Security
            UserUtil userUtil = ComponentAccessor.getUserUtil()

            //Use getUserByKey(String) or getUserByName(String) instead. Since v6.0.
            User user = userUtil.getUser(currentUser)
        }

        catch (all){

        }




    }


    def User getCurrentUser() {

        def jac
        def currentUser

        //Security
        jac = ComponentAccessor.getJiraAuthenticationContext()

        currentUser = jac.getUser().getDirectoryUser()

        return currentUser
    }


    def User getUserbyName(String nameOfUser){

        def jac
        def user

        //Security
        UserUtil userUtil = ComponentAccessor.getUserUtil()



        jac = ComponentAccessor.getJiraAuthenticationContext()

        ApplicationUser applicationUser = userUtil.getUserByName(nameOfUser)

        user = applicationUser.getDirectoryUser()
    }

//this method creates a comment
    def addComment(Issue issue, String myComment) {

        def cmm

        cmm = ComponentAccessor.getCommentManager()

        //cmm.create(issue,getCurrentApplicationUser(),myComment,true)
        cmm.create(issue,getCurrentApplicationUser(),myComment,true)


    }


//this method gets the value of a customfield value by its name
    def getCustomFieldValue(Issue issue, String myCustomField) {

        def cfm
        def value

        cfm = ComponentAccessor.getCustomFieldManager()

        CustomField customField = cfm.getCustomFieldObjectByName(myCustomField);
        value = customField.getValue(issue).toString()

        if (value == null) {
            value = ""
        }

        return value

    }




// this method returns a customfield
    def getCustomField(String myCustomFieldName) {

        def cfm

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


    //delivers the Jira-Project based on ProjectKey
    def Project getProject(String projectKey){

        def pjm  // project mngr
        def project

        try {
            pjm  = ComponentAccessor.getProjectManager()

            project = pjm.getProjectByCurrentKey(projectKey) // get the relevant project
        }

        catch (all){

        }



        return project


    }


    //gets a specific IssueType
    def IssueType getIssueType(String issueType, Project project){

        def IssueType myIssueType

        def itsm //IssueTypeSchemeMngr


        try {

            itsm = ComponentAccessor.getIssueTypeSchemeManager()

            Collection<IssueType> issueTypes = itsm.getIssueTypesForProject(project)

            for(IssueType type : issueTypes){

                def name = type.getName() //retrieves the name of the IssueType

                if(type.getName() == issueType){

                    myIssueType = type

                    break
                }

            }

        }

        catch (all){

        }

        return myIssueType

    }



    def createIssue(String projectKey, String issueType, String summary, String description, String reporter, User currentUser){

        def project //Jira-Project
        def isf  // issue factory
        def ism  //issueManager
        def issue // Issue

        try {

            //Instanzierung der factorie
            isf = ComponentAccessor.getIssueFactory()


            //IssueFactory: we create here a generic issue
            def mutableIssue = isf.getIssue()




            //configure the issue

                //set the project for the issue

                project = getProject(projectKey) // get the relevant project

                mutableIssue.setProjectObject(project) // assign the project to the issue

                mutableIssue.setIssueTypeObject(getIssueType(issueType,project)) //get and set IssueType

                //set the summary
                mutableIssue.setSummary(summary)

                //set the description
                mutableIssue.setDescription(description)

                //set reporter
                mutableIssue.setReporter(currentUser)


            //the issue gets created with the IssueMngr


            ism = ComponentAccessor.getIssueManager()

            issue = ism.createIssueObject(currentUser, mutableIssue)

            return issue


        }

        catch (all) {

        }



    }

    //gets a database connection
    def Connection getDatabaseConnection(String databaseProduct,String databaseHost, String database, String user, String passwd){

        def driver
        def databaseClassName // classname for JDBC driver
        def props //database properties
        def Connection conn //database connection



        //properties in order to connect to the database
        props = new Properties()
        props.setProperty("user", user)
        props.setProperty("password", passwd)

        if(databaseProduct == "mysql"){
            databaseClassName  = "com.mysql.jdbc.Driver"
            driver = Class.forName(databaseClassName).newInstance() as Driver
            conn = driver.connect("jdbc:mysql://"+databaseHost+"/"+database, props)
            //conn = driver.connect("jdbc:mysql://127.0.0.1:3306/sakila",props)
        }

        if(database == "oracle"){
            databaseClassName  = "oracle.jdbc.OracleDriver"
            driver = Class.forName(databaseClassName).newInstance() as Driver
        }



        return conn


    }


    //executes a query and delivers the result in a ResultSet
    def ResultSet executeSqlQuery (String sqlQuery,Connection connection){


        def PreparedStatement pstmt
        def ResultSet rs = null

        pstmt = connection.prepareCall(sqlQuery)


        try {
            rs = pstmt.executeQuery()

            }

        catch (all) {

        }

        //connection.close()

        return rs

    }



//this method is responsible for the creation of subTask
    def addSubTask(Issue issue, String subTaskType, String subTaskName, String subTaskSummary, String subTaskDescription,String environment,Issue linktToIssue) {

        //we have two kind of subtasks available real subtasks and orderItems

        def issueTypeIdForSubTask

        //DEV
        def story_DEV = "10001"
        //requirement 10102
        //pke 10101
        //test case 10103
        //business request 10100
        //bug 1
        def subtask_DEV = "5"
        //order 10201
        def orderitem_DEV = "10200"






        //define the issuetype for the object to be created

        if(subTaskType == "Sub-task" && environment == "DEV"){
             issueTypeIdForSubTask = "5"
        }

        else if(subTaskType =="OrderItem" && environment =="DEV"){

            issueTypeIdForSubTask = "10200"
        }

        else if(subTaskType == "Sub-task" && environment == "PROD"){
            issueTypeIdForSubTask = "5"
        }

        else if(subTaskType =="OrderItem" && environment =="PROD"){

            issueTypeIdForSubTask = "10200"
        }



        //Instanzierung der factories
        def isf = ComponentAccessor.getIssueFactory()

        //IssueFactory: we create her a generic issue
        def issueObject = isf.getIssue()

        //assign the project of the parent to the new issue
        issueObject.setProjectObject(issue.getProjectObject())


        //set the issuetype for the created issue
        issueObject.setIssueTypeId(issueTypeIdForSubTask)

        //set the parentID
        issueObject.setParentId(issue.getId())


        // set summary of subTask
        if(subTaskSummary == "") {
            issueObject.setSummary(subTaskName + ': user story ' + issue.getSummary())
        }

        else {
            issueObject.setSummary(subTaskName + ": " + subTaskSummary)
        }


        //set the assignee of parent
        issueObject.setAssignee(issue.getAssignee())

        issueObject.setDescription(subTaskDescription)

        //set the reporter of parent
        issueObject.setReporter(issue.getReporter())



        //here we check if the value for the summary of a subtasks has already been used. We do not want to have
        //two subtasks with the same value.
        def toBeCreatedSubTaskSummary

        if(subTaskSummary == ""){
            toBeCreatedSubTaskSummary = subTaskName + ': user story ' + issue.getSummary()
        }

        else {
            toBeCreatedSubTaskSummary = subTaskName + ": " + subTaskSummary
        }


        def checkResult = checkIfSubTaskSummaryExists(issue,toBeCreatedSubTaskSummary)


        // we only create our new SubTask if the the value of summary does not exist in any already defined subtask
        if (!checkResult) {

            //the issue gets created with the IssueMngr
            def ism = ComponentAccessor.getIssueManager()

            //Security
            //jac = ComponentAccessor.getJiraAuthenticationContext()

            def currentUser = getCurrentUser()

            def subTask = ism.createIssueObject(currentUser, issueObject)




            //the created subtask is linked to the issue.This is done through the SubTaskMngr
            def stm = ComponentAccessor.getSubTaskManager()
            stm.createSubTaskIssueLink(issue, subTask, currentUser)


            //create additional links
            addAndRemoveLinksToIssue(subTask,linktToIssue,"Relates",environment)


            // disable the watcher using the WatcherManager
            def wtm = ComponentAccessor.getWatcherManager()
            wtm.stopWatching(currentUser, subTask)


        }

    }






    //Method retrieves the Fixed Version name of the current issue
    //If no release is assigned, then "-" will be set.
    def getFirstReleaseName(Issue issue){

        // MutableIssue myMutableIssue = (MutableIssue)issue;// Flag EV is necessary to be able to be triggered by an event / listener

        ArrayList myListReleases = (ArrayList)issue.getFixVersions()

        def releaseName

        if(myListReleases.size() != 0){
            

            //we only consider getting the first item, even though more fix versions can be assigned to an issue
            releaseName = (String)myListReleases[0]

        }
        
        else {
            releaseName = "-"
        }


        return releaseName
    }

    // method retrieves the assigned sprint of an issue
    //If no sprint is assigned, then "-" will be set.
    def getSprintName(Issue issue){

        ArrayList<Sprint> listOfSprints = (ArrayList<Sprint>) ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Sprint").getValue(issue);

        def sprintName

        if(listOfSprints != null){

            //we only consider getting the first sprint in the list, event though more sprints can be assigned to an issue
            sprintName = (String)listOfSprints[0].getName()
        }

        else {
            
            sprintName = "-"

        }

        return sprintName
    }

    def setCustomFieldValue(Issue issue, Object myValueToSave, CustomField myCustomField){


        def myIssue = issue

        if(issue instanceof DocumentIssueImpl){
            myIssue = getIssueByKey(issue.getKey())
        }

        def MutableIssue myMutableIssue = (MutableIssue)myIssue


        myMutableIssue.setCustomFieldValue(myCustomField,myValueToSave)



        Map<String,ModifiedValue> modifiedFields = myMutableIssue.getModifiedFields()

        FieldLayoutItem myFieldLayoutItem = ComponentAccessor.getFieldLayoutManager().getFieldLayout(myMutableIssue).getFieldLayoutItem(myCustomField)

        DefaultIssueChangeHolder myDefaultIssueChangeHolder = new DefaultIssueChangeHolder()

        final ModifiedValue myModifiedValue = modifiedFields.get(myCustomField.getId())

        myCustomField.updateValue(myFieldLayoutItem,myMutableIssue,myModifiedValue,myDefaultIssueChangeHolder)


    }


    def setCustomFieldValueUserPicker(Issue issue, String userName, CustomField myCustomField){

        def myIssue = issue

        if(issue instanceof DocumentIssueImpl){
            myIssue = getIssueByKey(issue.getKey())
        }

        def MutableIssue myMutableIssue = (MutableIssue)myIssue

        UserUtil userUtil = ComponentAccessor.getUserUtil()
        ApplicationUser applicationUser = userUtil.getUserByName(userName)


        myMutableIssue.setCustomFieldValue(myCustomField,applicationUser)


        Map<String,ModifiedValue> modifiedFields = myMutableIssue.getModifiedFields()

        FieldLayoutItem myFieldLayoutItem = ComponentAccessor.getFieldLayoutManager().getFieldLayout(myMutableIssue).getFieldLayoutItem(myCustomField)

        DefaultIssueChangeHolder myDefaultIssueChangeHolder = new DefaultIssueChangeHolder()

        final ModifiedValue myModifiedValue = modifiedFields.get(myCustomField.getId())


        myCustomField.updateValue(myFieldLayoutItem,myMutableIssue,myModifiedValue,myDefaultIssueChangeHolder)

    }


    // sets n labels for a specific issue for a customfield

    def setLabelsForCustomField(Issue issue, Set labels, String customFieldName){

        def customFieldManager

        LabelManager labelManager = ComponentAccessor.getComponent(LabelManager.class)

        customFieldManager = ComponentAccessor.getCustomFieldManager()

        CustomField customField = customFieldManager.getCustomFieldObjectByName(customFieldName)

        labelManager.setLabels(getCurrentApplicationUser().getDirectoryUser(),issue.getId(),customField.getIdAsLong(),labels,false,true)

    }


    //sets the label for a customfield
    def setLabelCustomField(Issue issue, String newValue, String customFieldName){
        
        //a label can not be set with blank, therefore we replace a blank with a "-"
        if (newValue == ""){ 
            newValue ="-"
        }

        //we should always have a value for a label!
        if(newValue !=""){

            def customFieldManager
            def labelManager
            def customField

            labelManager = ComponentAccessor.getComponent(LabelManager.class)

            customFieldManager = ComponentAccessor.getCustomFieldManager()

            customField = customFieldManager.getCustomFieldObjectByName(customFieldName)

            
            //convert blanks in String to "_"
            
            Set<String> set = replaceBlankInStringWithUnderscore((String) newValue)



            labelManager.setLabels(getCurrentApplicationUser().getDirectoryUser(),issue.getId(),customField.getIdAsLong(),set,false,true)


        }

    }


    def deleteLabelCustomField(String customFieldName){

        def customFieldManager

        LabelManager labelManager = ComponentAccessor.getComponent(LabelManager.class)

        customFieldManager = ComponentAccessor.getCustomFieldManager()

        CustomField customField = customFieldManager.getCustomFieldObjectByName(customFieldName)

        labelManager.removeLabelsForCustomField(customField.getIdAsLong())



    }



// sets n labels in the standard JIRA labels field for a specific issue
    def setLabelJiraField(Issue issue, Set labels){

        def issueManager = ComponentAccessor.getIssueManager()
        def MutableIssue mutableIssue = issue
        def labelsWithoutBlanks = removeBlanksFromLabels(labels)

        mutableIssue.setLabels(labelsWithoutBlanks)
        issueManager.updateIssue(getCurrentUser(),mutableIssue,EventDispatchOption.DO_NOT_DISPATCH, false)

    }




//replaces all blanks with _  . This is necessary as labels are not allowed to contain a blank.
    def removeBlanksFromLabels(Set labels){

        Set<String> set = new HashSet<String>()


        for(String item : labels){



            StringTokenizer st = new StringTokenizer(item," ")

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

        }





        return set

    }

    //this methods replaces blanks in a string with "_"
    // Example: "this is my string" --> "this_is_my_string"
    // We need this if we want to insert a String to a label in JIRA
    def replaceBlankInStringWithUnderscore(String newValue){

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


    def retrieveTokens(String newValue,String delimitor){


        def newValueWithoutBlanks = removeBlanksFromString(newValue)


        Set<String> set = new HashSet<String>()

        StringTokenizer st = new StringTokenizer(newValueWithoutBlanks,delimitor)

        def myValue


        while(st.hasMoreTokens()) {

                myValue = st.nextToken()


                myValue.trim()   //removes all whitespaces and non-visible characters (e.g., tab, \n).

                set.add(myValue)
        }




        return set

    }


    /*
    remove blanks from a String
     */

    def removeBlanksFromString(String myString){

        def withoutspaces = ""


        for (int i = 0; i < myString.length(); i++) {
            if (myString.charAt(i) != ' ')
                withoutspaces += myString.charAt(i);

        }

        return withoutspaces
    }


    /*
    get all components for an issue
     */
    def getAllComponentsForIssue(Issue issue){


        def Collection<ProjectComponent> myComponents

        myComponents = issue.getComponentObjects() //get all components for the issue

        return myComponents


    }

// This method retrieves the issue based on its key
    def Issue getIssueByKey(String myIssueKey){


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

//get Object User for assignee of issue

    def getAssigneeUserName(Issue myIssue){

        def MutableIssue myMutableIssue = (MutableIssue)myIssue

        def userName = myMutableIssue.getAssignee().getName()

        return userName
    }




    def getTodaysDate(){
        def today = new Date()
        return today.toString()
    }


/*
Get the assignee of the subtask for the defined story starting with Prefix DEV
 */

    def getDevelopmentSubtaskAssigneeForStory(Issue issueStory){

        def devSubtaskAssignee
        def Issue developmentSubtaskIssue

        //get the development Subtask based on prefix "DEV"
        developmentSubtaskIssue = getDevSubTaskForStory(issueStory)

        devSubtaskAssignee = getAssigneeUserName(developmentSubtaskIssue)

        return devSubtaskAssignee
    }

/*
Get the subtask to a story with prefix DEV
 */

   def getDevSubTaskForStory(Issue issueStory){

       def subtasksForStoryList = []
       def Issue developmentSubTask

       subtasksForStoryList= getAllSubTasksForStory(issueStory) // get all subtasks for the story

       if(subtasksForStoryList.size() != 0){


               for (Issue issue : subtasksForStoryList){
                   if (issue.summary.substring(0,3) == "DEV"){  // only the subtask with prefix DEV is the relevant one.
                       developmentSubTask = issue
                   }
               }
       }

       return developmentSubTask
   }


    def getSprintAndReleaseName(Issue issue){

        //sprint name is "-" if not assigned to issue
        def sprint = getSprintName(issue)

        //release name is "-" if not assigned to issue
        def release = getFirstReleaseName(issue)

        def sprintName

        //sprint and release are assigned to issue
        if(sprint != "-" && release != "-"){

            sprintName = release + "_" + sprint
        }

        //only sprint assigned
        if(sprint != "-" && release == "-") {

            sprintName = "---_"+sprint
        }

        //only release assigned
        if(sprint == "-" && release != "-") {

            sprintName = release + "_"+"---"
        }

        //neither release and sprint is assigned
        if(sprint == "-" && release == "-") {

            sprintName = "---_"+"---"
        }

        // get rid of the blanks

        StringTokenizer st = new StringTokenizer(sprintName," ")

        String myValue = ""


        while(st.hasMoreTokens()) {
            if(myValue == ""){

                myValue=myValue+st.nextToken()
            }

            else {
                myValue=myValue + "_" + st.nextToken()
            }


        }

        sprintName = myValue

        return sprintName
    }




//removes first and last character from a String
    def removeFirstAndLastCharacterFromString(String myString){



        if (myString != ""){
            //remove last character
            myString = myString.substring(0, myString.length() - 1)

            //remove first character
            myString = myString.substring(1)
        }



        return myString
    }

    //The Subject is necessary to tell Tasktop Sync where (in the ALM testplan tree) to store the test cases
    //Example: EAP_R16.2JUL_SPRINT_1_TEAM_A

    def getAlmSubject(Issue story){



        def sprintName = getSprintName(story)

        def releaseName = getFirstReleaseName(story)

        def application_module = getCustomFieldValue(story,".IT-App_Module")



        //we only have to remove the brackets if a value in the field ".IT-App_Module" is found
        if(application_module != null && application_module != "") {

            //Unfortunately the value from a customfield is within []
            //Therefore these two brackets have to be removed
            application_module = removeFirstAndLastCharacterFromString(application_module)

        }


        def almSubject = ""

        //release and sprint are assigned to the issue
        if(sprintName != "-" && releaseName != "-"){

            if(application_module != null && application_module !=""){
                almSubject =  application_module + "_" + releaseName + "_" + sprintName
            }

            else {

                almSubject = "---_" + releaseName + "_" + sprintName
            }
        }


        //only sprint is assigned to the issue
        if(sprintName != "-" && releaseName == "-") {

            if(application_module != null && application_module !="") {
                almSubject = application_module + "_---_" +  sprintName
            }
            else {
                almSubject = "---_---_"+sprintName
            }

        }
        //only release is assigned to the issue
        if(sprintName == "-" && releaseName != "-") {

            if(application_module != null && application_module !="") {
                almSubject = application_module + "_" + releaseName +"_---"
            }
            else {
                almSubject = "---_" + releaseName + "_---"
            }

        }
        //neither release or sprint is assigned to the issue
        if(sprintName == "-" && releaseName == "-") {

            if(application_module != null && application_module !="") {
                almSubject = application_module+"_---_---"
            }
            else {
                almSubject = "---_---_---"
            }

        }



        // If releaseName or sprintName should have any blanks we have to get rid of those
        // remember, we need to do this, as no blanks are allowed for jira labels.

        StringTokenizer st = new StringTokenizer(almSubject," ")

        String myValue = ""


        while(st.hasMoreTokens()) {
            if(myValue == ""){

                myValue=myValue+st.nextToken()
            }

            else {
                myValue=myValue + "_" + st.nextToken()
            }


        }

        almSubject = myValue

        return almSubject
    }


    // returns one or more issues based on the defined query.
    def getIssuesByQuery(String myQuery){

        def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser)
        def searchProvider = ComponentAccessor.getComponent(SearchProvider)
        def user = getCurrentApplicationUser()
        def parsedQuery


        parsedQuery = jqlQueryParser.parseQuery(myQuery)

        def issues = searchProvider.search(parsedQuery, user, PagerFilter.getUnlimitedFilter())

        return issues

    }


    def getIssuesOfNetworkByLinkType(Issue issue, String traversalDepth, String linkType){

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

        else if(linkType=="validates"){

            query = jqlQueryParser.parseQuery("issueFunction in linkedIssuesOfRecursiveLimited(\"issue =" + issueId + "\"," + traversalDepth + ",\"validates\")  ORDER BY issuetype DESC")

        }


        else {
            query = jqlQueryParser.parseQuery("issueFunction in linkedIssuesOfRecursiveLimited(\"issue =" + issueId + "\"," + traversalDepth + ",\"" + linkType + "\")  ORDER BY issuetype DESC")
        }

        def issues = searchProvider.search(query, user, PagerFilter.getUnlimitedFilter())

        return issues


    }





    def getIssuesOfNetworkByIssueTypeAndLinkType(Issue issue, String issueType, String traversalDepth, String linkType){

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

// query with consideration of linkType and issuetype
        else {
            query = jqlQueryParser.parseQuery("issueFunction in linkedIssuesOfRecursiveLimited(\"issue =" + issueId + "\"," + traversalDepth + ",\"" + linkType + "\") AND issuetype =" + issueType + "  ORDER BY issuetype DESC")
        }

        def issues = searchProvider.search(query, user, PagerFilter.getUnlimitedFilter())


        return issues


    }


    def setReleaseAndSprintNamesInPKE(Issue issue,String customFieldName){

        //start customizing

        def issueTypeStory = "Story"
        def issueTypePKE = "PKE"
        def linkTypeRelatesTo = "relates to"

        //end customizing


        //we need the IssueManager in order to create the issue of the the result of the query
        IssueManager issueManager = ComponentAccessor.getIssueManager()

        //get all stories linked by relates to
        def stories = getIssuesOfNetworkByIssueTypeAndLinkType(issue,issueTypeStory,"3",linkTypeRelatesTo).getIssues()

        //get the pke. We should only have one PKE linked to the stories
        def pkes = getIssuesOfNetworkByIssueTypeAndLinkType(issue,issueTypePKE,"3",linkTypeRelatesTo).getIssues()

        //get the sprint name for each story. The names must be updated as labels in the pke

        Set<String> sprintNamesSet = new HashSet<String>()


        stories.each {

            //we create an issue
            def myIssue = issueManager.getIssueObject(it.getId())

            def mySprintAndReleaseName = getSprintAndReleaseName(myIssue)

            sprintNamesSet.add(mySprintAndReleaseName)

        }



        //we should have only one common business request for all found stories
        if (pkes.size()== 1){


            //we create an issue
            def myPKE = issueManager.getIssueObject(pkes.get(0).getId())

            setLabelsForCustomField(myPKE, sprintNamesSet, customFieldName)
        }


    }


    def setReleaseAndSprintNamesInBusinessRequest(Issue issue, String customFieldName){

        //start customizing

        def issueTypeStory = "Story"
        def issueTypeBusinessRequest = "\"Business Request\""
        def linkTypeRelatesTo = "relates to"

        //end customizing



        //we need the IssueManager in order to create the issue of the the result of the query
        IssueManager issueManager = ComponentAccessor.getIssueManager()

        //get all stories linked by relates to
        def stories = getIssuesOfNetworkByIssueTypeAndLinkType(issue,issueTypeStory,"3",linkTypeRelatesTo).getIssues()

        //get the business request. We should only have one business request linked to the stories
        def businessRequests= getIssuesOfNetworkByIssueTypeAndLinkType(issue,issueTypeBusinessRequest,"3",linkTypeRelatesTo).getIssues()

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

            setLabelsForCustomField(myBusinessRequest, sprintNamesSet, customFieldName)
        }



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
            myTempList = getIssuesOfNetworkByLinkType(issue,"3","relates to").getIssues()


            for (Issue item : myTempList){
                if(item.getIssueTypeObject().getName() != issueTypeBusinessRequest ){
                    issuesInNetwork.add(item)
                }
            }

            //get the requirements for each story

            myTempList = []

            for (Issue item : issuesInNetwork){
                if(item.getIssueTypeObject().getName() == issueTypeStory ){

                    myTempList.addAll(getIssuesOfNetworkByLinkType(item, "1", "is validated by").getIssues())

                }
            }

            issuesInNetwork.addAll(myTempList)

            myTempList = []
            //get the test cases for each requirement
            for (Issue item : issuesInNetwork){
                if(item.getIssueTypeObject().getName() == issueTypeRequirement ){

                    myTempList.addAll(getIssuesOfNetworkByLinkType(item, "1", "is tested by").getIssues())

                }
            }

            issuesInNetwork.addAll(myTempList)


            //get the bugs for each test case
            myTempList = []
            for (Issue item : issuesInNetwork){
                if(item.getIssueTypeObject().getName() == issueTypeTestCase ){

                    myTempList.addAll(getIssuesOfNetworkByLinkType(item, "1", "is blocked by").getIssues())

                }
            }
            issuesInNetwork.addAll(myTempList)

        }


        if(issue.getIssueTypeObject().getName()== issueTypePKE){

            //gets all issues of type story and excludes the PKE
            myTempList =[]
            for (Issue item : getIssuesOfNetworkByLinkType(issue,"3","relates to").getIssues()){
                if(item.getIssueTypeObject().getName() != issueTypePKE ){
                    myTempList.add(item)
                }
            }

            issuesInNetwork.addAll(myTempList)



            //get the requirements for each story
            myTempList = []

            for (Issue item : issuesInNetwork){
                if(item.getIssueTypeObject().getName() == issueTypeStory ){
                    myTempList.addAll(getIssuesOfNetworkByLinkType(item, "1", "is validated by").getIssues())
                }
            }

            issuesInNetwork.addAll(myTempList)



            //get the testcases for each requirement
            myTempList = []
            for (Issue item : issuesInNetwork){
                if(item.getIssueTypeObject().getName() == issueTypeRequirement ){
                    myTempList.addAll(getIssuesOfNetworkByLinkType(item, "1", "is tested by").getIssues())
                }
            }

            issuesInNetwork.addAll(myTempList)


            //get the bugs for each testcase
            myTempList = []
            for (Issue item : issuesInNetwork){
                if(item.getIssueTypeObject().getName() == issueTypeTestCase ){
                    myTempList.addAll(getIssuesOfNetworkByLinkType(item, "1", "is blocked by").getIssues())
                }
            }

            issuesInNetwork.addAll(myTempList)
        }





        if(issue.getIssueTypeObject().getName()== issueTypeStory){

            //gets all issues of type PKE or Business Request
            myTempList = []
            for (Issue item : getIssuesOfNetworkByLinkType(issue,"3","relates to").getIssues()){
                if(item.getIssueTypeObject().getName() != issueTypeStory ){
                    myTempList.add(item)
                }
            }

            issuesInNetwork.addAll(myTempList)


            //get the requirements for the story
            myTempList = getIssuesOfNetworkByLinkType(issue, "1", "is validated by").getIssues()
            for (Issue item : myTempList){
                issuesInNetwork.add(item)
            }



            //get the testcases for each requirement
            myTempList = []
            for (Issue item : issuesInNetwork){
                if(item.getIssueTypeObject().getName() == issueTypeRequirement ){
                    myTempList.addAll(getIssuesOfNetworkByLinkType(item, "1", "is tested by").getIssues())
                }
            }

            issuesInNetwork.addAll(myTempList)


            //get the bugs for each requirement
            myTempList = []
            for (Issue item : issuesInNetwork){
                if(item.getIssueTypeObject().getName() == issueTypeTestCase ){
                    myTempList.addAll(getIssuesOfNetworkByLinkType(item, "1", "is blocked by").getIssues())
                }
            }

            issuesInNetwork.addAll(myTempList)
        }


        if(issue.getIssueTypeObject().getName()== issueTypeRequirement){

            //get the test cases
            myTempList = getIssuesOfNetworkByLinkType(issue, "1", "is tested by").getIssues()
            for(Issue item : myTempList){
                issuesInNetwork.add(item)
            }


            //get the bugs for each test case
            myTempList = []
            for (Issue item : issuesInNetwork){
                if(item.getIssueTypeObject().getName() == issueTypeTestCase ){
                    myTempList.addAll(getIssuesOfNetworkByLinkType(item, "1", "is blocked by").getIssues())
                }
            }

            issuesInNetwork.addAll(myTempList)

            // get the stories
            myTempList = getIssuesOfNetworkByLinkType(issue, "1", "validates").getIssues()
            for(Issue item : myTempList){
                issuesInNetwork.add(item)
            }


            // get the business requests or PKEs
            myTempList = []
            for (Issue item : issuesInNetwork){
                if(item.getIssueTypeObject().getName() == issueTypeStory ){
                    myTempList.addAll(getIssuesOfNetworkByLinkType(item, "2", "is related").getIssues())
                }
            }

            issuesInNetwork.addAll(myTempList)

        }

        if(issue.getIssueTypeObject().getName()== issueTypeTestCase){

            //get the bugs
            myTempList = getIssuesOfNetworkByLinkType(issue, "1", "is blocked").getIssues()
            for (Issue item : myTempList){
                issuesInNetwork.add(item)
            }


            // get the requirements
            myTempList = getIssuesOfNetworkByLinkType(issue, "1", "tests").getIssues()
            for(Issue item : myTempList){
                issuesInNetwork.add(item)
            }


            // get the stories
            myTempList = []
            for (Issue item : issuesInNetwork){
                if(item.getIssueTypeObject().getName() == issueTypeRequirement ){
                    myTempList.addAll(getIssuesOfNetworkByLinkType(item, "1", "validates").getIssues())
                }
            }

            issuesInNetwork.addAll(myTempList)


            // get the business requests or PKEs
            myTempList = []
            for (Issue item : issuesInNetwork){
                if(item.getIssueTypeObject().getName() == issueTypeStory ){
                    myTempList.addAll(getIssuesOfNetworkByLinkType(item, "2", "is related").getIssues())
                }
            }

            issuesInNetwork.addAll(myTempList)

        }


        if(issue.getIssueTypeObject().getName()== issueTypeEpic){

            myTempList = getIssuesOfNetworkByLinkType(issue,"2","is_Epic_of").getIssues()
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




    def handleIssueTypeOrder(Issue issue){

        //** customizing **

        def issueTypeOrder = "OrderIssueEventHandler"
        def customFieldOrder = ".OrderIssueEventHandler"

        def orderId = "09071972"//issue.getSummary()
        def issueType = issue.getIssueTypeObject().getName()
        def issues

        if(issueType == issueTypeOrder){

            issues = selectOrderItems(issue,orderId,customFieldOrder)
        }

        for (Issue item : issues){

            addSubTask(issue,item.getKey(),item.getSummary(),item.getKey()+": "+ item.getSummary())
        }

        println ""


    }

    def selectOrderItems(Issue issue, String orderId, String customFieldOrder){

        def query
        def queryResult
        def issues = new HashSet<Issue>()

        query = customFieldOrder + "=" + orderId


        queryResult =getIssuesByQuery(query).getIssues()



        //we need the IssueManager in order to create the issue of the the result of the query
        IssueManager issueManager = ComponentAccessor.getIssueManager()


        for(Issue item : queryResult){

            issues.add(issueManager.getIssueObject(item.getId()))
        }

        return issues
    }



    def  setWorkflowTransition(Issue issue, Integer transitionID){

        def mutableIssue = (MutableIssue)issue
        def status

        WorkflowTransitionUtil workflowTransitionUtil = ( WorkflowTransitionUtil ) JiraUtils.loadComponent( WorkflowTransitionUtilImpl.class )

        workflowTransitionUtil.setIssue(mutableIssue)
        workflowTransitionUtil.setUsername(currentUser.getName())

        //Transitions toDo(11) inProgress(21) Done(31)

        workflowTransitionUtil.setAction(transitionID)
        workflowTransitionUtil.validate()
        workflowTransitionUtil.progress()



    }


    def getAllRequirementTestCasesBugForStory(Issue storyIssue){

        Set<Issue> issuesInNetwork = new HashSet<Issue>()


        Set<Issue> myListOfRequirements = new HashSet<Issue>()

        Set<Issue> myListOfTestCases = new HashSet<Issue>()

        Set<Issue> myListOfBugs = new HashSet<Issue>()


        //get the requirements which are tested by the the test case
        myListOfRequirements.addAll(getIssuesOfNetworkByLinkType(storyIssue,"1", "is validated by").getIssues())
        issuesInNetwork.addAll(myListOfRequirements)


        // get all test cases for all requirements
        for(Issue requirement : myListOfRequirements){

            myListOfTestCases.addAll(getIssuesOfNetworkByLinkType(requirement,"1", "is tested by").getIssues())
            issuesInNetwork.addAll(myListOfTestCases)
        }

        // get all bugs for all test cases
        for(Issue testcase : myListOfTestCases){

            myListOfBugs.addAll(getIssuesOfNetworkByLinkType(testcase,"1", "is blocked by").getIssues())
            issuesInNetwork.addAll(myListOfBugs)
        }

        return issuesInNetwork


    }

    def castToIssue(Issue issue){
        def castedIssue = issue

        if ( issue instanceof DocumentIssueImpl){

            castedIssue = getIssueByKey(issue.getKey())
        }


        return castedIssue
    }

    def getStoryFromRequirement(Issue requirement){

        def Issue story
        def  myTempListOfIssues =[]



        myTempListOfIssues.addAll(getIssuesOfNetworkByLinkType(requirement,"1", "validates").getIssues())

        //we should have only one story

        story = castToIssue(myTempListOfIssues.get(0))

        return story

    }

    def getRequirementsFromTestcase(Issue testcase){


        def  myTempListOfIssues =[]
        myTempListOfIssues.addAll(getIssuesOfNetworkByLinkType(testcase,"1", "tests").getIssues())


        return myTempListOfIssues

    }


    def getStoryFromTestcase(Issue testcase){

        def size

        Set<Issue> issuesInNetwork = new HashSet<Issue>()

        def stories = []

        for(Issue item : getRequirementsFromTestcase(testcase)){


            issuesInNetwork.addAll(getStoryFromRequirement(item))

        }

        stories.addAll(issuesInNetwork)

        if(issuesInNetwork.size() == 1){
            return stories.get(0)
        }



    }



    def addAndRemoveLinksToIssue(Issue fromIssue, Issue toIssue, String linkTypeName, String environment, String addDeleteFlag,Category log){

        log.info("Beginn of addAndRemoveLinksToIssue")

        def constPROD = "PROD"
        def constDEV = "DEV"

        //these valued depend directly of the JIRA customizing
        def Relates_DEV = "10003"
        def Tests_DEV = "10301"

        def Relates_PROD = "10003"
        def Tests_PROD = "10500"

        def Cloners = "10001"
        def Duplicate = "10002"
        def Validates = "10300"




        def MutableIssue mutableIssue = (MutableIssue)fromIssue
        def issueLinkManager = ComponentAccessor.getIssueLinkManager()
        def issueLinkTypeManager = ComponentAccessor.getComponentOfType(IssueLinkTypeManager.class)
        def issueManager = ComponentAccessor.getIssueManager()
        def sourceIssueId = fromIssue.getId()
        def destinationIssueId = toIssue.getId()
        def user = getCurrentUser()
        def linkTypeId = getIssueLinkTypeIDbyName(linkTypeName)


       def issueLink = issueLinkManager.getIssueLink(fromIssue.getId(),toIssue.getId(),linkTypeId)




        if(linkTypeName == "Tests"){

            if(environment == constPROD )

                if(addDeleteFlag == "add"){

                    log.info("Before issueLinkManager.createIssueLink")
                    issueLinkManager.createIssueLink(sourceIssueId,destinationIssueId, Long.parseLong(Tests_PROD),Long.valueOf(1), user)
                }

                if(addDeleteFlag == "delete"){

                    log.info("Before issueLinkManager.removeIssueLin")
                    issueLinkManager.removeIssueLink(issueLink,user)


                }

            else if (environment == constDEV){

                if(addDeleteFlag == "add"){
                    issueLinkManager.createIssueLink(sourceIssueId,destinationIssueId, Long.parseLong(Tests_DEV),Long.valueOf(1), user)
                }

                if (addDeleteFlag =="delete"){

                    issueLinkManager.removeIssueLink(issueLink,user)

                }

            }
        }



        else if (linkTypeName == "Relates"){

            if(environment == constPROD )
                issueLinkManager.createIssueLink(sourceIssueId,destinationIssueId, Long.parseLong(Relates_PROD),Long.valueOf(1), user)

            else if (environment == constDEV){
                issueLinkManager.createIssueLink(sourceIssueId,destinationIssueId, Long.parseLong(Relates_DEV),Long.valueOf(1), user)
            }

        }


        issueManager.updateIssue(user,mutableIssue,EventDispatchOption.ISSUE_UPDATED, false)

    }


    /*
    Returns the linkTypeId for the given Name
     */

    def getIssueLinkTypeIDbyName(String linkTypeName){

        def issueLinkTypeManager = ComponentAccessor.getComponentOfType(IssueLinkTypeManager.class)

        def issueLinkTypes = issueLinkTypeManager.getIssueLinkTypes()

        def linkTypeId


        for (IssueLinkType linkType :issueLinkTypes){

            if(linkType.getName() == linkTypeName){

                linkTypeId = linkType.getId()

            }
        }


        return linkTypeId



    }


    def showLinkTypes(Category log){




         def issueLinkTypeManager = ComponentAccessor.getComponentOfType(IssueLinkTypeManager.class)

         def issueLinkTypes = issueLinkTypeManager.getIssueLinkTypes()

         for (IssueLinkType linkType :issueLinkTypes){


                 def name = linkType.getName()
                     log.info("linkTypeName = " + name)
                 def id = linkType.getId()
                     log.info("linkTypeID = " + id)
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



    def setCustomFieldValueOption(Issue myIssue, String mycustomFieldName, String myOption){



        def optionsManager = ComponentAccessor.getComponentOfType(OptionsManager.class)
        def customFieldManager = ComponentAccessor.getCustomFieldManager()

        //gather the fields needed
        def cf = customFieldManager.getCustomFieldObjectByName(mycustomFieldName)
        def fieldconfig = cf.getRelevantConfig(myIssue)
        def options = optionsManager.getOptions(fieldconfig)
        def option = optionsManager.getOptions(fieldconfig).find {it.value == myOption}



        if(myIssue instanceof DocumentIssueImpl){
            myIssue = getIssueByKey(myIssue.getKey())
        }

        def MutableIssue myMutableIssue = (MutableIssue)myIssue


        myMutableIssue.setCustomFieldValue(cf,option)


        Map<String,ModifiedValue> modifiedFields = myMutableIssue.getModifiedFields()

        FieldLayoutItem myFieldLayoutItem = ComponentAccessor.getFieldLayoutManager().getFieldLayout(myMutableIssue).getFieldLayoutItem(cf)

        DefaultIssueChangeHolder myDefaultIssueChangeHolder = new DefaultIssueChangeHolder()

        final ModifiedValue myModifiedValue = modifiedFields.get(cf.getId())

        cf.updateValue(myFieldLayoutItem,myMutableIssue,myModifiedValue,myDefaultIssueChangeHolder)


    }


    def getStoryFromSubTask(Issue issue){

        //get the user story based on link type "validates" and set .developername
        def  myTempListOfStories =[]

        def story

        myTempListOfStories.addAll(getIssuesOfNetworkByIssueTypeAndLinkType(issue,"Story","1", "").getIssues())

        if (myTempListOfStories.size() != null) {

            //we should have only one story as parent of a sub task
            return story = castToIssue(myTempListOfStories[0])
        }
    }


/*
This method gets al subtasks for a story and casts the result in to proper issues
 */


    def getAllSubTasksForStory(Issue issueStory){

        def issueTypeNameSubTasks = "Sub-task"

        def castedIssuesOfTypeSubTask = []

        // get all SubTasks for the story
        def subTasks = getIssuesOfNetworkByIssueTypeAndLinkType(issueStory,issueTypeNameSubTasks,"1","").getIssues()


        //cast all issues
        for(Issue item : subTasks){

            def myCastedItem = castToIssue(item)

            castedIssuesOfTypeSubTask.add(myCastedItem)

        }


        return castedIssuesOfTypeSubTask

    }

    /*
    This method determines the delta of two lists.
    It returns two lists : toBeAdded and toBeDeleted from existingList
     */
    def compareTwoListsOfStrings(List existingList, List updatedList){

        def toBeAddedToExistingList = []
        def toBeDeletedFromExistingList = []
        Set<Object> overlapList = new HashSet<Object>()

        def resultLists =[]
        def found

        resultLists.add(toBeAddedToExistingList)
        resultLists.add(toBeDeletedFromExistingList)
        resultLists.add(overlapList)


        //check for all objects that exist in the compareWithList if these also exist in the masterList
        //if this is the case, then nothing to be done. If one is missing, this hast to be added to the masterlist

        if(updatedList.size()!=0){  //make sure we have at least one item in the list

            // check if new items have to be added to the existingList
            // for every item in the updatedList we check if we have the item in the existing list
            for(String item: updatedList){
                def test = item.toString()
                found = existingList.find{ it == item.toString()}

                //item in updatedList does not exist in existing List
                if(found == null){
                    toBeAddedToExistingList.add(item)
                }


                else if(found != null){ //the item exists in both lists. Nothing to be done
                    overlapList.add(item)
                }
            }

            for(String item :existingList){

                found = updatedList.find{ it == item.toString()}

                    if(found == null){

                        toBeDeletedFromExistingList.add(item)

                    }

                    if(found != null){
                        overlapList.add(item)
                    }


                }
            }



        return resultLists
    }

}