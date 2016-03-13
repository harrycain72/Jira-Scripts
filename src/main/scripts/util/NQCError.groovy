package util

/**
 * Created by roland on 20.02.16.
 */


class NQCError {

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

    NQCError(NF_ID, SUPID, NAME_1, LIEF_ID, UPIK, COUNTRY_KEY, POSTLEITZAHL, ORT, STRASSE, LOCATION_CONTEXT_REF, SAQ_ANGELEGT, SAQ_GEAENDERT, SAQ_STATUS, SCHNITTSTELLE_DATUM, ERROR_CODE, ERROR_TEXT, INTERFACE_DATUM, REFERENCE, XML_REFERENCE, EMAIL, NAME_VORNAME, ABTEILUNG, MIN_DATE, MAX_DATE) {
        this.NF_ID = NF_ID
        this.SUPID = SUPID
        this.NAME_1 = NAME_1
        this.LIEF_ID = LIEF_ID
        this.UPIK = UPIK
        this.COUNTRY_KEY = COUNTRY_KEY
        this.POSTLEITZAHL = POSTLEITZAHL
        this.ORT = ORT
        this.STRASSE = STRASSE
        this.LOCATION_CONTEXT_REF = LOCATION_CONTEXT_REF
        this.SAQ_ANGELEGT = SAQ_ANGELEGT
        this.SAQ_GEAENDERT = SAQ_GEAENDERT
        this.SAQ_STATUS = SAQ_STATUS
        this.SCHNITTSTELLE_DATUM = SCHNITTSTELLE_DATUM
        this.ERROR_CODE = ERROR_CODE
        this.ERROR_TEXT = ERROR_TEXT
        this.INTERFACE_DATUM = INTERFACE_DATUM
        this.REFERENCE = REFERENCE
        this.XML_REFERENCE = XML_REFERENCE
        this.EMAIL = EMAIL
        this.NAME_VORNAME = NAME_VORNAME
        this.ABTEILUNG = ABTEILUNG
        this.MIN_DATE = MIN_DATE
        this.MAX_DATE = MAX_DATE
    }

    def getEMAIL() {
        return EMAIL
    }

    void setEMAIL(EMAIL) {
        this.EMAIL = EMAIL
    }

    def getUPIK() {
        return UPIK
    }

    void setUPIK(UPIK) {
        this.UPIK = UPIK
    }

    def getCOUNTRY_KEY() {
        return COUNTRY_KEY
    }

    void setCOUNTRY_KEY(COUNTRY_KEY) {
        this.COUNTRY_KEY = COUNTRY_KEY
    }

    def getPOSTLEITZAHL() {
        return POSTLEITZAHL
    }

    void setPOSTLEITZAHL(POSTLEITZAHL) {
        this.POSTLEITZAHL = POSTLEITZAHL
    }

    def getORT() {
        return ORT
    }

    void setORT(ORT) {
        this.ORT = ORT
    }

    def getSTRASSE() {
        return STRASSE
    }

    void setSTRASSE(STRASSE) {
        this.STRASSE = STRASSE
    }




    def getNF_ID() {
        return NF_ID
    }

    void setNF_ID(NF_ID) {
        this.NF_ID = NF_ID
    }

    def getSUPID() {
        return SUPID
    }

    void setSUPID(SUPID) {
        this.SUPID = SUPID
    }

    def getNAME_1() {
        return NAME_1
    }

    void setNAME_1(NAME_1) {
        this.NAME_1 = NAME_1
    }

    def getLIEF_ID() {
        return LIEF_ID
    }

    void setLIEF_ID(LIEF_ID) {
        this.LIEF_ID = LIEF_ID
    }

    def getLOCATION_CONTEXT_REF() {
        return LOCATION_CONTEXT_REF
    }

    void setLOCATION_CONTEXT_REF(LOCATION_CONTEXT_REF) {
        this.LOCATION_CONTEXT_REF = LOCATION_CONTEXT_REF
    }

    def getSAQ_ANGELEGT() {
        return SAQ_ANGELEGT
    }

    void setSAQ_ANGELEGT(SAQ_ANGELEGT) {
        this.SAQ_ANGELEGT = SAQ_ANGELEGT
    }

    def getSAQ_GEAENDERT() {
        return SAQ_GEAENDERT
    }

    void setSAQ_GEAENDERT(SAQ_GEAENDERT) {
        this.SAQ_GEAENDERT = SAQ_GEAENDERT
    }

    def getSAQ_STATUS() {
        return SAQ_STATUS
    }

    void setSAQ_STATUS(SAQ_STATUS) {
        this.SAQ_STATUS = SAQ_STATUS
    }

    def getSCHNITTSTELLE_DATUM() {
        return SCHNITTSTELLE_DATUM
    }

    void setSCHNITTSTELLE_DATUM(SCHNITTSTELLE_DATUM) {
        this.SCHNITTSTELLE_DATUM = SCHNITTSTELLE_DATUM
    }

    def getERROR_CODE() {
        return ERROR_CODE
    }

    void setERROR_CODE(ERROR_CODE) {
        this.ERROR_CODE = ERROR_CODE
    }

    def getERROR_TEXT() {
        return ERROR_TEXT
    }

    void setERROR_TEXT(ERROR_TEXT) {
        this.ERROR_TEXT = ERROR_TEXT
    }

    def getINTERFACE_DATUM() {
        return INTERFACE_DATUM
    }

    void setINTERFACE_DATUM(INTERFACE_DATUM) {
        this.INTERFACE_DATUM = INTERFACE_DATUM
    }

    def getREFERENCE() {
        return REFERENCE
    }

    void setREFERENCE(REFERENCE) {
        this.REFERENCE = REFERENCE
    }

    def getXML_REFERENCE() {
        return XML_REFERENCE
    }

    void setXML_REFERENCE(XML_REFERENCE) {
        this.XML_REFERENCE = XML_REFERENCE
    }

    def getNAME_VORNAME() {
        return NAME_VORNAME
    }

    void setNAME_VORNAME(NAME_VORNAME) {
        this.NAME_VORNAME = NAME_VORNAME
    }

    def getABTEILUNG() {
        return ABTEILUNG
    }

    void setABTEILUNG(ABTEILUNG) {
        this.ABTEILUNG = ABTEILUNG
    }

    def getMIN_DATE() {
        return MIN_DATE
    }

    void setMIN_DATE(MIN_DATE) {
        this.MIN_DATE = MIN_DATE
    }

    def getMAX_DATE() {
        return MAX_DATE
    }

    void setMAX_DATE(MAX_DATE) {
        this.MAX_DATE = MAX_DATE
    }
}