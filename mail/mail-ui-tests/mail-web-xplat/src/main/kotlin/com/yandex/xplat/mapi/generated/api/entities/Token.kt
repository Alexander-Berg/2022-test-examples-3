// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM api/entities/token.ts >>>

package com.yandex.xplat.mapi

public open class Token(private val value: String) {
    open fun asHeaderValue(): String {
        return "OAuth " + this.value
    }

}
