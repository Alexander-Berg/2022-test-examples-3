// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/actions/settings/general-settings-actions.ts >>>

package com.yandex.xplat.testopithecus

import com.yandex.xplat.common.YSArray
import com.yandex.xplat.eventus.Eventus
import com.yandex.xplat.eventus.common.EventusEvent
import com.yandex.xplat.testopithecus.common.*

public open class ClearCache: MBTAction {
    open override fun supported(modelFeatures: YSArray<FeatureID>, applicationFeatures: YSArray<FeatureID>): Boolean {
        return GeneralSettingsFeature.`get`.includedAll(modelFeatures, applicationFeatures)
    }

    open override fun canBePerformed(model: App): Boolean {
        return true
    }

    open override fun perform(model: App, application: App, history: MBTHistory): MBTComponent {
        GeneralSettingsFeature.`get`.forceCast(model).clearCache()
        GeneralSettingsFeature.`get`.forceCast(application).clearCache()
        return history.currentComponent
    }

    open override fun events(): YSArray<EventusEvent> {
        return mutableListOf(Eventus.settingsEvents.clearCache())
    }

    open override fun tostring(): String {
        return "ClearCache"
    }

    open override fun getActionType(): String {
        return ClearCache.type
    }

    companion object {
        @JvmStatic val type: MBTActionType = "ClearCache"
    }
}

public open class OpenGeneralSettingsAction: MBTAction {
    open override fun supported(modelFeatures: YSArray<FeatureID>, applicationFeatures: YSArray<FeatureID>): Boolean {
        return GeneralSettingsFeature.`get`.includedAll(modelFeatures, applicationFeatures)
    }

    open override fun canBePerformed(model: App): Boolean {
        return true
    }

    open override fun perform(model: App, application: App, history: MBTHistory): MBTComponent {
        GeneralSettingsFeature.`get`.forceCast(model).openGeneralSettings()
        GeneralSettingsFeature.`get`.forceCast(application).openGeneralSettings()
        return GeneralSettingsComponent()
    }

    open override fun events(): YSArray<EventusEvent> {
        return mutableListOf()
    }

    open override fun tostring(): String {
        return "OpenGeneralSettings"
    }

    open override fun getActionType(): String {
        return OpenGeneralSettingsAction.type
    }

    companion object {
        @JvmStatic val type: MBTActionType = "OpenGeneralSettings"
    }
}

public open class CloseGeneralSettingsAction: MBTAction {
    open override fun supported(modelFeatures: YSArray<FeatureID>, applicationFeatures: YSArray<FeatureID>): Boolean {
        return GeneralSettingsFeature.`get`.includedAll(modelFeatures, applicationFeatures)
    }

    open override fun canBePerformed(model: App): Boolean {
        return true
    }

    open override fun perform(model: App, application: App, history: MBTHistory): MBTComponent {
        GeneralSettingsFeature.`get`.forceCast(model).closeGeneralSettings()
        GeneralSettingsFeature.`get`.forceCast(application).closeGeneralSettings()
        return RootSettingsComponent()
    }

    open override fun events(): YSArray<EventusEvent> {
        return mutableListOf()
    }

    open override fun getActionType(): String {
        return CloseGeneralSettingsAction.type
    }

    open override fun tostring(): String {
        return "CloseGeneralSettings"
    }

    companion object {
        @JvmStatic val type: MBTActionType = "CloseGeneralSettings"
    }
}

public open class SetActionOnSwipe(private var action: ActionOnSwipe): MBTAction {
    open override fun supported(modelFeatures: YSArray<FeatureID>, applicationFeatures: YSArray<FeatureID>): Boolean {
        return GeneralSettingsFeature.`get`.includedAll(modelFeatures, applicationFeatures)
    }

    open override fun canBePerformed(model: App): Boolean {
        return true
    }

    open override fun perform(model: App, application: App, history: MBTHistory): MBTComponent {
        GeneralSettingsFeature.`get`.forceCast(model).setActionOnSwipe(this.action)
        GeneralSettingsFeature.`get`.forceCast(application).setActionOnSwipe(this.action)
        return history.currentComponent
    }

    open override fun events(): YSArray<EventusEvent> {
        return mutableListOf()
    }

    open override fun tostring(): String {
        return "Set ${this.action} action on swipe"
    }

    open override fun getActionType(): String {
        return SetActionOnSwipe.type
    }

    companion object {
        @JvmStatic val type: MBTActionType = "SetActionOnSwipe"
    }
}

public open class TurnOnCompactMode: MBTAction {
    open override fun supported(modelFeatures: YSArray<FeatureID>, applicationFeatures: YSArray<FeatureID>): Boolean {
        return GeneralSettingsFeature.`get`.includedAll(modelFeatures, applicationFeatures)
    }

    open override fun canBePerformed(model: App): Boolean {
        val isCompactModeEnabled = GeneralSettingsFeature.`get`.forceCast(model).isCompactModeEnabled()
        return !isCompactModeEnabled
    }

    open override fun perform(model: App, application: App, history: MBTHistory): MBTComponent {
        GeneralSettingsFeature.`get`.forceCast(model).switchCompactMode()
        GeneralSettingsFeature.`get`.forceCast(application).switchCompactMode()
        return history.currentComponent
    }

    open override fun events(): YSArray<EventusEvent> {
        return mutableListOf(Eventus.settingsEvents.toggleCompactMode(true))
    }

    open override fun tostring(): String {
        return "Turn on compact mode"
    }

    open override fun getActionType(): String {
        return TurnOnCompactMode.type
    }

    companion object {
        @JvmStatic val type: MBTActionType = "TurnOnCompactMode"
    }
}

public open class TurnOffCompactMode: MBTAction {
    open override fun supported(modelFeatures: YSArray<FeatureID>, applicationFeatures: YSArray<FeatureID>): Boolean {
        return GeneralSettingsFeature.`get`.includedAll(modelFeatures, applicationFeatures)
    }

    open override fun canBePerformed(model: App): Boolean {
        return GeneralSettingsFeature.`get`.forceCast(model).isCompactModeEnabled()
    }

    open override fun perform(model: App, application: App, history: MBTHistory): MBTComponent {
        GeneralSettingsFeature.`get`.forceCast(model).switchCompactMode()
        GeneralSettingsFeature.`get`.forceCast(application).switchCompactMode()
        return history.currentComponent
    }

    open override fun events(): YSArray<EventusEvent> {
        return mutableListOf(Eventus.settingsEvents.toggleCompactMode(false))
    }

    open override fun tostring(): String {
        return "Turn off compact mode"
    }

    open override fun getActionType(): String {
        return TurnOffCompactMode.type
    }

    companion object {
        @JvmStatic val type: MBTActionType = "TurnOffCompactMode"
    }
}
