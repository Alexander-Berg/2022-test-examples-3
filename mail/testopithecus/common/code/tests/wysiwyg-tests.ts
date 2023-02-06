import { Fuzzer } from '../fuzzing/fuzzer'
import { OpenComposeAction } from '../mail/actions/write-message-actions'
import { AppendToBody, SetItalic, SetStrong } from '../mail/actions/wysiwyg-actions'
import { ImapMailboxBuilder } from '../mail/mailbox-preparer'
import { RegularYandexTestBase } from '../mbt/mbt-test'
import { TestPlan } from '../mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../users/user-pool'
import { PseudoRandomProvider } from '../utils/pseudo-random'

export class FormatTextTest extends RegularYandexTestBase {
  constructor() {
    super('should format text in wysiwyg correct')
  }

  public prepareMailbox(mailbox: ImapMailboxBuilder): void {
  }

  public regularScenario(account: UserAccount): TestPlan {
    return TestPlan.yandexLogin(account)
      .then(new OpenComposeAction())
      .then(new AppendToBody(0, new Fuzzer().fuzzyBody(PseudoRandomProvider.INSTANCE, 10)))
      .then(new SetItalic(1, 5))
      .then(new SetStrong(3, 8))
  }
}
