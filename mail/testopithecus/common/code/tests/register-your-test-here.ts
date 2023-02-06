import { Nullable, range, TypeSupport } from '../../ys/ys'
import { MBTTest } from '../mbt/mbt-test'
import { Logger } from '../utils/logger'
import { ArchiveFirstMessageTest } from './archive-message-tests'
import { SendMessageWithBody, SendMessageWithToAddedFromSuggestTest } from './compose-message-tests'
import { DeleteCurrentMessageTest, DeleteMessageBySwipeTest, DeleteThreadTest } from './delete-message-tests'
import { ChangeFoldersTest, CreateFoldersTest } from './folder-navigation-tests'
import { NoComposeFullCoverageTest } from './full-coverage-tests'
import { MBTTestGenerator } from './generators/mbt-test-generator'
import { GroupBySubjectTest } from './group-by-subject-test'
import {
  CanOpenMessageAfterGroupActionTest,
  GroupDeleteMessagesTest,
  GroupMarkAsReadDifferentMessagesTest,
  GroupMarkAsReadMessagesTest,
} from './group-mode-tests'
import { LabelAllThreadMessagesImportantByLabellingMainMessageTest, MarkAsImportantTest } from './importance-tests'
import { InboxTopBarDisplayTest } from './inbox-top-bar-display-test'
import { SwitchAccountTest, YandexLoginTest } from './login-tests'
import { GroupModeViewTest } from './group-mode-view-test'
import { SwipeToUndeadTest } from './swipe-to-undead-test'
import { MoveToFolderFromContextMenuTest } from './move-to-folder-from-context-menu-test'
import { SwipeToReadTest } from './swipe-to-read-test'
import { MoveToFolderTest } from './move-to-folder-tests'
import { MySuperTest } from './my-super-tests'
import { RandomWalkTest } from './random-walk-test'
import {
  MarkAllThreadMessagesReadByMarkingMainMessageTest,
  MarkUnreadAfterReadTest,
  ReadMessageAfterOpeningTest,
} from './read-unread-tests'
import { ReceiveMessageTest, ReplyMessageTest } from './send-message-tests'
import { MoveToSpamFirstMessageTest } from './spamable-tests'
import { FormatTextTest } from './wysiwyg-tests'

export class TestsRegistry {
  public static testToDebug: Nullable<MBTTest> = null
  public static shouldBeFixedInApp: MBTTest[] = []

  private tests: MBTTest[] = [
    new YandexLoginTest(),
    new SwitchAccountTest(),
    new SendMessageWithBody(),
    new SendMessageWithToAddedFromSuggestTest(),
    new MoveToSpamFirstMessageTest(),
    new MarkUnreadAfterReadTest(),
    new ChangeFoldersTest(),
    new CreateFoldersTest(),
    new MoveToFolderTest(),
    new ReadMessageAfterOpeningTest(),
    new MarkAllThreadMessagesReadByMarkingMainMessageTest(),
    new ReceiveMessageTest(),
    new ReplyMessageTest(),
    new GroupMarkAsReadDifferentMessagesTest(),
    new GroupMarkAsReadMessagesTest(),
    new GroupDeleteMessagesTest(),
    new CanOpenMessageAfterGroupActionTest(),
    new DeleteThreadTest(),
    new DeleteMessageBySwipeTest(),
    new DeleteCurrentMessageTest(),
    new MarkAsImportantTest(),
    new LabelAllThreadMessagesImportantByLabellingMainMessageTest(),
    new FormatTextTest(),
    new MySuperTest(),
    new ArchiveFirstMessageTest(),
    new GroupBySubjectTest(),
    new InboxTopBarDisplayTest(),
    new GroupModeViewTest(),
    new SwipeToReadTest(),
    new SwipeToUndeadTest(),
    new MoveToFolderFromContextMenuTest(),

  ]

  private generators: MBTTestGenerator[] = [
    // new LogTestGenerator(),
  ]

  constructor(private logger: Logger) {
    // for (const i of range(0, 10)) {
    //  this.tests.push(new RandomWalkTest(5, logger, TypeSupport.asInt32(i)!))
    // }
    // this.tests.push(new NoComposeFullCoverageTest(this.logger))
  }

  public static failsBecauseBugInApp(test: MBTTest): boolean {
    return this.shouldBeFixedInApp.map((t) => t.description).includes(test.description)
  }

  public getAllTests(): MBTTest[] {
    const testToDebugSnapshot = TestsRegistry.testToDebug
    if (testToDebugSnapshot !== null) {
      return [testToDebugSnapshot]
    }
    const tests = this.tests
    for (const generator of this.generators) {
      for (const test of generator.generateTests()) {
        tests.push(test)
      }
    }
    return tests
  }
}
