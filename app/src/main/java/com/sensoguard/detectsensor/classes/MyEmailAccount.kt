package com.sensoguard.detectsensor.classes

class MyEmailAccount {
    var userName: String? = null
    var password: String? = null
    var outgoingServer: String? = null
    var outgoingPort: Int? = null
    var recipient: String? = null
    var isUseSSL: Boolean = false

    constructor(
        userName: String?,
        password: String?,
        outgoingServer: String?,
        outgoingPort: Int?,
        recipient: String?,
        isUseSSL: Boolean
    ) {
        this.userName = userName
        this.password = password
        this.outgoingServer = outgoingServer
        this.outgoingPort = outgoingPort
        this.recipient = recipient
        this.isUseSSL = isUseSSL
    }
}