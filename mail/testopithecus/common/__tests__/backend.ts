import 'mocha'
import { MailboxClient, MailboxClientHandler } from '../code/client/mailbox-client'
import { MobileMailBackend } from '../code/mail/backend/mobile-mail-backend'
import { LoginComponent } from '../code/mail/components/login-component'
import { MaillistComponent } from '../code/mail/components/maillist-component'
import { TestopithecusRegistry } from '../code/mail/logging/testopithecus-registry'
import { MailboxDownloader } from '../code/mail/mailbox-downloader'
import { MailboxModel } from '../code/mail/model/mail-model'
import { MBTPlatform } from '../code/mbt/mbt-test'
import { StateMachine } from '../code/mbt/state-machine'
import { singleAccountBehaviour } from '../code/mbt/walk/behaviour/full-user-behaviour'
import { MailTestRunner, TestPlan } from '../code/mbt/walk/fixed-scenario-strategy'
import { RandomActionChooser, UserBehaviourWalkStrategy } from '../code/mbt/walk/user-behaviour-walk-strategy'
import { TestsRegistry } from '../code/tests/register-your-test-here'
import { Registry } from '../code/utils/registry'
import { TestStackReporter } from './logging-tests/reporting/test-stack-reporter'
import { MockMailboxProvider } from './mock-mailbox'
import { ConsoleLog } from './pod/console-log'
import { DefaultJSONSerializer } from './pod/default-json'
import { SyncSleepImpl } from './pod/sleep'
import { createNetworkClient, createSyncNetwork } from './test-utils'

describe('Run all tests on Mobile API', () => {
  runAllTests(false)
})

describe('Run all tests on Model', () => {
  runAllTests(true)
})

function runAllTests(mockMode: boolean) {
  setupRegistry()
  const registry: TestsRegistry = new TestsRegistry(ConsoleLog.LOGGER)
  // TestsRegistry.testToDebug = new FullCoverageTest( ConsoleLog.LOGGER)

  for (const test of registry.getAllTests()) {
    const network = createSyncNetwork()
    const jsonSerializer = new DefaultJSONSerializer()

    const testRunner = new MailTestRunner(MBTPlatform.MobileAPI, test, network, jsonSerializer, SyncSleepImpl.instance, ConsoleLog.LOGGER)

    afterEach((done) => {
      testRunner.finish()
      done()
    })

    const supportedFeatures = mockMode ? MailboxModel.allSupportedFeatures : MobileMailBackend.allSupportedFeatures
    if (testRunner.isEnabled(MailboxModel.allSupportedFeatures, supportedFeatures)) {
      it(test.description, (done) => {
        const accounts = testRunner.lockAndPrepareMailbox()
        if (accounts !== []) {
          const clients: MailboxClient[] = []
          accounts.forEach((account) => {
            const mailboxClient = createNetworkClient(account)
            clients.push(mailboxClient)
          })
          const mailboxClientHandler = new MailboxClientHandler(clients)
          const mailBackend = new MobileMailBackend(mailboxClientHandler)
          const application = !mockMode ? mailBackend : null

          const modelProvider = new MailboxDownloader(clients, ConsoleLog.LOGGER)

          const plan = test.scenario(accounts.map((a) => a.account), modelProvider, supportedFeatures)
          const logs = emulateActualLogs(plan)

          testRunner.runTest(accounts, new LoginComponent(), application)
          testRunner.validateLogs(logs)
        } else {
          // TODO: this.skip()
        }
        done()
      })
    } else {
      // tslint:disable-next-line:no-empty
      it.skip(test.description, () => {
      })
    }
  }
}

function emulateActualLogs(plan: TestPlan): string {
  const actualEvents = plan.getExpectedEvents()
  const reporter = new TestStackReporter()
  TestopithecusRegistry.setEventReporter(reporter)
  actualEvents.forEach((e) => e.report())
  return reporter.events.map((e) => {
    const valueDict: { [key: string]: any } = {}
    e.attributes.forEach((v, k) => {
      if (typeof v === 'bigint') {
        valueDict[k] = Number(v)
      } else {
        valueDict[k] = v
      }
    })
    return JSON.stringify({
      name: e.name,
      value: valueDict,
    })
  }).join('\n')
}

describe('Test model inconsistensy', () => {
  it('model should be consistent', (done) => {
    setupRegistry()
    const model = MockMailboxProvider.emptyFoldersOneAccount().takeAppModel()
    const walkStrategy = new UserBehaviourWalkStrategy(singleAccountBehaviour(), new RandomActionChooser(), 1000)
    const stateMachine = new StateMachine(model, model.copy(), walkStrategy, ConsoleLog.LOGGER)
    stateMachine.go(new MaillistComponent())
    done()
  })
})

function setupRegistry() {
  Registry.get().logger = ConsoleLog.LOGGER
}
