// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/components/folder-list-component.ts >>>

package com.yandex.xplat.testopithecus

import com.yandex.xplat.common.YSArray
import com.yandex.xplat.common.map
import com.yandex.xplat.common.reverse
import com.yandex.xplat.common.split
import com.yandex.xplat.testopithecus.common.*

public open class FolderListComponent: MBTComponent {
    open override fun getComponentType(): String {
        return FolderListComponent.type
    }

    open override fun assertMatches(model: App, application: App): Unit {
        val folderNavigatorModel = FolderNavigatorFeature.`get`.castIfSupported(model)
        val folderNavigatorApplication = FolderNavigatorFeature.`get`.castIfSupported(application)
        if (folderNavigatorModel != null && folderNavigatorApplication != null) {
            val folderListModel = folderNavigatorModel.getFoldersList().map( {
                folder ->
                folder.split("|").reverse()[0]
            })
            val folderListApplication = folderNavigatorApplication.getFoldersList().map( {
                folder ->
                folder.split("|").reverse()[0]
            })
            assertInt32Equals(folderListModel.size, folderListApplication.size, "Different number of folders")
            for (folder in folderListModel) {
                assertBooleanEquals(true, folderListApplication.contains(folder), "Missing folder ${folder}")
            }
        }
        val labelNavigatorModel = LabelNavigatorFeature.`get`.castIfSupported(model)
        val labelNavigatorApplication = LabelNavigatorFeature.`get`.castIfSupported(application)
        if (labelNavigatorModel != null && labelNavigatorApplication != null) {
            val labelListModel = labelNavigatorModel.getLabelList()
            val labelListApplication = labelNavigatorApplication.getLabelList()
            assertInt32Equals(labelListModel.size, labelListApplication.size, "Different number of labels")
            for (label in labelListModel) {
                assertBooleanEquals(true, labelListApplication.contains(label), "Missing label ${label}")
            }
        }
    }

    open override fun tostring(): String {
        return this.getComponentType()
    }

    companion object {
        @JvmStatic val type: String = "FolderListComponent"
    }
}

public open class FolderListActions: MBTComponentActions {
    open override fun getActions(_model: App): YSArray<MBTAction> {
        val actions: YSArray<MBTAction> = mutableListOf()
        return actions
    }

}

