// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/actions/pin-actions.ts >>>

package com.yandex.xplat.testopithecus

import com.yandex.xplat.common.YSArray
import com.yandex.xplat.common.reverse
import com.yandex.xplat.eventus.common.EventusEvent
import com.yandex.xplat.testopithecus.common.*

public open class TurnOnLoginUsingPasswordAction(protected var password: String): MBTAction {
    open override fun supported(modelFeatures: YSArray<FeatureID>, applicationFeatures: YSArray<FeatureID>): Boolean {
        return PinFeature.`get`.includedAll(modelFeatures, applicationFeatures)
    }

    open override fun canBePerformed(model: App): Boolean {
        val isLoginUsingPasswordEnabled = PinFeature.`get`.forceCast(model).isLoginUsingPasswordEnabled()
        return !isLoginUsingPasswordEnabled
    }

    open override fun perform(model: App, application: App, history: MBTHistory): MBTComponent {
        PinFeature.`get`.forceCast(model).turnOnLoginUsingPassword(this.password)
        PinFeature.`get`.forceCast(application).turnOnLoginUsingPassword(this.password)
        return history.currentComponent
    }

    open override fun events(): YSArray<EventusEvent> {
        return mutableListOf()
    }

    open override fun tostring(): String {
        return "TurnOnLoginUsingPassword"
    }

    open override fun getActionType(): String {
        return TurnOnLoginUsingPasswordAction.type
    }

    companion object {
        @JvmStatic val type: MBTActionType = "TurnOnLoginUsingPasswordAction"
    }
}

public open class TurnOffLoginUsingPasswordAction: MBTAction {
    open override fun supported(modelFeatures: YSArray<FeatureID>, applicationFeatures: YSArray<FeatureID>): Boolean {
        return PinFeature.`get`.includedAll(modelFeatures, applicationFeatures)
    }

    open override fun canBePerformed(model: App): Boolean {
        val isLoginUsingPasswordEnabled = PinFeature.`get`.forceCast(model).isLoginUsingPasswordEnabled()
        return isLoginUsingPasswordEnabled
    }

    open override fun perform(model: App, application: App, history: MBTHistory): MBTComponent {
        PinFeature.`get`.forceCast(model).turnOffLoginUsingPassword()
        PinFeature.`get`.forceCast(application).turnOffLoginUsingPassword()
        return history.currentComponent
    }

    open override fun events(): YSArray<EventusEvent> {
        return mutableListOf()
    }

    open override fun tostring(): String {
        return "TurnOffLoginUsingPassword"
    }

    open override fun getActionType(): String {
        return TurnOffLoginUsingPasswordAction.type
    }

    companion object {
        @JvmStatic val type: MBTActionType = "TurnOffLoginUsingPasswordAction"
    }
}

public open class ChangePasswordAction(protected var newPassword: String): MBTAction {
    open override fun supported(modelFeatures: YSArray<FeatureID>, applicationFeatures: YSArray<FeatureID>): Boolean {
        return PinFeature.`get`.includedAll(modelFeatures, applicationFeatures)
    }

    open override fun canBePerformed(model: App): Boolean {
        val isLoginUsingPasswordEnabled = PinFeature.`get`.forceCast(model).isLoginUsingPasswordEnabled()
        return isLoginUsingPasswordEnabled
    }

    open override fun perform(model: App, application: App, history: MBTHistory): MBTComponent {
        PinFeature.`get`.forceCast(model).changePassword(this.newPassword)
        PinFeature.`get`.forceCast(application).changePassword(this.newPassword)
        return history.currentComponent
    }

    open override fun events(): YSArray<EventusEvent> {
        return mutableListOf()
    }

    open override fun tostring(): String {
        return "ChangePassword"
    }

    open override fun getActionType(): String {
        return ChangePasswordAction.type
    }

    companion object {
        @JvmStatic val type: MBTActionType = "ChangePasswordAction"
    }
}

public open class EnterPasswordAction(protected var password: String): MBTAction {
    open override fun supported(modelFeatures: YSArray<FeatureID>, applicationFeatures: YSArray<FeatureID>): Boolean {
        return PinFeature.`get`.includedAll(modelFeatures, applicationFeatures)
    }

    open override fun canBePerformed(model: App): Boolean {
        val isLoginUsingPasswordEnabled = PinFeature.`get`.forceCast(model).isLoginUsingPasswordEnabled()
        return isLoginUsingPasswordEnabled
    }

    open override fun perform(model: App, application: App, history: MBTHistory): MBTComponent {
        PinFeature.`get`.forceCast(model).enterPassword(this.password)
        PinFeature.`get`.forceCast(application).enterPassword(this.password)
        return this.previousComponentsExceptBackgroundAndPin(history)
    }

    private fun previousComponentsExceptBackgroundAndPin(history: MBTHistory): MBTComponent {
        var previousComponent: MBTComponent? = null
        for (component in history.allPreviousComponents.reverse()) {
            if (!mutableListOf(BackgroundRunningStateComponent.type, PinComponent.type).contains(component.tostring())) {
                previousComponent = component
                break
            }
        }
        return requireNonNull(previousComponent, "No previous component")
    }

    open override fun events(): YSArray<EventusEvent> {
        return mutableListOf()
    }

    open override fun tostring(): String {
        return "EnterPassword"
    }

    open override fun getActionType(): String {
        return EnterPasswordAction.type
    }

    companion object {
        @JvmStatic val type: MBTActionType = "EnterPasswordAction"
    }
}

public open class ResetPasswordAction: MBTAction {
    open override fun supported(modelFeatures: YSArray<FeatureID>, applicationFeatures: YSArray<FeatureID>): Boolean {
        return PinFeature.`get`.includedAll(modelFeatures, applicationFeatures)
    }

    open override fun canBePerformed(model: App): Boolean {
        val isLoginUsingPasswordEnabled = PinFeature.`get`.forceCast(model).isLoginUsingPasswordEnabled()
        return isLoginUsingPasswordEnabled
    }

    open override fun perform(model: App, application: App, history: MBTHistory): MBTComponent {
        PinFeature.`get`.forceCast(model).resetPassword()
        PinFeature.`get`.forceCast(application).resetPassword()
        return LoginComponent()
    }

    open override fun events(): YSArray<EventusEvent> {
        return mutableListOf()
    }

    open override fun tostring(): String {
        return "ResetPassword"
    }

    open override fun getActionType(): String {
        return ResetPasswordAction.type
    }

    companion object {
        @JvmStatic val type: MBTActionType = "ResetPasswordAction"
    }
}

