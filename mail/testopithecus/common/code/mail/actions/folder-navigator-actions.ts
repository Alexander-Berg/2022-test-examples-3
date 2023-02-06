import { App, FeatureID, MBTAction, MBTActionType, MBTComponent, MBTHistory } from '../../mbt/mbt-abstractions'
import { MaillistComponent } from '../components/maillist-component'
import { Testopithecus } from '../logging/events/testopithecus'
import { TestopithecusEvent } from '../logging/testopithecus-event'
import { CreatableFolderFeature, FolderNavigatorFeature } from '../mail-features'

export class GoToFolderAction implements MBTAction {
  public static readonly type: MBTActionType = 'GoToFolder'

  constructor(private folderName: string) {
  }

  public canBePerformed(model: App): boolean {
    const folderNavigatorModel = FolderNavigatorFeature.get.forceCast(model)
    const folders = folderNavigatorModel.getFoldersList()
    return folders.filter((folder) => folder.name === this.folderName).length > 0
  }

  public events(): TestopithecusEvent[] {
    return [Testopithecus.stubEvent()]
  }

  public perform(model: App, application: App, history: MBTHistory): MBTComponent {
    FolderNavigatorFeature.get.forceCast(model).goToFolder(this.folderName)
    FolderNavigatorFeature.get.forceCast(application).goToFolder(this.folderName)
    return new MaillistComponent()
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return FolderNavigatorFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public getActionType(): MBTActionType {
    return GoToFolderAction.type
  }

  public tostring(): string {
    return `GoToFolder(${this.folderName})`
  }
}

export class CreateFolderAction implements MBTAction {
  public static readonly type: MBTActionType = 'CreateFolder'

  constructor(private folderName: string) {
  }

  public canBePerformed(model: App): boolean {
    const folderNavigatorModel = FolderNavigatorFeature.get.forceCast(model)
    const folders = folderNavigatorModel.getFoldersList()
    return folders.filter((folder) => folder.name === this.folderName).length === 0
  }

  public events(): TestopithecusEvent[] {
    return [Testopithecus.stubEvent()]
  }

  public perform(model: App, application: App, history: MBTHistory): MBTComponent {
    CreatableFolderFeature.get.forceCast(model).createFolder(this.folderName)
    CreatableFolderFeature.get.forceCast(application).createFolder(this.folderName)
    return history.currentComponent
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return FolderNavigatorFeature.get.includedAll(modelFeatures, applicationFeatures) &&
      CreatableFolderFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public getActionType(): MBTActionType {
    return CreateFolderAction.type
  }

  public tostring(): string {
    return `CreateFolder(${this.folderName})`
  }
}
