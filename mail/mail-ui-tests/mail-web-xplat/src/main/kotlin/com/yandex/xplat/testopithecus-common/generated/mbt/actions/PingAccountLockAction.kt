// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mbt/actions/ping-account-lock-action.ts >>>

package com.yandex.xplat.testopithecus.common

import com.yandex.xplat.common.YSArray
import com.yandex.xplat.common.int64
import com.yandex.xplat.eventus.common.EventusEvent

public open class PingAccountLockAction(private var accountLock: UserLock): MBTAction {
    open override fun supported(_modelFeatures: YSArray<FeatureID>, _applicationFeatures: YSArray<FeatureID>): Boolean {
        return true
    }

    open override fun canBePerformed(_model: App): Boolean {
        return true
    }

    open override fun perform(_model: App, _application: App, history: MBTHistory): MBTComponent {
        this.accountLock.ping(int64(30 * 1000))
        return history.currentComponent
    }

    open override fun events(): YSArray<EventusEvent> {
        return mutableListOf()
    }

    open override fun tostring(): String {
        return "PingAccountLock"
    }

    open override fun getActionType(): MBTActionType {
        return PingAccountLockAction.type
    }

    companion object {
        @JvmStatic val type: MBTActionType = "PingAccountLock"
    }
}
