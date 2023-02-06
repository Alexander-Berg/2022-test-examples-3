import { UserAccount } from '../../../users/user-pool'
import {
  CustomMailServiceLogin,
  GoogleLogin,
  HotmailLogin,
  MailRuLogin,
  OutlookLogin,
  RamblerLogin,
  YahooLogin,
  YandexLogin,
} from '../../mail-features'
import { MailAppModelHandler } from '../mail-model'

export class LoginModel implements YandexLogin, MailRuLogin, GoogleLogin, OutlookLogin, HotmailLogin,
  RamblerLogin, YahooLogin, CustomMailServiceLogin {

  public constructor(public accountDataHandler: MailAppModelHandler) {
  }

  public loginWithYandexAccount(account: UserAccount): void {
    this.accountDataHandler.logInToAccount(account)
  }

  public loginWithCustomMailServiceAccount(account: UserAccount): void {
    this.accountDataHandler.logInToAccount(account)
  }

  public loginWithGoogleAccount(account: UserAccount): void {
    this.accountDataHandler.logInToAccount(account)
  }

  public loginWithHotmailAccount(account: UserAccount): void {
    this.accountDataHandler.logInToAccount(account)
  }

  public loginWithMailRuAccount(account: UserAccount): void {
    this.accountDataHandler.logInToAccount(account)
  }

  public loginWithOutlookAccount(account: UserAccount): void {
    this.accountDataHandler.logInToAccount(account)
  }

  public loginWithRamblerAccount(account: UserAccount): void {
    this.accountDataHandler.logInToAccount(account)
  }

  public loginWithYahooAccount(account: UserAccount): void {
    this.accountDataHandler.logInToAccount(account)
  }
}
