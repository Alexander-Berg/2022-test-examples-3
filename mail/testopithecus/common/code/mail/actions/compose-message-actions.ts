import { BaseSimpleAction } from '../../mbt/base-simple-action'
import { Feature, MBTActionType, MBTComponent } from '../../mbt/mbt-abstractions'
import { Testopithecus } from '../logging/events/testopithecus'
import { TestopithecusEvent } from '../logging/testopithecus-event'
import { ComposeMessage, ComposeMessageFeature } from '../mail-features'

export abstract class ComposeMessageBaseAction extends BaseSimpleAction<ComposeMessage, MBTComponent> {
  constructor(type: MBTActionType) {
    super(type)
  }

  public requiredFeature(): Feature<ComposeMessage> {
    return ComposeMessageFeature.get
  }

  public abstract events(): TestopithecusEvent[]
}

export class AddToAction extends ComposeMessageBaseAction {
  public static readonly type: MBTActionType = 'AddTo'

  constructor(private to: MBTActionType) {
    super(AddToAction.type)
  }

  public performImpl(modelOrApplication: ComposeMessage, currentComponent: MBTComponent): MBTComponent {
    modelOrApplication.addTo(this.to)
    return currentComponent
  }

  public events(): TestopithecusEvent[] {
    return [Testopithecus.composeEvents.addReceiver()]
  }

  public tostring(): string {
    return `AddToAction(to=${this.to})`
  }
}

export class SetSubjectAction extends ComposeMessageBaseAction {
  public static readonly type: MBTActionType = 'SetSubject'

  constructor(private subject: MBTActionType) {
    super(SetSubjectAction.type)
  }

  public performImpl(modelOrApplication: ComposeMessage, currentComponent: MBTComponent): MBTComponent {
    modelOrApplication.setSubject(this.subject)
    return currentComponent
  }

  public events(): TestopithecusEvent[] {
    return [Testopithecus.composeEvents.setSubject()]
  }

  public tostring(): string {
    return `AddSubjectAction(subject=${this.subject})`
  }
}

export class SetBodyAction extends ComposeMessageBaseAction {
  public static readonly type: MBTActionType = 'SetBody'

  constructor(private body: MBTActionType) {
    super(SetBodyAction.type)
  }

  public performImpl(modelOrApplication: ComposeMessage, currentComponent: MBTComponent): MBTComponent {
    modelOrApplication.setBody(this.body)
    return currentComponent
  }

  public events(): TestopithecusEvent[] {
    return [Testopithecus.composeEvents.editBody(this.body.length)]
  }

  public tostring(): string {
    return `AppendBodyAction(bodyFragment=${this.body})`
  }
}

export class AddToFromSuggestAction extends ComposeMessageBaseAction {
  public static readonly type: MBTActionType = 'AddToFromSuggest'

  constructor(private to: MBTActionType) {
    super(AddToFromSuggestAction.type)
  }

  public performImpl(modelOrApplication: ComposeMessage, currentComponent: MBTComponent): MBTComponent {
    modelOrApplication.addToUsingSuggest(this.to)
    return currentComponent
  }

  public events(): TestopithecusEvent[] {
    return [Testopithecus.composeEvents.addReceiver()] // TODO
  }

  public tostring(): string {
    return `AddToFromSuggestAction(to=${this.to})`
  }
}
