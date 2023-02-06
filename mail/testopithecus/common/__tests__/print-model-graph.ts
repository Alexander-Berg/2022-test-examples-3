import * as fs from 'fs';
import { ReplyMessageAction, SendMessageAction } from '../code/mail/actions/write-message-actions';
import { MaillistComponent } from '../code/mail/components/maillist-component';
import { MailboxModel } from '../code/mail/model/mail-model';
import { MBTActionType } from '../code/mbt/mbt-abstractions';
import { allActionsBehaviour, singleAccountBehaviour } from '../code/mbt/walk/behaviour/full-user-behaviour';
import { MultiRunner } from '../code/mbt/walk/dfs-walk-strategy';
import { PersonalActionLimits } from '../code/mbt/walk/limits/personal-action-limits';
import { Int32 } from '../ys/ys';
import { MockMailboxProvider } from './mock-mailbox';
import { ConsoleLog } from './pod/console-log';

describe('Application Model graph', () => {
  it.skip('should be printed', (done) => {
    const supportedFeatures = MailboxModel.allSupportedFeatures;
    const modelProvider = MockMailboxProvider.emptyFoldersOneAccount();

    const limits = new PersonalActionLimits(100)
      .setLimit(ReplyMessageAction.type, 2)
      .setLimit(SendMessageAction.type, 2)

    const runner = new MultiRunner(new MaillistComponent(), singleAccountBehaviour(), limits, modelProvider, supportedFeatures, ConsoleLog.LOGGER);
    runner.preparePath();

    const filename = 'graph.txt';

    fs.writeFileSync(filename, 'digraph g {\n');
    for (const vertex of runner.walkStrategyWithState.graph.adjList.keys()) {
      fs.appendFileSync(filename, `    ${vertex};\n`);
    }
    for (const edge of runner.walkStrategyWithState.graph.edges) {
      fs.appendFileSync(filename, `    ${edge.getFrom()} -> ${edge.getTo()} [label="${edge.getAction().tostring()}"]\n`);
    }
    fs.appendFileSync(filename, '}');
    done()
  });
});
