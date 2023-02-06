// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mbt/base-simple-action.ts >>>

package com.yandex.xplat.testopithecus.common

import com.yandex.xplat.common.YSArray
import com.yandex.xplat.eventus.common.EventusEvent

public abstract class BaseSimpleAction<F, C>(private var type: MBTActionType): MBTAction {
    open override fun supported(modelFeatures: YSArray<FeatureID>, applicationFearures: YSArray<FeatureID>): Boolean {
        return this.requiredFeature().includedAll(modelFeatures, applicationFearures)
    }

    open override fun canBePerformed(model: App): Boolean {
        val featuredModel = this.requiredFeature().forceCast(model)
        return this.canBePerformedImpl(featuredModel)
    }

    open override fun perform(model: App, application: App, history: MBTHistory): MBTComponent {
        val currentComponent = history.currentComponent
        val modelFeature: F = this.requiredFeature().forceCast(model)
        val applicationFeature: F = this.requiredFeature().forceCast(application)
        val component = currentComponent as C
        this.performImpl(modelFeature, component)
        return this.performImpl(applicationFeature, component)
    }

    open fun canBePerformedImpl(_model: F): Boolean {
        return true
    }

    open override fun getActionType(): MBTActionType {
        return this.type
    }

    open override fun tostring(): String {
        return this.getActionType()
    }

    abstract override fun events(): YSArray<EventusEvent>
    abstract fun requiredFeature(): Feature<F>
    abstract fun performImpl(modelOrApplication: F, currentComponent: C): MBTComponent
}
