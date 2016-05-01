package util

/**
 * Created by roland on 30.04.16.
 */


class CustomizingMngr implements CustomizingMngrIF {

    def issueTypeNameRequirement = "Requirement"

    CustomizingMngr() {
    }

    def getIssueTypeNameRequirement() {
        return issueTypeNameRequirement
    }


}