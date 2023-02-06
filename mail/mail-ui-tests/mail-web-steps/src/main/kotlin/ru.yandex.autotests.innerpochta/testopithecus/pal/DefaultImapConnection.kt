package ru.yandex.autotests.innerpochta.testopithecus.pal

import com.yandex.xplat.testopithecus.MailAccountSpec
import java.util.*
import javax.mail.MessagingException
import javax.mail.Session
import javax.mail.Store

public class DefaultImapConnection {

    private val protocolKey: String = "mail.store.protocol"
    private val protocol: String = "imaps"

    private var host: String
    private var login: String
    private var password: String

    private lateinit var session: Session
    private lateinit var store: Store

    public constructor(account: MailAccountSpec) {
        this.host = account.host
        this.login = account.login
        this.password = account.password
    }

    public fun getStore(): Store {
        if (!this::store.isInitialized) {
            this.createStore()
        }
        return this.store
    }

    public fun getSession(): Session {
        if (!this::session.isInitialized) {
            this.startSession()
        }
        return this.session
    }

    private fun createStore() {
        try {
            val store: Store = this.getSession().getStore(this.protocol)
            store.connect(this.host, 993, this.login, this.password)
            this.store = store
        } catch (e: MessagingException) {
            throw RuntimeException(e)
        }
    }

    private fun startSession() {
        val props: Properties = System.getProperties()
        props.setProperty(protocolKey, protocol)
        props.setProperty("mail.debug", "true")
        this.session = Session.getInstance(props, null)
    }
}
