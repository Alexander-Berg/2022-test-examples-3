// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/components/compose-component.ts >>>

import Foundation

open class ComposeComponent: MBTComponent {
  public static let type: MBTComponentType = "ComposeComponent"
  open func assertMatches(_ model: App, _ application: App) {
    let composeModel: ComposeMessage! = ComposeMessageFeature.get.castIfSupported(model)
    let composeApp: ComposeMessage! = ComposeMessageFeature.get.castIfSupported(application)
    if composeModel != nil, composeApp != nil {
      let modelDraft = composeModel.getDraft()
      let appDraft = composeApp.getDraft()
      if !Draft.matches(modelDraft, appDraft) {
        fatalError("Drafts are different, expected =\(modelDraft.tostring()), but actual=\(appDraft.tostring())")
      }
      log("Drafts are equals \(modelDraft.tostring()) \(appDraft.tostring())")
    }
  }

  @discardableResult
  open func tostring() -> String {
    return getComponentType()
  }

  @discardableResult
  open func getComponentType() -> MBTComponentType {
    return ComposeComponent.type
  }
}

open class AllComposeActions: MBTComponentActions {
  @discardableResult
  open func getActions(_ model: App) -> YSArray<MBTAction> {
    let composeModel: ComposeMessage! = ComposeMessageFeature.get.castIfSupported(model)
    let random = PseudoRandomProvider.INSTANCE
    let fuzzer = Fuzzer()
    let actions: YSArray<MBTAction> = YSArray()
    if composeModel != nil {
      let bodySize = composeModel.getDraft().getWysiwyg().getText().length
      actions.push(AppendToBody(0, fuzzer.fuzzyBody(random, 1)))
      if bodySize > 0 {
        actions.push(AppendToBody(random.generate(bodySize), fuzzer.fuzzyBody(random, 1)))
        var randomGenInterval: YSArray<Int32> = randomInterval(random, 0, bodySize)
        actions.push(SetItalic(randomGenInterval[0], randomGenInterval[1]))
        randomGenInterval = randomInterval(random, 0, bodySize)
        actions.push(SetStrong(randomGenInterval[0], randomGenInterval[1]))
        randomGenInterval = randomInterval(random, 0, bodySize)
        actions.push(ClearFormatting(randomGenInterval[0], randomGenInterval[1]))
      }
    }
    actions.push(SendMessageAction(fuzzer.fuzzyValidEmail(), fuzzer.naughtyString(1)))
    actions.push(AddToAction(fuzzer.fuzzyValidEmail()))
    actions.push(SetSubjectAction(fuzzer.fuzzyBody(random, 1)))
    actions.push(SendPreparedAction())
    return actions
  }
}
