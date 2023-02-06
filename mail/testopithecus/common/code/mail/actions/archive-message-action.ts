import { Int32, int64 } from '../../../ys/ys'
import { App, FeatureID, MBTAction, MBTActionType, MBTComponent, MBTHistory } from '../../mbt/mbt-abstractions';
import { Testopithecus } from '../logging/events/testopithecus';
import { TestopithecusEvent } from '../logging/testopithecus-event';
import {
  ArchiveMessageFeature,
  FolderNavigatorFeature,
  MessageListDisplayFeature
} from '../mail-features';
import { DefaultFolderName} from '../model/mail-model';

export class ArchiveMessageAction implements MBTAction {
  public static readonly type: MBTActionType = 'ArchiveMessage'

  constructor(private order: Int32) {
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return MessageListDisplayFeature.get.included(modelFeatures)
      && FolderNavigatorFeature.get.included(modelFeatures)
      && ArchiveMessageFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): boolean {
    const messageListDisplayModel = MessageListDisplayFeature.get.forceCast(model)
    const messages = messageListDisplayModel.getMessageList(this.order + 1)
    const currentFolder = FolderNavigatorFeature.get.forceCast(model).getCurrentFolder()
    return currentFolder.name !== DefaultFolderName.archive && this.order < messages.length
  }

  public perform(model: App, application: App, history: MBTHistory): MBTComponent {
    ArchiveMessageFeature.get.forceCast(model).archiveMessage(this.order)
    ArchiveMessageFeature.get.forceCast(application).archiveMessage(this.order)
    return history.currentComponent
  }

  public getActionType(): MBTActionType {
    return ArchiveMessageAction.type
  }

  public tostring(): string {
    return `MoveToArchive(#${this.order})`
  }

  public events(): TestopithecusEvent[] {
    return [
      Testopithecus.messageListEvents.openMessageActions(this.order, int64(-1)),
      Testopithecus.messageActionsEvents.archive(),
    ]
  }
}
