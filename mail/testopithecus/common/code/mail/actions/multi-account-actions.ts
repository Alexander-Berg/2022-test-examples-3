import { App, FeatureID, MBTAction, MBTActionType, MBTComponent, MBTHistory } from '../../mbt/mbt-abstractions'
import { LoginComponent } from '../components/login-component'
import { MaillistComponent } from '../components/maillist-component'
import { Testopithecus } from '../logging/events/testopithecus'
import { TestopithecusEvent } from '../logging/testopithecus-event'
import { MultiAccountFeature} from '../mail-features'

export abstract class MultiAccountAction implements MBTAction {
  protected constructor() {}

  public events(): TestopithecusEvent[] {
    return [Testopithecus.stubEvent()]
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return MultiAccountFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public abstract perform(model: App, application: App, history: MBTHistory): MBTComponent

  public abstract canBePerformed(model: App): boolean

  public abstract tostring(): string

  public abstract getActionType(): string
}

export class SwitchAccountAction extends MultiAccountAction {
  public static readonly type: MBTActionType = 'SwitchAccount'

  public constructor(private login: string) {
    super()
  }

  public canBePerformed(model: App): boolean {
    return MultiAccountFeature.get.forceCast(model).getLoggedInAccountsList().includes(this.login)
  }
  public perform(model: App, application: App, history: MBTHistory): MBTComponent {
    this.performImpl(model)
    this.performImpl(application)
    return new MaillistComponent()
  }

  public tostring(): string {
    return `SwitchAccountAction(login=${this.login})`
  }

  public getActionType(): string {
    return SwitchAccountAction.type
  }

  private performImpl(modelOrApplication: App): void {
    MultiAccountFeature.get.forceCast(modelOrApplication).switchToAccount(this.login)
  }
}

export class AddNewAccountAction extends MultiAccountAction {
  public static readonly type: MBTActionType = 'AddNewAccount'

  constructor() {
    super()
  }

  public canBePerformed(model: App): boolean {
    return true
  }

  public perform(model: App, application: App, history: MBTHistory): MBTComponent {
    this.performImpl(model)
    this.performImpl(application)
    return new LoginComponent()
  }

  public tostring(): string {
    return `AddNewAccountAction`
  }

  public getActionType(): string {
    return AddNewAccountAction.type
  }

  private performImpl(modelOrApplication: App): void {
    MultiAccountFeature.get.forceCast(modelOrApplication).addNewAccount()
  }
}
