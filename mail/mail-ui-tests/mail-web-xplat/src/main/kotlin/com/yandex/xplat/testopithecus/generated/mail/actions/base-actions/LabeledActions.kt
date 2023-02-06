// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/actions/base-actions/labeled-actions.ts >>>

package com.yandex.xplat.testopithecus

import com.yandex.xplat.common.YSArray
import com.yandex.xplat.common.int64
import com.yandex.xplat.eventus.Eventus
import com.yandex.xplat.eventus.common.EventusEvent
import com.yandex.xplat.testopithecus.common.*

public abstract class BaseLabelAction protected constructor(protected var order: Int, private var type: MBTActionType): MBTAction {
    open override fun supported(modelFeatures: YSArray<FeatureID>, applicationFeatures: YSArray<FeatureID>): Boolean {
        return MarkableImportantFeature.`get`.includedAll(modelFeatures, applicationFeatures)
    }

    open override fun canBePerformed(model: App): Boolean {
        val messageListModel = MessageListDisplayFeature.`get`.forceCast(model)
        val messages = messageListModel.getMessageList(10)
        val canPerform = this.canBePerformedImpl(messages[this.order])
        return this.order < messages.size && canPerform
    }

    open override fun perform(model: App, application: App, history: MBTHistory): MBTComponent {
        this.performImpl(MarkableImportantFeature.`get`.forceCast(model))
        this.performImpl(MarkableImportantFeature.`get`.forceCast(application))
        return history.currentComponent
    }

    open override fun getActionType(): MBTActionType {
        return this.type
    }

    abstract fun canBePerformedImpl(message: MessageView): Boolean
    abstract fun performImpl(modelOrApplication: MarkableImportant): Unit
    abstract override fun events(): YSArray<EventusEvent>
    abstract override fun tostring(): String
}

public open class MarkAsImportant(order: Int): BaseLabelAction(order, MarkAsImportant.type) {
    open override fun canBePerformedImpl(message: MessageView): Boolean {
        return MarkAsImportant.canMarkImportant(message)
    }

    open override fun performImpl(modelOrApplication: MarkableImportant): Unit {
        return modelOrApplication.markAsImportant(this.order)
    }

    open override fun events(): YSArray<EventusEvent> {
        return mutableListOf(Eventus.messageListEvents.openMessageActions(this.order, int64(-1)), Eventus.messageListEvents.markMessageAsImportant(this.order, int64(-1)))
    }

    open override fun tostring(): String {
        return "MarkAsImportant(#${this.order})"
    }

    companion object {
        @JvmStatic val type: MBTActionType = "MarkAsImportant"
        @JvmStatic
        open fun canMarkImportant(message: MessageView): Boolean {
            return !message.important
        }

    }
}

public open class MarkAsUnimportant(order: Int): BaseLabelAction(order, MarkAsUnimportant.type) {
    open override fun canBePerformedImpl(message: MessageView): Boolean {
        return MarkAsUnimportant.canMarkUnimportant(message)
    }

    open override fun performImpl(modelOrApplication: MarkableImportant): Unit {
        return modelOrApplication.markAsUnimportant(this.order)
    }

    open override fun events(): YSArray<EventusEvent> {
        return mutableListOf(Eventus.messageListEvents.openMessageActions(this.order, int64(-1)), Eventus.messageListEvents.markMessageAsNotImportant(this.order, int64(-1)))
    }

    open override fun tostring(): String {
        return "MarkAsUnimportant(#${this.order})"
    }

    companion object {
        @JvmStatic val type: MBTActionType = "MarkAsImportant"
        @JvmStatic
        open fun canMarkUnimportant(message: MessageView): Boolean {
            return message.important
        }

    }
}
