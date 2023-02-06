import { int64 } from '../../../ys/ys'
import { App, FeatureID, MBTAction, MBTActionType, MBTComponent, MBTHistory } from '../../mbt/mbt-abstractions'
import { UserLock } from '../../users/user-pool'
import { TestopithecusEvent } from '../logging/testopithecus-event'

export class PingAccountLockAction implements MBTAction {
  public static readonly type: MBTActionType = 'PingAccountLock'

  constructor(private accountLock: UserLock) {
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return true
  }

  public canBePerformed(model: App): boolean {
    return true
  }

  public perform(model: App, application: App, history: MBTHistory): MBTComponent {
    this.accountLock.ping(int64(30 * 1000))
    return history.currentComponent
  }

  public events(): TestopithecusEvent[] {
    return []
  }

  public tostring(): string {
    return 'PingAccountLock'
  }

  public getActionType(): MBTActionType {
    return PingAccountLockAction.type
  }
}
