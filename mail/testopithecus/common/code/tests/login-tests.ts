import { Nullable } from '../../ys/ys'
import { YandexLoginAction } from '../mail/actions/login-actions'
import { GoToAccountSwitcherAction } from '../mail/actions/message-list-actions'
import { AddNewAccountAction, SwitchAccountAction } from '../mail/actions/multi-account-actions'
import { ImapMailboxBuilder, ImapMessage } from '../mail/mailbox-preparer'
import { FeatureID } from '../mbt/mbt-abstractions'
import { AbstractMBTTest, AccountType} from '../mbt/mbt-test'
import { AppModelProvider, TestPlan } from '../mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../users/user-pool'

export class YandexLoginTest extends AbstractMBTTest {
  constructor() {
    super('should login 3 yandex accounts')
  }

  public requiredAccounts(): AccountType[] {
    return [AccountType.Yandex, AccountType.Yandex, AccountType.Yandex]
  }

  public prepareMailboxes(mailboxes: ImapMailboxBuilder[]): void {
    mailboxes[0].nextMessage('firstAccountMsg')
    mailboxes[1].nextMessage('secondAccountMsg')
    mailboxes[2].nextMessage('thirdAccountMsg')
  }

  public scenario(accounts: UserAccount[], modelProvider: Nullable<AppModelProvider>, supportedFeatures: FeatureID[]): TestPlan {
    return TestPlan.empty()
      .then(new YandexLoginAction(accounts[0]))
      .then(new GoToAccountSwitcherAction())
      .then(new AddNewAccountAction())
      .then(new YandexLoginAction(accounts[1]))
      .then(new GoToAccountSwitcherAction())
      .then(new AddNewAccountAction())
      .then(new YandexLoginAction(accounts[2]))
  }
}

export class SwitchAccountTest extends AbstractMBTTest {
  constructor() {
    super('should switch between 2 yandex accounts')
  }

  public requiredAccounts(): AccountType[] {
    return [AccountType.Yandex, AccountType.Yandex]
  }

  public prepareMailboxes(mailboxes: ImapMailboxBuilder[]): void {
    mailboxes[0].nextMessage('firstAccountMsg')
    mailboxes[1].nextMessage('secondAccountMsg')
  }

  public scenario(accounts: UserAccount[], modelProvider: Nullable<AppModelProvider>, supportedFeatures: FeatureID[]): TestPlan {
    return TestPlan.empty()
      .then(new YandexLoginAction(accounts[0]))
      .then(new GoToAccountSwitcherAction())
      .then(new AddNewAccountAction())
      .then(new YandexLoginAction(accounts[1]))
      .then(new GoToAccountSwitcherAction())
      .then(new SwitchAccountAction(accounts[0].login))
      .then(new GoToAccountSwitcherAction())
      .then(new SwitchAccountAction(accounts[0].login))
  }
}
