// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/actions/compose/write-message-actions.ts >>>

package com.yandex.xplat.testopithecus

import com.yandex.xplat.common.YSArray
import com.yandex.xplat.eventus.Eventus
import com.yandex.xplat.eventus.common.EventusEvent
import com.yandex.xplat.testopithecus.common.*

public abstract class WriteMessageBaseAction(type: MBTActionType): BaseSimpleAction<WriteMessage, MBTComponent>(type) {
    open override fun requiredFeature(): Feature<WriteMessage> {
        return WriteMessageFeature.`get`
    }

}

public open class SendMessageAction(private var to: String, private var subject: String): WriteMessageBaseAction(SendMessageAction.type) {
    open override fun performImpl(modelOrApplication: WriteMessage, _currentComponent: MBTComponent): MBTComponent {
        modelOrApplication.sendMessage(this.to, this.subject)
        return MaillistComponent()
    }

    open override fun events(): YSArray<EventusEvent> {
        return mutableListOf(Eventus.composeEvents.sendMessage())
    }

    open override fun tostring(): String {
        return "SendMessage(to=${this.to}, subject=${this.subject})"
    }

    companion object {
        @JvmStatic val type: MBTActionType = "SendMessage"
    }
}

public open class OpenComposeAction(): WriteMessageBaseAction(OpenComposeAction.type) {
    open override fun performImpl(modelOrApplication: WriteMessage, _currentComponent: MBTComponent): MBTComponent {
        modelOrApplication.openCompose()
        return ComposeComponent()
    }

    open override fun tostring(): String {
        return "OpenCompose"
    }

    open override fun events(): YSArray<EventusEvent> {
        return mutableListOf(Eventus.messageListEvents.writeNewMessage())
    }

    companion object {
        @JvmStatic val type: MBTActionType = "OpenCompose"
    }
}

public open class ReplyMessageAction: MBTAction {
    open override fun supported(modelFeatures: YSArray<FeatureID>, applicationFeatures: YSArray<FeatureID>): Boolean {
        return WriteMessageFeature.`get`.includedAll(modelFeatures, applicationFeatures)
    }

    open override fun canBePerformed(_model: App): Boolean {
        return true
    }

    open override fun perform(model: App, application: App, history: MBTHistory): MBTComponent {
        WriteMessageFeature.`get`.forceCast(model).replyMessage()
        WriteMessageFeature.`get`.forceCast(application).replyMessage()
        val mailListOrMessageView = history.previousDifferentComponent
        if (mailListOrMessageView != null) {
            return mailListOrMessageView!!
        }
        return MaillistComponent()
    }

    open override fun events(): YSArray<EventusEvent> {
        return mutableListOf(Eventus.messageViewEvents.reply(0), Eventus.composeEvents.sendMessage())
    }

    open override fun tostring(): String {
        return "ReplyMessageAction"
    }

    open override fun getActionType(): MBTActionType {
        return ReplyMessageAction.type
    }

    companion object {
        @JvmStatic val type: MBTActionType = "ReplyMessage"
    }
}

public open class SendPreparedAction: MBTAction {
    open override fun supported(modelFeatures: YSArray<FeatureID>, applicationFeatures: YSArray<FeatureID>): Boolean {
        return (ComposeMessageFeature.`get`.included(modelFeatures) && WriteMessageFeature.`get`.includedAll(modelFeatures, applicationFeatures))
    }

    open override fun canBePerformed(model: App): Boolean {
        return ComposeMessageFeature.`get`.forceCast(model).getTo().size > 0
    }

    open override fun perform(model: App, application: App, history: MBTHistory): MBTComponent {
        WriteMessageFeature.`get`.forceCast(model).sendPrepared()
        WriteMessageFeature.`get`.forceCast(application).sendPrepared()
        return requireNonNull(history.previousDifferentComponent, "No previous screen!")
    }

    open override fun events(): YSArray<EventusEvent> {
        return mutableListOf(Eventus.composeEvents.sendMessage())
    }

    open override fun tostring(): String {
        return "SendPreparedAction"
    }

    open override fun getActionType(): MBTActionType {
        return SendMessageAction.type
    }

    companion object {
        @JvmStatic val type: MBTActionType = "SendPrepared"
    }
}

