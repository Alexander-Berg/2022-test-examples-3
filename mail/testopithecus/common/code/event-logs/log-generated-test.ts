import { range } from '../../ys/ys'
import { ImapMailboxBuilder, ImapMessage } from '../mail/mailbox-preparer'
import { RegularYandexTestBase } from '../mbt/mbt-test'
import { TestPlan } from '../mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../users/user-pool'

export class LogGeneratedTest extends RegularYandexTestBase {
  constructor(public plan: TestPlan) {
    super('Test was generated from logs')
  }

  public prepareMailbox(mailbox: ImapMailboxBuilder): void {
    for (const i of range(0, 15)) {
      mailbox.nextMessage(`subj${i}`)
    }
  }

  public regularScenario(account: UserAccount): TestPlan {
    return this.plan
  }
}
