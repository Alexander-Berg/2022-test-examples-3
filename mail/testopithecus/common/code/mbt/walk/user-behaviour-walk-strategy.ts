import { Int32, Nullable, undefinedToNull } from '../../../ys/ys'
import { MaillistComponent } from '../../mail/components/maillist-component'
import { PseudoRandomProvider } from '../../utils/pseudo-random'
import { RandomProvider } from '../../utils/random'
import { Registry } from '../../utils/registry'
import { App, FeatureID, MBTAction, MBTComponent, MBTComponentType } from '../mbt-abstractions'
import { WalkStrategy } from '../state-machine'
import { UserBehaviour } from './behaviour/user-behaviour'

export class UserBehaviourWalkStrategy implements WalkStrategy {
  public readonly history: MBTAction[] = []
  private currentStep: Int32 = 0

  constructor(private behaviour: UserBehaviour,
              private chooser: ActionChooser = new RandomActionChooser(),
              private stepsLimit: Nullable<Int32> = null) {
  }

  public nextAction(model: App, applicationFeatures: FeatureID[], component: MBTComponent): Nullable<MBTAction> {
    const possibleActions = this.behaviour.getActions(model, component)
      .filter((mbtAction) => mbtAction.supported(model.supportedFeatures, applicationFeatures))
      .filter((mbtAction) => mbtAction.canBePerformed(model))
    if (possibleActions.length === 0) {
      return null
    }
    if (this.currentStep === this.stepsLimit) {
      return null
    }
    const action = this.chooser.choose(possibleActions, component)
    if (action === null) {
      return null
    }
    this.history.push(action)
    this.currentStep += 1
    return action
  }
}

export interface ActionChooser {
  choose(actions: MBTAction[], component: MBTComponent): Nullable<MBTAction>
}

export class RandomActionChooser implements ActionChooser {
  constructor(private random: RandomProvider = PseudoRandomProvider.INSTANCE) {
  }

  public choose(actions: MBTAction[], component: MBTComponent): Nullable<MBTAction> {
    if (actions.length === 0) {
      return null
    }
    const order = this.random.generate(actions.length)
    return actions[order]
  }
}
