import { App, MBTAction, MBTComponent } from '../../mbt/mbt-abstractions'
import { MBTComponentActions } from '../../mbt/walk/behaviour/user-behaviour';
import { GoToFolderAction } from '../actions/folder-navigator-actions';
import { OpenSettingsAction } from '../actions/settings-navigator-actions'
import { FolderNavigatorFeature } from '../mail-features';

export class SettingsComponent implements MBTComponent {
  public static readonly type: string = 'SettingsComponent'

  public assertMatches(model: App, application: App): void {
  }

  public tostring(): string {
    return 'SettingsComponent()'
  }

  public getComponentType(): string {
    return SettingsComponent.type
  }
}

export class AllSettingsActions implements MBTComponentActions {
  public getActions(model: App): MBTAction[] {
    const actions: MBTAction[] = []
    FolderNavigatorFeature.get.performIfSupported(model, (mailboxModel) => {
      const folders = mailboxModel.getFoldersList()
      for (const folder of folders) {
        actions.push(new GoToFolderAction(folder.name))
      }
    })
    actions.push(new OpenSettingsAction())
    return actions
  }
}
