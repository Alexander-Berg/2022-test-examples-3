// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/model/general/rotatable-model.ts >>>

package com.yandex.xplat.testopithecus

import com.yandex.xplat.common.YSArray

public open class RotatableModel: Rotatable {
    var landscape: Boolean = false
    private var listeners: YSArray<RotateListener> = mutableListOf()
    open override fun rotateToLandscape(): Unit {
        this.landscape = true
        this.notifyRotated()
    }

    open override fun rotateToPortrait(): Unit {
        this.landscape = false
        this.notifyRotated()
    }

    open fun copy(): RotatableModel {
        val copy = RotatableModel()
        copy.landscape = this.landscape
        return copy
    }

    open override fun isInLandscape(): Boolean {
        return this.landscape
    }

    open fun attach(listener: RotateListener): Unit {
        this.listeners.add(listener)
    }

    open fun notifyRotated(): Unit {
        for (listener in this.listeners) {
            listener.rotated(this.landscape)
        }
    }

}

public interface RotateListener {
    fun rotated(landscape: Boolean): Unit
}
