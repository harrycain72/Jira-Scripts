import com.atlassian.jira.event.issue.IssueEvent
import com.atlassian.jira.issue.Issue
import org.apache.log4j.Category
import util.Helper
import util.CustomizingMngr



def getCurrentIssue(String flag,CustomizingMngr cm){

    def myIssue

    if(flag == cm.getConsstantWF()){
        myIssue =(Issue)issue
    }

    if(flag == cm.getConstantEV()){
        def event = event as IssueEvent
        myIssue = event.getIssue()
    }

    return myIssue
}

def main(Issue issue, Category log, Helper hp, String environment) {

    def CustomizingMngr cm = new CustomizingMngr()


    def issueType = issue.getIssueTypeObject().getName()

    //we only consider changes of workflow status for sub tasks
    if(issueType == cm.getIssueTypeNameSubTasks()){

        def story
        def storyWfStatus
        def subTaskWfStatus
        def listAllsubTasksForStory = []
        Set<String> setSubTaskWfStatusNames = new HashSet<String>()
        def wfStatusNameDoneExists
        def wfStatusNameInProgessExists
        def wfStatusNameToDoExists
        def wfStatusStoryCurrent

        //get the story relevant for this subtask
        story = hp.getStoryFromSubTask(issue)
        wfStatusStoryCurrent = story.getStatusObject().getName()

        //get alll sub tasks
        listAllsubTasksForStory = hp.getAllSubTasksForStory(story)

        //get the workflow status for all subtasks
        for(Issue subTaskIssue : listAllsubTasksForStory){

            def wfStatusName = subTaskIssue.getStatusObject().getName()
            setSubTaskWfStatusNames.add(wfStatusName)

        }

        wfStatusNameDoneExists = setSubTaskWfStatusNames.contains(cm.getWfStatusDone())
        wfStatusNameInProgessExists = setSubTaskWfStatusNames.contains(cm.getWfStatusInProgress())
        wfStatusNameToDoExists = setSubTaskWfStatusNames.contains(cm.getWfStatusToDo())


        //get the WF-Status of the story and of the subtask

        storyWfStatus = story.getStatusObject().getName()

        subTaskWfStatus = issue.getStatusObject().getName()



        //if the status of the workflow was changed to "In Progress"
        if(subTaskWfStatus == cm.getWfStatusInProgress()) {

            if (wfStatusNameInProgessExists == true && wfStatusNameDoneExists == false && wfStatusStoryCurrent == cm.getWfStatusToDo() && storyWfStatus != subTaskWfStatus) {


                if (storyWfStatus != subTaskWfStatus) {



                    if (environment == cm.getConstantDEV()) {
                        hp.setWorkflowTransition(story, cm.getWfTransitionID_StartWork_11_DEV())
                    }

                    if (environment == cm.getConstantPROD()) {
                        hp.setWorkflowTransition(story, cm.getWfTransitionID_Story_StartWork_11_PROD())
                    }


                }


            }

            if (wfStatusNameInProgessExists == true && wfStatusStoryCurrent == cm.getWfStatusDone() && storyWfStatus != subTaskWfStatus) {


                    if (storyWfStatus != subTaskWfStatus) {



                        if (environment == cm.getConstantDEV()) {
                            hp.setWorkflowTransition(story, cm.getWfTransitionID_RestartWork_41_DEV())
                        }

                        if (environment == cm.getConstantPROD()) {
                            hp.setWorkflowTransition(story, cm.getWfTranstionID_Story_RestartWork_41_PROD())
                        }


                    }



            }



        }




        else if(subTaskWfStatus == cm.getWfStatusDone()){

            if(wfStatusNameInProgessExists==false && wfStatusNameToDoExists==false && storyWfStatus != subTaskWfStatus ){



                if(environment == cm.getConstantDEV()){
                    hp.setWorkflowTransition(story,cm.getWfTransitionID_FinalizeWork_31_DEV())
                }

                if(environment == cm.getConstantPROD()){
                    hp.setWorkflowTransition(story,cm.getWfTransitionID_Story_FinishWork_31_PROD())
                }

            }



        }


        else if(subTaskWfStatus == cm.getWfStatusToDo()){

            if(wfStatusNameInProgessExists == false && wfStatusNameDoneExists==false && storyWfStatus != subTaskWfStatus){

                //transition in 11 = To Do
                if(environment == cm.getConstantDEV()){
                    hp.setWorkflowTransition(story,cm.getWfTransitionID_CancelWork_21_DEV())
                }

                if(environment == cm.getConstantPROD()){
                    hp.setWorkflowTransition(story,cm.getWfTransitionID_Story_CancelWork_21_PROD())
                }


            }


        }




    }



}





def Category log = Category.getInstance("com.onresolve.jira.groovy")
def CustomizingMngr cm = new CustomizingMngr()
def hp = new Helper()

log.setLevel(org.apache.log4j.Level.OFF)

main(getCurrentIssue("WF",cm),log,hp,cm.getConstantDEV())