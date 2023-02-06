import { Int32, int64, Nullable, range } from '../../../ys/ys'
import { JSONSerializer } from '../../client/json/json-serializer'
import { MailboxClient } from '../../client/mailbox-client'
import { SyncNetwork } from '../../client/network/sync-network'
import { CrossPlatformLogsParser } from '../../event-logs/cross-platform-logs-parser'
import { AssertAction } from '../../mail/actions/assert-action'
import { DebugDumpAction } from '../../mail/actions/debug-dump-action'
import { YandexLoginAction } from '../../mail/actions/login-actions'
import { PingAccountLockAction } from '../../mail/actions/ping-account-lock-action'
import { TestopithecusEvent } from '../../mail/logging/testopithecus-event'
import { MailboxDownloader } from '../../mail/mailbox-downloader'
import {
  ImapMailbox,
  ImapMailboxBuilder,
  MailboxPreparer,
} from '../../mail/mailbox-preparer'
import { TestsRegistry } from '../../tests/register-your-test-here'
import { OauthService } from '../../users/oauth-service'
import { OAuthUserAccount, UserAccount, UserLock, UserPool } from '../../users/user-pool'
import { UserService } from '../../users/user-service'
import { UserServicePool } from '../../users/user-service-pool'
import { assertStringEquals } from '../../utils/assert'
import { Logger } from '../../utils/logger'
import { MockPlatform } from '../../utils/platform'
import { SyncSleep } from '../../utils/sync-sleep'
import { max, requireNonNull } from '../../utils/utils'
import { App, FeatureID, MBTAction, MBTComponent } from '../mbt-abstractions'
import { MBTPlatform, MBTTest, TestSettings } from '../mbt-test'
import { StateMachine, WalkStrategy } from '../state-machine'

export class FixedScenarioStrategy implements WalkStrategy {
  private position: Int32 = 0

  constructor(private scenario: MBTAction[]) {
  }

  public nextAction(model: App, applicationFeatures: FeatureID[], component: MBTComponent): Nullable<MBTAction> {
    if (this.position === this.scenario.length) {
      return null
    }
    const action = this.scenario[this.position]
    this.position += 1
    return action
  }
}

export class TestPlan {

  private actions: MBTAction[] = []
  private constructor() {}

  /**
   * Статический метод для вызова консртуктора. Позволяет не писать первый действием
   * LoginAction
   */
  public static yandexLogin(account: UserAccount): TestPlan {
    return new TestPlan().then(new YandexLoginAction(account))
  }

  /**
   * Стотический метод для вызова конструктора. При его использовании указывать LoginAction
   * надо самостоятельно.
   */
  public static empty(): TestPlan {
    return new TestPlan()
  }

  public then(action: MBTAction): TestPlan {
    return this.thenChain([action])
  }

  public thenTestPlan(testPlan: TestPlan): TestPlan {
    for (const action of testPlan.actions) {
      this.actions.push(action)
    }
    return this
  }

  public thenChain(actions: MBTAction[]): TestPlan {
    for (const action of actions) {
      this.actions.push(action)
      this.actions.push(new DebugDumpAction())
      this.actions.push(new AssertAction())
    }
    return this
  }

  public unsupportedActions(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): MBTAction[] {
    const result: MBTAction[] = []
    for (const action of this.actions) {
      if (!action.supported(modelFeatures, applicationFeatures)) {
        result.push(action)
      }
    }
    return result
  }

  public getExpectedEvents(): TestopithecusEvent[] {
    const result: TestopithecusEvent[] = []
    for (const action of this.actions) {
      for (const event of action.events()) {
        result.push(event)
      }
    }
    return result
  }

  public build(accountLocks: Nullable<UserLock[]>, assertionsEnabled: boolean = true): WalkStrategy {
    const actions: MBTAction[] = []
    for (const action of this.actions) {
      if (assertionsEnabled || AssertAction.type !== action.getActionType()) {
        actions.push(action)
      }
      if (accountLocks !== null) {
        accountLocks!.forEach((accLock) => actions.push(new PingAccountLockAction(accLock)))
      }
    }
    return new FixedScenarioStrategy(actions)
  }

  public tostring(): string {
    let s = 'PATH\n'
    for (const action of this.actions) {
      s += action.tostring() + '\n'
    }
    return s + 'END\n'
  }
}

export class MailTestRunner {
  private userPool: UserPool
  private mailboxPreparer: MailboxPreparer
  private oauthService: OauthService

  private locks: UserLock[] = []
  private modelProvider: Nullable<AppModelProvider> = null
  private testPlan: Nullable<TestPlan> = null

  /**
   * Запускалка почтовых статических тестов. Умеет проверять, поддерживает ли приложение данный тест
   *
   * @param platform - платформа. на которой запускаются тесты
   * @param test - тест, который мы хотим запустить
   * @param network - сеть
   * @param jsonSerializer - работа с json
   * @param sleep - сон во время ожидания наливки ящика
   * @param logger - логгер
   * @param assertionsEnabled - включены ли assert-ы, по-умолчанию, true
   */
  constructor(private platform: MBTPlatform,
              private test: MBTTest,
              private network: SyncNetwork,
              private jsonSerializer: JSONSerializer,
              private sleep: SyncSleep,
              private logger: Logger,
              private assertionsEnabled: boolean = true) {
    const userService = new UserService(network, jsonSerializer, logger)
    this.userPool = new UserServicePool(userService, 'prod')
    this.mailboxPreparer = new MailboxPreparer(network, jsonSerializer, sleep, logger)
    this.oauthService = new OauthService(network, jsonSerializer)
  }

  /**
   * Сначала проверяем, поддерживает ли тест приложение
   *
   * @param modelFeatures - фичи, поодерживаемые моделью
   * @param applicationFeatures - фичи, поддерживаемые приложением
   */
  public isEnabled(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    if (TestsRegistry.testToDebug !== null) {
      return true
    }

    const fakeAccounts = this.test.requiredAccounts().map((_) => new UserAccount('', ''))
    const unsupportedActions = this.test
      .scenario(fakeAccounts, null, applicationFeatures)
      .unsupportedActions(modelFeatures, applicationFeatures)
    if (unsupportedActions.length > 0) {
      let s = ''
      for (const action of unsupportedActions) {
        s += `;${action.tostring()}`
      }
      this.logger.log(`'${this.test.description}': application should support actions: ${s}`)
    }
    const ignored = this.getTestSettings().isIgnored()
    if (ignored) {
      this.logger.log(`'${this.test.description}': помечен как нерабочий. ` +
        'Пожалуйста, почитите это - тесты должна включаться/отключаться через поддержку фичей')
    }
    return unsupportedActions.length === 0 && !ignored
  }

  public lockAndPrepareMailbox(): OAuthUserAccount[] {
    this.logger.log(`Try to prepare mailbox for test ${this.test.description}`)
    const min10 = int64(10 * 60 * 1000)
    const min2 = int64(2 * 60 * 1000)
    const requiredAccounts = this.test.requiredAccounts()
    const accounts: OAuthUserAccount[] = []
    const clients: MailboxClient[] = []
    const mailboxBuilders: ImapMailboxBuilder[] = []

    // Initially we acquire all locks
    for (const accountIndex of range(0, requiredAccounts.length)) {
      const lock = this.userPool.tryAcquire(min10, min2)
      if (lock === null) {
        // if at least one of the locks can't be acquired, we are done and should release locks acquired before
        this.releaseLocks()
        return []
      }
      this.locks.push(lock!)
    }

    // Create all builders
    for (const accountIndex of range(0, this.locks.length)) {
      const account = this.locks[accountIndex].lockedAccount()
      mailboxBuilders.push(ImapMailbox.builder(account))
    }

    this.test.prepareMailboxes(mailboxBuilders)

    for (const i of range(0, this.locks.length)) {
      const account = this.locks[i].lockedAccount()
      const mailbox = mailboxBuilders[i].build()
      this.mailboxPreparer.prepare(mailbox)
      const token = this.oauthService.getToken(account)
      const client = new MailboxClient(
        MockPlatform.androidDefault,
        account,
        token,
        this.network,
        this.jsonSerializer,
        this.logger,
      )
      clients.push(client)
      accounts.push(new OAuthUserAccount(account, token))
    }

    this.modelProvider = new MailboxDownloader(clients, this.logger)
    return accounts
  }

  public runTest(accounts: OAuthUserAccount[], start: MBTComponent, application: Nullable<App>): void {
    this.logger.log(`Test ${this.test.description} started`)
    const modelProvider = requireNonNull(this.modelProvider, 'Should lockAndPrepareMailbox before runTest!')
    const model = modelProvider.takeAppModel()
    const model2 = model.copy()
    const model3 = model.copy()
    const supportedFeatures = (application !== null ? application! : model).supportedFeatures

    const testPlan = this.test.scenario(accounts.map((a) => a.account), modelProvider, supportedFeatures)
    this.testPlan = testPlan

    this.logger.log(testPlan.tostring())
    const walkStrategyWithState1 = testPlan.build(null, true)
    const walkStrategyWithState2 = testPlan.build(this.locks, this.assertionsEnabled)

    this.logger.log('Model vs Model testing started')
    const modelVsModel = new StateMachine(model, model2, walkStrategyWithState1, this.logger)
    modelVsModel.go(start)
    this.logger.log('Model vs Model testing finished')
    this.logger.log('\n')

    if (application === null) {
      return
    }

    this.logger.log('Model vs Application testing started')
    const modelVsApplication = new StateMachine(model3, application, walkStrategyWithState2, this.logger)
    modelVsApplication.go(start)
    this.logger.log('Model vs Application testing finished')
    this.logger.log('\n')
    this.logger.log(`Test ${this.test.description} finished`)
  }

  public validateLogs(logs: string): void {
    if (!this.getTestSettings().shouldValidateLogs()) {
      this.logger.log('Тест не хочет, чтобы его логи проверяли')
      return
    }

    const testPlan = requireNonNull(this.testPlan, 'Запустите тест runTest сначала!')

    const actualEvents: TestopithecusEvent[] = []
    const lines = logs.split('\n')
    for (const line of lines) {
      if (line.length > 0) {
        const parser = new CrossPlatformLogsParser(this.jsonSerializer)
        const event = parser.parse(line)
        actualEvents.push(event)
      }
    }

    const expectedEvents = testPlan.getExpectedEvents()

    for (const i of range(0, max(actualEvents.length, expectedEvents.length))) {
      if (i >= actualEvents.length) {
        throw new Error(`Ожидалось событие '${expectedEvents[i].name}', но больше нет`)
      }
      const actual = actualEvents[i]
      if (i >= expectedEvents.length) {
        throw new Error(`Вижу событие '${actual.name}', но больше не должно быть`)
      }
      const expected = expectedEvents[i]
      assertStringEquals(expected.name, actual.name, `Разные события на месте #${i}`)
    }
  }

  public finish(): void {
    this.releaseLocks()
    this.logger.log(`Тест '${this.test.description}' закончился`)
  }

  private getTestSettings(): TestSettings {
    const settings = new TestSettings(this.platform)
    this.test.setupSettings(settings)
    return settings
  }

  private releaseLocks(): void {
    if (this.locks.length !== 0) {
      this.locks.forEach((lock) => {
        if (lock !== null) {
          lock.release()
        }
      })
      this.locks = []
      this.logger.log(`Locks for test ${this.test.description} released`)
    }
  }
}

/**
 * Модель еще должна быть копируемой
 */
export interface AppModel extends App {
  copy(): AppModel
}

export interface AppModelProvider {
  takeAppModel(): AppModel
}
