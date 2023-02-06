import { MockMailboxProvider } from '../../__tests__/mock-mailbox';
import { MaillistComponent } from '../../code/mail/components/maillist-component';
import { EventNames } from '../../code/mail/logging/events/event-names';
import { MBTComponent } from '../../code/mbt/mbt-abstractions';
import { AppModel, AppModelProvider } from '../../code/mbt/walk/fixed-scenario-strategy';
import { Scenario } from '../scenario';

export class InitialModelProvider {

  public createApplicationModel(session: Scenario): AppModel {
    const modelProvider: AppModelProvider = MockMailboxProvider.emptyFoldersOneAccount()
    return modelProvider.takeAppModel();
  }

  public getInitialComponent(session: Scenario): MBTComponent {
    const event = session.events[0]
    switch (event.name) {
      case EventNames.START_WITH_MESSAGE_LIST:
        return new MaillistComponent()
    }
    throw new Error()
  }

}
