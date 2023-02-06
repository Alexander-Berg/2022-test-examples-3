import { Int32 } from '../../../ys/ys'
import { Fuzzer } from '../../fuzzing/fuzzer'
import { App, MBTAction, MBTComponent, MBTComponentType } from '../../mbt/mbt-abstractions'
import { MBTComponentActions } from '../../mbt/walk/behaviour/user-behaviour'
import { log } from '../../utils/logger'
import { PseudoRandomProvider } from '../../utils/pseudo-random'
import { randomInterval } from '../../utils/random'
import {
  AddToAction,
  SetBodyAction,
  SetSubjectAction,
} from '../actions/compose-message-actions'
import {
  SendMessageAction, SendPreparedAction,
} from '../actions/write-message-actions'
import { AppendToBody, ClearFormatting, SetItalic, SetStrong } from '../actions/wysiwyg-actions'
import { ComposeMessageFeature } from '../mail-features'
import { Draft } from '../model/compose-message-model'

export class ComposeComponent implements MBTComponent {
  public static readonly type: MBTComponentType = 'ComposeComponent'

  public assertMatches(model: App, application: App): void {
    const composeModel = ComposeMessageFeature.get.castIfSupported(model)
    const composeApp = ComposeMessageFeature.get.castIfSupported(application)
    if (composeModel !== null && composeApp !== null) {
      const modelDraft = composeModel.getDraft()
      const appDraft = composeApp.getDraft()
      if (!Draft.matches(modelDraft, appDraft)) {
        throw new Error(`Drafts are different, expected =${modelDraft.tostring()}, but actual=${appDraft.tostring()}`)
      }
      log(`Drafts are equals ${modelDraft.tostring()} ${appDraft.tostring()}`)
    }
  }

  public tostring(): string {
    return this.getComponentType()
  }

  public getComponentType(): MBTComponentType {
    return ComposeComponent.type
  }
}

export class AllComposeActions implements MBTComponentActions {
  public getActions(model: App): MBTAction[] {
    const composeModel = ComposeMessageFeature.get.castIfSupported(model)
    const random = PseudoRandomProvider.INSTANCE
    const fuzzer = new Fuzzer()
    const actions: MBTAction[] = []
    if (composeModel !== null) {
      const bodySize = composeModel.getDraft().getWysiwyg().getText().length
      actions.push(new AppendToBody(0, fuzzer.fuzzyBody(random, 1)))
      if (bodySize > 0) {
        actions.push(new AppendToBody(random.generate(bodySize), fuzzer.fuzzyBody(random, 1)))
        let randomGenInterval: Int32[] = randomInterval(random, 0, bodySize)
        actions.push(new SetItalic(randomGenInterval[0], randomGenInterval[1]))
        randomGenInterval = randomInterval(random, 0, bodySize)
        actions.push(new SetStrong(randomGenInterval[0], randomGenInterval[1]))
        randomGenInterval = randomInterval(random, 0, bodySize)
        actions.push(new ClearFormatting(randomGenInterval[0], randomGenInterval[1]))
      }
    }
    actions.push(new SendMessageAction(fuzzer.fuzzyValidEmail(), fuzzer.naughtyString(1)))
    actions.push(new AddToAction(fuzzer.fuzzyValidEmail()))
    actions.push(new SetSubjectAction(fuzzer.fuzzyBody(random, 1)))
    actions.push(new SendPreparedAction())
    // actions.push(new SetBodyAction(fuzzer.naughtyString(1))) TODO: android chromedriver
    return actions
  }
}
