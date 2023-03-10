// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM api/entities/recipient/email.ts >>>

package com.yandex.xplat.mapi

import com.yandex.xplat.common.quote
import com.yandex.xplat.common.split
import com.yandex.xplat.common.substr
import com.yandex.xplat.common.substring

public open class Email(val login: String, val domain: String) {
    open fun asString(): String {
        return if (this.login.length > 0 && this.domain.length > 0) "${this.login}@${this.domain}" else ""
    }

    companion object {
        @JvmStatic
        open fun fromString(value: String): Email? {
            val parts = value.split("@")
            if (parts.size != 2) {
                return null
            }
            return Email(parts[0], parts[1])
        }

    }
}

public open class EmailWithName private constructor(val login: String, val domain: String, val name: String?) {
    open fun toEmail(): String {
        return Email(this.login, this.domain).asString()
    }

    open fun asString(): String {
        val result = StringBuilder()
        val hasName = this.name != null && this.name.length > 0
        if (hasName) {
            result.add(quote(this.name!!))
        }
        val email = this.toEmail()
        if (email.length > 0) {
            if (hasName) {
                result.add(" <").add(email).add(">")
            } else {
                result.add(email)
            }
        }
        return result.build()
    }

    companion object {
        @JvmStatic
        open fun fromString(params: String): EmailWithName {
            val emailStringIndex: Int = params.indexOf("<")
            if (emailStringIndex != -1) {
                val name = params.substring(0, emailStringIndex - 1).trim()
                val left: Int = if (name.startsWith("\"")) 1 else 0
                val right: Int = if (name.endsWith("\"")) 1 else 0
                val cleanedName = name.substr(left, name.length - left - right)
                val email = Email.fromString(params.substring(emailStringIndex + 1, params.length - 1))!!
                return EmailWithName(email.login, email.domain, cleanedName)
            } else {
                return this.fromNameAndEmail(null, params)
            }
        }

        @JvmStatic
        open fun fromNameAndEmail(name: String?, email: String): EmailWithName {
            val emailObject = Email.fromString(email)
            return if (emailObject == null) EmailWithName("", "", name) else EmailWithName(emailObject.login, emailObject.domain, name)
        }

    }
}

