import { Int32, Nullable } from '../../ys/ys'
import { fakeActions } from '../mail/actions/fake-actions'
import { Logger } from '../utils/logger'
import { App, FeatureID, MBTAction, MBTComponent, MBTHistory } from './mbt-abstractions'

export class StateMachine {
  constructor(private model: App,
              private application: App,
              private walkStrategy: WalkStrategy,
              private logger: Logger) {
  }

  public go(start: MBTComponent, limit: Nullable<Int32> = null): boolean {
    let currentComponent: Nullable<MBTComponent> = start
    const history = new HistoryBuilder(start)
    let i: Int32 = 0
    while (currentComponent !== null) {
      currentComponent = this.step(history)
      if (currentComponent !== null) {
        history.next(currentComponent!)
      }
      i += 1
      if (i === limit) {
        break
      }
    }
    return i > 1
  }

  public step(history: MBTHistory): Nullable<MBTComponent> {
    const current = history.currentComponent

    const action = this.walkStrategy.nextAction(this.model, this.application.supportedFeatures, current)
    if (action === null) {
      this.logger.log(`No possible actions available`)
      return null
    }

    this.logAction(action, '==========')

    // В теории, nextAction может вернуть вообще произвольное действие, не обязательно из списка possibleActions.
    // Например, в классических тестах при фиксированном сценарии.
    // Поэтому требуются следующие две проверки.
    if (!action.supported(this.model.supportedFeatures, this.application.supportedFeatures)) {
      throw new Error(`Can\'t perform ${action.tostring()}, because application doesn\'t support it`)
    }
    if (!action.canBePerformed(this.model)) {
      throw new Error(`Can\'t perform ${action.tostring()}, because it can\'t be performed in current model state`)
    }

    this.logAction(action, `Performing action ${action.tostring()} on current component ${current.tostring()}`)
    const nextComponent = action.perform(this.model, this.application, history)
    this.logAction(action, `Action ${action.tostring()} on component ${current.tostring()} performed, new component is ${nextComponent.tostring()}`)
    this.logAction(action, '==========\n')
    return nextComponent
  }

  private logAction(action: MBTAction, message: string): void {
    if (!fakeActions().includes(action.getActionType())) {
      this.logger.log(message)
    }
  }
}

export class HistoryBuilder implements MBTHistory {
  public previousDifferentComponent: Nullable<MBTComponent> = null

  constructor(public currentComponent: MBTComponent) {
  }

  public next(component: MBTComponent): HistoryBuilder {
    if (this.currentComponent.tostring() !== component.tostring()) {
      this.previousDifferentComponent = this.currentComponent
      this.currentComponent = component
    }
    return this
  }
}

export interface WalkStrategy {
  nextAction(model: App, applicationFeatures: FeatureID[], component: MBTComponent): Nullable<MBTAction>
}
