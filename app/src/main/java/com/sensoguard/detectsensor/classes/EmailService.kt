package com.sensoguard.detectsensor.classes

import android.util.Log
import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

class EmailService(private val server: String, private val port: Int) {

    data class Email(
        val auth: Authenticator,
        val toList: List<InternetAddress>,
        val from: Address,
        val subject: String,
        val body: String
    )

    class UserPassAuthenticator(private val username: String, private val password: String) :
        Authenticator() {
        override fun getPasswordAuthentication(): PasswordAuthentication {
            return PasswordAuthentication(username, password)
        }
    }

    fun send(email: Email, isSSL: Boolean) {
        val props = Properties()
        //Log.d("", props.propertyNames().toString())
        props["mail.smtp.auth"] = "true"
        props["mail.user"] = email.from
        props["mail.smtp.host"] = server
        props["mail.smtp.port"] = port
        props["mail.smtp.starttls.enable"] = "true"
        if (isSSL) {
            props["mail.smtp.ssl.trust"] = server
        } else {
            props["mail.smtp.ssl.trust"] = "*"
        }
        props["mail.mime.charset"] = "UTF-8"
        val msg: Message = MimeMessage(Session.getDefaultInstance(props, email.auth))
        msg.setFrom(email.from)
        msg.sentDate = Calendar.getInstance().time
        msg.setRecipients(Message.RecipientType.TO, email.toList.toTypedArray())
//      msg.setRecipients(Message.RecipientType.CC, email.ccList.toTypedArray())
//      msg.setRecipients(Message.RecipientType.BCC, email.bccList.toTypedArray())
        msg.replyTo = arrayOf(email.from)

        msg.addHeader("X-Mailer", CLIENT_NAME)
        msg.addHeader("Precedence", "bulk")
        //msg.addHeader("Content-Type","text/plain; charset=\"utf-8\"")

        msg.subject = email.subject


        msg.setContent(MimeMultipart().apply {
            addBodyPart(MimeBodyPart().apply {
                setText(email.body, "UTF-8")//"text/html; charset=utf-8")//"iso-8859-1")
                //setContent(email.htmlBody, "text/html; charset=UTF-8")
            })
        })
        try {
            Transport.send(msg)
        } catch (ex: SendFailedException) {
            Log.d("", ex.message.toString())
            Log.d("", ex.cause.toString())
        } catch (ex: MessagingException) {
            Log.d("", ex.message.toString())
            Log.d("", ex.cause.toString())
        }
    }

    companion object {
        const val CLIENT_NAME = "Android StackOverflow programmatic email"
    }
}