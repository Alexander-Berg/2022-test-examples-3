// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/actions/base-actions/movable-to-folder-actions.ts >>>

package com.yandex.xplat.testopithecus

import com.yandex.xplat.common.YSArray
import com.yandex.xplat.common.filter
import com.yandex.xplat.common.int64
import com.yandex.xplat.eventus.Eventus
import com.yandex.xplat.eventus.common.EventusEvent
import com.yandex.xplat.testopithecus.common.*

public open class MoveToFolderAction(private var order: Int, private var folderName: FolderName): MBTAction {
    open override fun supported(modelFeatures: YSArray<FeatureID>, applicationFeatures: YSArray<FeatureID>): Boolean {
        return (FolderNavigatorFeature.`get`.included(modelFeatures) && MovableToFolderFeature.`get`.includedAll(modelFeatures, applicationFeatures) && ContainerGetterFeature.`get`.included(modelFeatures))
    }

    open override fun canBePerformed(model: App): Boolean {
        val folderNavigatorModel = FolderNavigatorFeature.`get`.forceCast(model)
        val folders = folderNavigatorModel.getFoldersList()
        val containerGetterModel = ContainerGetterFeature.`get`.forceCast(model)
        val currentContainer = containerGetterModel.getCurrentContainer()
        return (folders.filter( {
            folder ->
            folder == this.folderName
        }).size > 0 && currentContainer.type == MessageContainerType.folder && currentContainer.name != this.folderName)
    }

    open override fun events(): YSArray<EventusEvent> {
        return mutableListOf(Eventus.messageListEvents.openMessageActions(this.order, int64(-1)), Eventus.messageListEvents.moveMessageToFolder(this.order, int64(-1)))
    }

    open override fun perform(model: App, application: App, history: MBTHistory): MBTComponent {
        MovableToFolderFeature.`get`.forceCast(model).moveMessageToFolder(this.order, this.folderName)
        MovableToFolderFeature.`get`.forceCast(application).moveMessageToFolder(this.order, this.folderName)
        return history.currentComponent
    }

    open override fun tostring(): String {
        return "MovableToFolderAction(${this.order} ${this.folderName})"
    }

    open override fun getActionType(): MBTActionType {
        return MoveToFolderAction.type
    }

    companion object {
        @JvmStatic val type: MBTActionType = "MoveToFolder"
    }
}

