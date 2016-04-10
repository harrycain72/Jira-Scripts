import com.atlassian.jira.event.issue.IssueEvent
import com.atlassian.jira.issue.Issue
import org.apache.log4j.Category
import util.Helper

/**
 * Created by roland on 10.04.16.
 */


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

def main(Issue issue, Category log, Helper hp, String environment) {

    //just relevant for testing purposes in order to check the name of JIRA-fields
    //def test = event.getChangeLog().getRelated('ChildChangeItem')

    //begin customizing


    def issueTypeNameSubTasks = "Sub-task"
    def constantWfStatusInProgress = "In Progress"
    def constantWfStatusDone = "Done"
    def constandWfStatusToDo = "To Do"

    //end customizing


    def issueType = issue.getIssueTypeObject().getName()

    //we only consider changes of workflow status for sub tasks
    if(issueType == issueTypeNameSubTasks){

        def story
        def storyWfStatus
        def subTaskWfStatus
        def listAllsubTasksForStory = []
        Set<String> setSubTaskWfStatusNames = new HashSet<String>()
        def wfStatusNameDoneExists
        def wfStatusNameInProgessExists
        def wfStatusNameToDoExists

        //get the story relevant for this subtask
        story = hp.getStoryFromSubTask(issue)

        //get alll sub tasks
        listAllsubTasksForStory = hp.getAllSubTasksForStory(story)

        //get the workflow status for all subtasks
        for(Issue subTaskIssue : listAllsubTasksForStory){

            def wfStatusName = subTaskIssue.getStatusObject().getName()
            setSubTaskWfStatusNames.add(wfStatusName)

        }

        wfStatusNameDoneExists = setSubTaskWfStatusNames.contains(constantWfStatusDone)
        wfStatusNameInProgessExists = setSubTaskWfStatusNames.contains(constantWfStatusInProgress)
        wfStatusNameToDoExists = setSubTaskWfStatusNames.contains(constandWfStatusToDo)


        //get the WF-Status of the story and of the subtask

        storyWfStatus = story.getStatusObject().getName()

        subTaskWfStatus = issue.getStatusObject().getName()


        //if the status of the workflow was changed to "In Progress"
        if(subTaskWfStatus == constantWfStatusInProgress){


            if(storyWfStatus != subTaskWfStatus){

                    //transition id 21 = In Progress

                    hp.setWorkflowTransition(story,21)

            }

        }

        else if(subTaskWfStatus == constantWfStatusDone){

            if(wfStatusNameInProgessExists==false && wfStatusNameToDoExists==false && storyWfStatus != subTaskWfStatus ){

                //transition in 31 = Done
                hp.setWorkflowTransition(story,31)
            }

            if(wfStatusNameInProgessExists==false && wfStatusNameToDoExists==true && storyWfStatus != subTaskWfStatus ){

                //transition in 21 = In Progress
                hp.setWorkflowTransition(story,21)
            }

        }


        else if(subTaskWfStatus == constandWfStatusToDo){

            if(wfStatusNameInProgessExists == false && wfStatusNameDoneExists==false && storyWfStatus != subTaskWfStatus){

                //transition in 11 = To Do
                hp.setWorkflowTransition(story,11)

            }

            else if(wfStatusNameInProgessExists == false && wfStatusNameDoneExists==true && storyWfStatus != subTaskWfStatus){

                //transition in 21 = In Progresss
                hp.setWorkflowTransition(story,21)

            }


        }




    }



}





def Category log = Category.getInstance("com.onresolve.jira.groovy")

log.setLevel(org.apache.log4j.Level.OFF)

def constDEV = "DEV"
def constPROD = "PROD"

hp = new Helper()

main(getCurrentIssue("WF"),log,hp,constDEV)