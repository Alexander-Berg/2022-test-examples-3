import { App, Feature, FeatureID, MBTAction, MBTComponent, MBTHistory } from '../../mbt/mbt-abstractions'
import { UserAccount } from '../../users/user-pool'
import { MaillistComponent } from '../components/maillist-component'
import { Testopithecus } from '../logging/events/testopithecus'
import { TestopithecusEvent } from '../logging/testopithecus-event'
import {
  CustomMailServiceLogin,
  CustomMailServiceLoginFeature, GoogleLogin,
  GoogleLoginFeature, HotmailLogin,
  HotmailLoginFeature, MailRuLogin,
  MailRuLoginFeature, OutlookLogin,
  OutlookLoginFeature, RamblerLogin,
  RamblerLoginFeature, YahooLogin,
  YahooLoginFeature, YandexLogin,
  YandexLoginFeature,
} from '../mail-features'

export abstract class LoginAction<T> implements MBTAction {
  protected constructor(protected account: UserAccount, protected feature: Feature<T>) {
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return this.feature.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): boolean {
    return true
  }

  public events(): TestopithecusEvent[] {
    return [Testopithecus.startEvents.startWithMessageListShow()]
  }

  public perform(model: App, application: App, history: MBTHistory): MBTComponent {
    this.performImpl(this.feature.forceCast(model))
    this.performImpl(this.feature.forceCast(application))
    return new MaillistComponent()
  }

  public tostring(): string {
    return `${this.getActionType()}(login=${this.account.login}, password=${this.account.password})`
  }

  public abstract getActionType(): string

  public abstract performImpl(modelOrApplication: T): void

}

export class YandexLoginAction extends LoginAction<YandexLogin> {
  public static readonly type: string = 'YandexLogin'

  constructor(account: UserAccount) {
    super(account, YandexLoginFeature.get)
  }

  public performImpl(modelOrApplication: YandexLogin): void {
    modelOrApplication.loginWithYandexAccount(this.account)
  }

  public getActionType(): string {
    return YandexLoginAction.type
  }
}

export class MailRuLoginAction extends LoginAction<MailRuLogin> {
  public static readonly type: string = 'MailRuLogin'

  constructor(account: UserAccount) {
    super(account, MailRuLoginFeature.get)
  }

  public performImpl(modelOrApplication: MailRuLogin): void {
    modelOrApplication.loginWithMailRuAccount(this.account)
  }

  public getActionType(): string {
    return MailRuLoginAction.type
  }
}

export class GoogleLoginAction extends LoginAction<GoogleLogin> {
  public static readonly type: string = 'GoogleLogin'

  constructor(account: UserAccount) {
    super(account, GoogleLoginFeature.get)
  }

  public performImpl(modelOrApplication: GoogleLogin): void {
    modelOrApplication.loginWithGoogleAccount(this.account)
  }

  public getActionType(): string {
    return GoogleLoginAction.type
  }
}

export class OutlookLoginAction extends LoginAction<OutlookLogin> {
  public static readonly type: string = 'OutlookLogin'

  constructor(account: UserAccount) {
    super(account, OutlookLoginFeature.get)
  }

  public performImpl(modelOrApplication: OutlookLogin): void {
    modelOrApplication.loginWithOutlookAccount(this.account)
  }

  public getActionType(): string {
    return OutlookLoginAction.type
  }
}

export class HotmailLoginAction extends LoginAction<HotmailLogin> {
  public static readonly type: string = 'HotmailLogin'

  constructor(account: UserAccount) {
    super(account, HotmailLoginFeature.get)
  }

  public performImpl(modelOrApplication: HotmailLogin): void {
    modelOrApplication.loginWithHotmailAccount(this.account)
  }

  public getActionType(): string {
    return HotmailLoginAction.type
  }
}

export class RamblerlLoginAction extends LoginAction<RamblerLogin> {
  public static readonly type: string = 'RamblerLogin'

  constructor(account: UserAccount) {
    super(account, RamblerLoginFeature.get)
  }

  public performImpl(modelOrApplication: RamblerLogin): void {
    modelOrApplication.loginWithRamblerAccount(this.account)
  }

  public getActionType(): string {
    return RamblerlLoginAction.type
  }
}

export class YahooLoginAction extends LoginAction<YahooLogin> {
  public static readonly type: string = 'YahooLogin'

  constructor(account: UserAccount) {
    super(account, YahooLoginFeature.get)
  }

  public performImpl(modelOrApplication: YahooLogin): void {
    modelOrApplication.loginWithYahooAccount(this.account)
  }

  public getActionType(): string {
    return YandexLoginAction.type
  }
}

export class CustomMailServiceLoginAction extends LoginAction<CustomMailServiceLogin> {
  public static readonly type: string = 'CustomMailServiceLogin'

  constructor(account: UserAccount) {
    super(account, CustomMailServiceLoginFeature.get)
  }

  public performImpl(modelOrApplication: CustomMailServiceLogin): void {
    modelOrApplication.loginWithCustomMailServiceAccount(this.account)
  }

  public getActionType(): string {
    return CustomMailServiceLoginAction.type
  }
}
