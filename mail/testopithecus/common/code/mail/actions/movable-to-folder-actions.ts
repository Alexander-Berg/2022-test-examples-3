import { Int32, int64 } from '../../../ys/ys'
import { App, FeatureID, MBTAction, MBTActionType, MBTComponent, MBTHistory } from '../../mbt/mbt-abstractions'
import { Testopithecus } from '../logging/events/testopithecus'
import { TestopithecusEvent } from '../logging/testopithecus-event'
import { FolderNavigatorFeature, MovableToFolderFeature } from '../mail-features'

export class MoveToFolderAction implements MBTAction {
  public static readonly type: MBTActionType = 'MoveToFolder'

  constructor(private order: Int32, private folderName: string) {
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return FolderNavigatorFeature.get.included(modelFeatures)
      && MovableToFolderFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): boolean {
    const folderNavigatorModel = FolderNavigatorFeature.get.forceCast(model)
    const folders = folderNavigatorModel.getFoldersList()
    return folders.filter((folder) => folder.name === this.folderName).length > 0 &&
      folderNavigatorModel.getCurrentFolder().name !== this.folderName
  }

  public events(): TestopithecusEvent[] {
    return [
      Testopithecus.messageListEvents.openMessageActions(this.order, int64(-1)),
      Testopithecus.messageActionsEvents.moveToFolder(),
    ]
  }

  public perform(model: App, application: App, history: MBTHistory): MBTComponent {
    MovableToFolderFeature.get.forceCast(model).moveMessageToFolder(this.order, this.folderName)
    MovableToFolderFeature.get.forceCast(application).moveMessageToFolder(this.order, this.folderName)
    return history.currentComponent
  }

  public tostring(): string {
    return `MovableToFolderAction(${this.order} ${this.folderName})`
  }

  public getActionType(): MBTActionType {
    return MoveToFolderAction.type
  }
}
