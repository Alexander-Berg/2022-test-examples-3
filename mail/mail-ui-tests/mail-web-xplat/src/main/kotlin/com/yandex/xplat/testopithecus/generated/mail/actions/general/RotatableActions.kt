// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/actions/general/rotatable-actions.ts >>>

package com.yandex.xplat.testopithecus

import com.yandex.xplat.common.YSArray
import com.yandex.xplat.eventus.common.EventusEvent
import com.yandex.xplat.testopithecus.common.*

public abstract class RotatableAction(type: MBTActionType): BaseSimpleAction<Rotatable, MBTComponent>(type) {
    open override fun requiredFeature(): Feature<Rotatable> {
        return RotatableFeature.`get`
    }

    open override fun events(): YSArray<EventusEvent> {
        return mutableListOf()
    }

    open override fun performImpl(modelOrApplication: Rotatable, currentComponent: MBTComponent): MBTComponent {
        this.rotate(modelOrApplication)
        return currentComponent
    }

    abstract fun rotate(modelOrApplication: Rotatable): Unit
    companion object {
        @JvmStatic
        open fun addActions(actions: YSArray<MBTAction>): Unit {
            actions.add(RotateToLandscape())
            actions.add(RotateToPortrait())
        }

    }
}

public open class RotateToLandscape(): RotatableAction(RotateToLandscape.type) {
    open override fun canBePerformedImpl(model: Rotatable): Boolean {
        val isInLandscape = model.isInLandscape()
        return !isInLandscape
    }

    open override fun rotate(modelOrApplication: Rotatable): Unit {
        modelOrApplication.rotateToLandscape()
    }

    companion object {
        @JvmStatic val type: MBTActionType = "RotateToLandscape"
    }
}

public open class RotateToPortrait(): RotatableAction(RotateToPortrait.type) {
    open override fun canBePerformedImpl(model: Rotatable): Boolean {
        val isInLandscape = model.isInLandscape()
        return isInLandscape
    }

    open override fun rotate(modelOrApplication: Rotatable): Unit {
        modelOrApplication.rotateToPortrait()
    }

    companion object {
        @JvmStatic val type: MBTActionType = "RotateToPortrait"
    }
}
