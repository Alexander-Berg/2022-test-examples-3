import { BaseSimpleAction } from '../../mbt/base-simple-action'
import { Feature, MBTAction, MBTActionType, MBTComponent } from '../../mbt/mbt-abstractions'
import { Testopithecus } from '../logging/events/testopithecus'
import { TestopithecusEvent } from '../logging/testopithecus-event'
import { Rotatable, RotatableFeature } from '../mail-features'

export abstract class RotatableAction extends BaseSimpleAction<Rotatable, MBTComponent> {
  constructor(type: MBTActionType) {
    super(type)
  }

  public static addActions(actions: MBTAction[]): void {
    actions.push(new RotateToLandscape())
    actions.push(new RotateToPortrait())
  }

  public requiredFeature(): Feature<Rotatable> {
    return RotatableFeature.get
  }

  public events(): TestopithecusEvent[] {
    return [Testopithecus.stubEvent()]
  }

  public performImpl(modelOrApplication: Rotatable, currentComponent: MBTComponent): MBTComponent {
    this.rotate(modelOrApplication)
    return currentComponent
  }

  public abstract rotate(modelOrApplication: Rotatable): void
}

export class RotateToLandscape extends RotatableAction {
  public static readonly type: MBTActionType = 'RotateToLandscape'

  constructor() {
    super(RotateToLandscape.type)
  }

  public canBePerformedImpl(model: Rotatable): boolean {
    return !model.isInLandscape()
  }

  public rotate(modelOrApplication: Rotatable): void {
    modelOrApplication.rotateToLandscape()
  }
}

export class RotateToPortrait extends RotatableAction {
  public static readonly type: MBTActionType = 'RotateToPortrait'

  constructor() {
    super(RotateToPortrait.type)
  }

  public canBePerformedImpl(model: Rotatable): boolean {
    return model.isInLandscape()
  }

  public rotate(modelOrApplication: Rotatable): void {
    modelOrApplication.rotateToPortrait()
  }
}
