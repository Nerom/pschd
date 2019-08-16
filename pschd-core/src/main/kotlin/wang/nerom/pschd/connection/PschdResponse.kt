package wang.nerom.pschd.connection

import java.io.Serializable

class PschdResponse : Serializable {
    val success: Boolean
    val errorCode: ErrorCode?
    val errorMsg: String

    constructor(success: Boolean) {
        this.success = success
        this.errorMsg = ""
        this.errorCode = null
    }

    constructor(errorCode: ErrorCode, errorMsg: String) {
        this.success = false
        this.errorMsg = errorMsg
        this.errorCode = errorCode
    }

    enum class ErrorCode {
        SYS_ERR
    }
}