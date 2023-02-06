import { Int32, Nullable } from '../../ys/ys'
import { ImapMailboxBuilder } from '../mail/mailbox-preparer'
import { UserAccount } from '../users/user-pool'
import { requireNonNull } from '../utils/utils'
import { FeatureID } from './mbt-abstractions'
import { AppModelProvider, TestPlan } from './walk/fixed-scenario-strategy'

export enum AccountType {
  Yandex = 'YANDEX',
  Yahoo = 'YAHOO',
  Google = 'GMAIL',
  Mail = 'MAIL',
  Hotmail = 'HOTMAIL',
  Rambler = 'RAMBLER',
  Outlook = 'OUTLOOK',
  Other = 'OTHER',
}

export enum MBTPlatform {
  MobileAPI,
  Android,
  IOS,
}

export interface MBTTest {
  readonly description: string

  /**
   * Если у вашего теста есть братишка в Пальме, то вы можете тут вернуть id соответсвующих кейсов
   */
  setupSettings(settings: TestSettings): void

  requiredAccounts(): AccountType[]

  /**
   * Тут надо описать mailbox для теста
   * Этот метод вызывается перед методом scenario
   *
   * @param builders - билдеры почтовых ящиков
   */
  prepareMailboxes(builders: ImapMailboxBuilder[]): void

  /**
   * Тут надо вернуть сценарий для теста, вызывается после prepareMailbox
   *
   * @param accounts - аккаунты, которые оперируем во время теста. Может быть пустым, если тест проверя поддержку фичей
   * @param modelProvider - штука, которая может скачать ящик. Может быть null, если тест проверяет поддержку фичей
   * @param supportedFeatures - поддерживаемые приложением фичи
   */
  scenario(accounts: UserAccount[],
           modelProvider: Nullable<AppModelProvider>,
           supportedFeatures: FeatureID[]): TestPlan
}

export abstract class AbstractMBTTest implements MBTTest {
  constructor(public readonly description: string) {
  }

  public setupSettings(settings: TestSettings): void {
  }

  public abstract requiredAccounts(): AccountType[]

  public abstract prepareMailboxes(mailboxes: ImapMailboxBuilder[]): void

  public abstract scenario(accounts: UserAccount[], modelProvider: Nullable<AppModelProvider>, supportedFeatures: FeatureID[]): TestPlan
}

export abstract class RegularYandexTestBase extends AbstractMBTTest {
  constructor(description: string) {
    super(description)
  }

  public requiredAccounts(): AccountType[] {
    return [AccountType.Yandex]
  }

  public prepareMailboxes(mailboxes: ImapMailboxBuilder[]): void {
    if (mailboxes.length !== 1) {
      throw new Error('Тесты на базе RegularYandexTestBase должны наливать ровно один аккаунт!')
    }
    this.prepareMailbox(mailboxes[0])
  }

  public scenario(accounts: UserAccount[], modelProvider: Nullable<AppModelProvider>, supportedFeatures: FeatureID[]): TestPlan {
    if (accounts.length !== 1) {
      throw new Error('Тесты на базе RegularYandexTestBase должны использовать ровно один аккаунт!')
    }
    return this.regularScenario(accounts[0])
  }

  public abstract regularScenario(account: UserAccount): TestPlan

  public abstract prepareMailbox(mailbox: ImapMailboxBuilder): void
}

export class TestSettings {
  private testCaseIds: Map<MBTPlatform, Int32> = new Map<MBTPlatform, Int32>()
  private logValidationPlatforms: Set<MBTPlatform> = new Set()
  private ignoredPlatforms: Set<MBTPlatform> = new Set()

  constructor(private currentPlatform: MBTPlatform) {
  }

  public setTestCaseId(platform: MBTPlatform, id: Int32): TestSettings {
    this.testCaseIds.set(platform, id)
    return this
  }

  public androidCase(id: Int32): TestSettings {
    return this.setTestCaseId(MBTPlatform.Android, id)
  }

  public iosCase(id: Int32): TestSettings {
    return this.setTestCaseId(MBTPlatform.IOS, id)
  }

  public validateLogs(platform: MBTPlatform): TestSettings {
    this.logValidationPlatforms.add(platform)
    return this
  }

  public shouldValidateLogs(): boolean {
    return this.logValidationPlatforms.has(this.currentPlatform)
  }

  public ignoreOn(platform: MBTPlatform): TestSettings {
    this.ignoredPlatforms.add(platform)
    return this
  }

  public isIgnored(): boolean {
    return this.ignoredPlatforms.has(this.currentPlatform)
  }
}
