// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/components/settings-component.ts >>>

import Foundation

open class SettingsComponent: MBTComponent {
  public static let type: String = "SettingsComponent"
  open func assertMatches(_: App, _: App) {}

  @discardableResult
  open func tostring() -> String {
    return "SettingsComponent()"
  }

  @discardableResult
  open func getComponentType() -> String {
    return SettingsComponent.type
  }
}

open class AllSettingsActions: MBTComponentActions {
  @discardableResult
  open func getActions(_ model: App) -> YSArray<MBTAction> {
    let actions: YSArray<MBTAction> = YSArray()
    FolderNavigatorFeature.get.performIfSupported(model) {
      mailboxModel in
      let folders = mailboxModel.getFoldersList()
      for folder in folders {
        actions.push(GoToFolderAction(folder.name))
      }
    }
    actions.push(OpenSettingsAction())
    return actions
  }
}