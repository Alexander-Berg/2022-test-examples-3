import { App, MBTAction, MBTComponent, MBTComponentType } from '../../mbt/mbt-abstractions'
import { MBTComponentActions } from '../../mbt/walk/behaviour/user-behaviour'
import { GoToFolderAction } from '../actions/folder-navigator-actions'
import {
  DeleteSelectedMessages,
  MarkAsReadSelectedMessages,
  MarkAsUnreadSelectedMessages,
  MarkImportantSelectedAction,
  MarkNotSpamSelectedAction,
  MarkSpamSelectedAction,
  MarkUnimportantSelectedAction, MoveToFolderSelectedMessagesAction, UnselectAllMessagesAction, UnselectMessageAction,
} from '../actions/group-mode-actions'
import { RotatableAction } from '../actions/rotatable-actions'
import { FolderNavigatorFeature, GroupModeFeature } from '../mail-features'

export class GroupOperationsComponent implements MBTComponent {
  public static readonly type: MBTComponentType = 'GroupOperationsComponent'

  public assertMatches(model: App, application: App): void {
  }

  public tostring(): string {
    return this.getComponentType()
  }

  public getComponentType(): MBTComponentType {
    return GroupOperationsComponent.type
  }
}

export class AllGroupOperationsActions implements MBTComponentActions {
  public getActions(model: App): MBTAction[] {
    const actions: MBTAction[] = []
    actions.push(new MarkAsReadSelectedMessages())
    actions.push(new MarkAsUnreadSelectedMessages())
    RotatableAction.addActions(actions)
    return actions
  }
}

export class NotImplementedInClientsActions implements MBTComponentActions {
  public getActions(model: App): MBTAction[] {
    const actions: MBTAction[] = []
    actions.push(new DeleteSelectedMessages())
    actions.push(new MarkImportantSelectedAction())
    actions.push(new MarkUnimportantSelectedAction())
    actions.push(new MarkSpamSelectedAction())
    actions.push(new MarkNotSpamSelectedAction())
    FolderNavigatorFeature.get.performIfSupported(model, (mailboxModel) => {
      const folders = mailboxModel.getFoldersList()
      for (const folder of folders) {
        actions.push(new MoveToFolderSelectedMessagesAction(folder.name))
      }
    })
    const groupMode = GroupModeFeature.get.castIfSupported(model)
    if (groupMode !== null) {
      if (groupMode.getSelectedMessages().size !== null) {
        for (const i of groupMode.getSelectedMessages().values()) {
          actions.push(new UnselectMessageAction(i))
        }
      }
    }
    actions.push(new UnselectAllMessagesAction())
    return actions
  }
}
