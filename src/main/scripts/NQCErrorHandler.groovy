import com.atlassian.crowd.embedded.api.User
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.issue.IssueEvent
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.user.util.UserUtil
import com.opensymphony.workflow.WorkflowContext
import util.Helper
import util.NQCError

import java.sql.ResultSet

/**
 * Created by roland on 17.02.16.
 */




//retrieves the current issue i.e. for a listener
def Issue getCurrentIssue(String flag){

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

def User getCurrentUserWF() {

    String currentUser = ((WorkflowContext) transientVars.get("context")).getCaller()

    //Security
    UserUtil userUtil = ComponentAccessor.getUserUtil()

    User user = userUtil.getUser(currentUser)


}

def User getCurrentUser() {

    //Security
    jac = ComponentAccessor.getJiraAuthenticationContext()

    currentUser = jac.getUser().getDirectoryUser()
}

/* creates an issue in Jira for a defined project and issuetype
   It is necessary, that this issuetype is availalbe for the project
*/
def Issue createIssues(String projectKey, String issueType, List issuesToCreateList, Helper hp){


    def myIssue
    def myIssueSummary
    def myIssueDescription
    def myIssueReporter
    def currentUser

    //create Jira issues

    for (NQCError error : issuesToCreateList){

        def minDate = error.getMIN_DATE()
        def minDATE = new Date().parse("yyyy-M-d",minDate)

        def maxDate = error.getMAX_DATE()
        def maxDATE = new Date().parse("yyyy-M-d",maxDate)

        def age = ( (maxDATE.getTime() - minDATE.getTime()) / (1000 * 60 * 60 * 24))

        myIssueSummary = error.getLIEF_ID() +" "+error.getNAME_1() + " Error Code: " + error.getERROR_CODE() + ": " + error.getERROR_TEXT()


        myIssueDescription = """


        h1. Error data

        h4. Error message
        ${error.getERROR_TEXT()}

        h4. Error code
        ${error.getERROR_CODE()}

        h4. Error age
        ${age} days

        h4. Date of first occurence of error
        ${error.getMIN_DATE()}

        h4. Date of last occurence of error
        ${error.getMAX_DATE()}

        h1. Supplier data


        h4. Name

        ${error.getNAME_1()}

        h4. Supplier ID

        ${error.getLIEF_ID()}

        h4. Email

         ${error.getEMAIL()}


        h1. SAQ Data


        h4. SAQ created

        ${error.getSAQ_ANGELEGT()}

        h4. SAQ changed

        ${error.getSAQ_GEAENDERT()}

        h4. Interface date

        ${error.getINTERFACE_DATUM()}


        h1. BMW buyer and department


        h4. Buyer BMW
        ${error.getNAME_VORNAME()}

        h4. Departement BMW
        ${error.getABTEILUNG()}


        h1. Address


        h4. Country Key
        ${error.getCOUNTRY_KEY()}

        h4. Zip Code
        ${error.getPOSTLEITZAHL()}

        h4. City
        ${error.getORT()}

        h4. Road
        ${error.getSTRASSE()}

        h2. Technical data



        h4. NF_ID
        ${error.getNF_ID()}

        h4. Location context reference
        ${error.getLOCATION_CONTEXT_REF()}

        h4. Reference
        ${error.getREFERENCE()}

        h4. XML Reference
        ${error.getREFERENCE()}


        """.replaceAll(/    /, '')







        myIssueReporter = "harry.cain72@gmail.com"
        currentUser = getCurrentUser()


        try{
            myIssue = hp.createIssue(projectKey,issueType,myIssueSummary,myIssueDescription,myIssueReporter,currentUser)

            hp.setCustomFieldValue(myIssue,error.getNF_ID().toString(),hp.getCustomField(".NF_ID"))

            // set additional information as labels to the created issue
            def labels = [error.getERROR_CODE(),error.getERROR_TEXT()].toSet()

            hp.setLabelJiraField(myIssue,labels)


        }

        catch(all){

        }

    }



    return myIssue

}



def main(Issue issue, Helper hp){

    //Beginn customizing
        def projectKey = "DEMO"
        def issueType = "Story"
    //End customizing

    def check = event.getChangeLog().getRelated('ChildChangeItem')


    //
    def existingIssuesMap = [:]
    def issueList

    issueList = getAllIssuesOfProject(hp)

    for (Issue myIssue : issueList ) {

       def id = hp.getCustomFieldValue(myIssue,".NF_ID").toInteger()

       existingIssuesMap.put(id,myIssue)
    }



    //
    def nqcErrors
    def dbTable = "V_NQC_INTERFACE_ERROR_INIT"
    def sqlQuery = "SELECT * FROM LIDA.V_NQC_INTERFACE_ERROR_INIT where INTERFACE_DATUM = \"2016.03.01\" order by NF_ID asc"

    nqcErrors = getNQCErrorsFromDB(dbTable,sqlQuery,hp)





    //
    def toDoLists
    toDoLists = getIssueLists(existingIssuesMap,nqcErrors)

    def issuesToCreateList = toDoLists.get("CREATE")
    def issuesToCloseList = toDoLists.get("CLOSE")
    def issuesToUpdateList = toDoLists.get("UPDATE")


    createIssues("DEMO","Story",issuesToCreateList,hp)



}


def getIssueLists(Map existingIssues, Map nqcErrors ){


    def issuesToUpdateList = []
    def issuesToCloseList = []
    def issuesToCreateList = []
    def issueLists = [:]


    existingIssues.each { entry ->

        def issue  = entry.getValue()

        def result = nqcErrors.containsKey(entry.getKey())


        if (result == true){

            def nqcErrorToUpdate

            nqcErrorToUpdate  = nqcErrors.get(entry.getKey())
            issuesToUpdateList.add(nqcErrorToUpdate)
        }

        if (result == false){

            def nqcErrorToClose

            issueToClose = existingIssues.get(entry.getKey())
            issuesToCloseList.add(issueToClose)
        }


    }

    nqcErrors.each { entry ->


        def result = existingIssues.containsKey(entry.getKey())



        if (result == false){

            def nqcErrorToCreate

            nqcErrorToCreate = nqcErrors.get(entry.getKey())
            issuesToCreateList.add(nqcErrorToCreate)
        }


    }



    issueLists.put("UPDATE",issuesToUpdateList)
    issueLists.put("CLOSE",issuesToCloseList)
    issueLists.put("CREATE",issuesToCreateList)

    return issueLists

}

def getAllIssuesOfProject(Helper hp){

    def sqlQuery
    def issues
    sqlQuery = "project = demo and issuetype = Story"
    issues =  hp.getIssuesByQuery(sqlQuery).getIssues()

    return issues

}

def Map getNQCErrorsFromDB(String dbTable, String sqlQuery,Helper hp){

    //this map has the key of the  row in DB and the object NQCError
    def  nqcErrorsMap = [:]

    //prepare the structure
    def NF_ID
    def SUPID
    def NAME_1
    def LIEF_ID
    def UPIK
    def COUNTRY_KEY
    def POSTLEITZAHL
    def ORT
    def STRASSE
    def LOCATION_CONTEXT_REF
    def SAQ_ANGELEGT
    def SAQ_GEAENDERT
    def SAQ_STATUS
    def SCHNITTSTELLE_DATUM
    def ERROR_CODE
    def ERROR_TEXT
    def INTERFACE_DATUM
    def REFERENCE
    def XML_REFERENCE
    def EMAIL
    def NAME_VORNAME
    def ABTEILUNG

    def MIN_DATE
    def MAX_DATE





    //---------------


    def conn // DB connection


    // retrieve a connection for the desired DB-Product
    conn = hp.getDatabaseConnection("mysql","localhost:3306","LIDA","roland","01eins10")

    def sqlMinMax = "SELECT NF_ID, MIN(INTERFACE_DATUM), MAX(INTERFACE_DATUM) FROM LIDA.V_NQC_INTERFACE_ERROR_INIT where NF_ID = ? group by NF_ID asc"

    def preparedStmt = conn.prepareStatement(sqlMinMax)


    def stmt = conn.createStatement()

    def ResultSet rs = stmt.executeQuery(sqlQuery)
    def ResultSet rs2


    def err

    while(rs.next()){
        NF_ID = rs.getInt("NF_ID")

            preparedStmt.setInt(1,NF_ID)
            rs2 = preparedStmt.executeQuery()
            while(rs2.next()){
                MIN_DATE = rs2.getString(2)
                MAX_DATE = rs2.getString(3)
            }

        SUPID = rs.getInt("SUPID")
        NAME_1 = rs.getString("NAME_1")
        LIEF_ID = rs.getString("LIEF_ID")
        UPIK = rs.getString("UPIK")
        COUNTRY_KEY = rs.getString("COUNTRY_KEY")
        POSTLEITZAHL = rs.getString("POSTLEITZAHL")
        ORT = rs.getString("ORT")
        STRASSE = rs.getString("STRASSE")
        LOCATION_CONTEXT_REF = rs.getString("LOCATION_CONTEXT_REF")
        SAQ_ANGELEGT = rs.getString("SAQ_ANGELEGT")
        SAQ_GEAENDERT = rs.getString("SAQ_GEAENDERT")
        SAQ_STATUS = rs.getString("SAQ_STATUS")
        SCHNITTSTELLE_DATUM = rs.getString("SCHNITTSTELLE_DATUM")
        ERROR_CODE = rs.getInt("ERROR_CODE")
        ERROR_TEXT = rs.getString("ERROR_TEXT")
        INTERFACE_DATUM = rs.getString("INTERFACE_DATUM")
        REFERENCE = rs.getInt("REFERENCE")
        XML_REFERENCE = rs.getInt("XML_REFERENCE")
        NAME_VORNAME = rs.getString("NAME_VORNAME")
        ABTEILUNG = rs.getString("ABTEILUNG")


        err = new NQCError(NF_ID,SUPID,NAME_1,LIEF_ID,UPIK,COUNTRY_KEY,POSTLEITZAHL,ORT,STRASSE,LOCATION_CONTEXT_REF,SAQ_ANGELEGT,SAQ_GEAENDERT,SAQ_STATUS,SCHNITTSTELLE_DATUM,ERROR_CODE,ERROR_TEXT,INTERFACE_DATUM,REFERENCE,XML_REFERENCE,EMAIL,NAME_VORNAME,ABTEILUNG,MIN_DATE,MAX_DATE)


        nqcErrorsMap.put(NF_ID.toInteger(),err)
    }

    return nqcErrorsMap

}




//---------

hp = new Helper()

//main(getCurrentIssue("EV"),hp)

//hp.setWorkflowTransition(getCurrentIssue("EV"),getCurrentUser())


