import { int64, range } from '../../../ys/ys';
import { currentTimeMs } from '../logging/logging-utils';
import { WriteMessage } from '../mail-features';
import { ComposeMessageModel, Draft } from './compose-message-model';
import { DefaultFolderName, FullMessage, MailAppModelHandler, Message, MessageId } from './mail-model';
import { MessageListDisplayModel } from './message-list-display-model';
import { MessageNavigatorModel } from './message-navigator-model';
import { WysiwygModel } from './wysiwyg-model';

export class WriteMessageModel implements WriteMessage {

  constructor(
    private model: MessageListDisplayModel,
    private messageNavigator: MessageNavigatorModel,
    private compose: ComposeMessageModel,
    public readonly accountDataHandler: MailAppModelHandler,
    public wysiwig: WysiwygModel,
  ) {
  }

  public openCompose(): void {
    this.compose.composeDraft = new Draft(this.wysiwig);
  }

  public sendMessage(to: string, subject: string): void {
    this.createAndAddSentReceivedMessage(to, subject);
  }

  public replyMessage(): void {
    const openedMessage = this.model.storedMessage(this.messageNavigator.openedMessage).head
    const msgSentThread = this.createAndAddSentReceivedMessage(openedMessage.from, 'Re: ' + openedMessage.subject)
    this.model.accountDataHandler.getCurrentAccount().threads.forEach((thread) => {
      if (thread.has(this.messageNavigator.openedMessage)) {
        msgSentThread.forEach((msg) => thread.add(msg));
      }
    });
  }

  public copy(): WriteMessageModel {
    return new WriteMessageModel(this.model, this.messageNavigator, this.compose, this.accountDataHandler, this.wysiwig)
  }

  public sendPrepared(): void {
    const sentSubject = this.compose.getDraft().subject === null ? '' : this.compose.getDraft().subject!;
    const sentTo = this.compose.getDraft().to.size === 0 ? new Set<string>() : this.compose.getDraft().to;
    const sentBody: string = this.compose.getDraft().getWysiwyg().getRichBody();
    const sentMsg = new FullMessage(
      new Message(this.accountDataHandler.getCurrentAccount().aliases[0], sentSubject, int64(Date.now()), 1, true),
      sentTo,
      sentBody,
    );
    const messages = this.model.accountDataHandler.getCurrentAccount().messages
    const sentMsgMid = int64(messages.size);
    messages.set(int64(messages.size), sentMsg);
    this.model.accountDataHandler.getCurrentAccount().folderToMessages.get(DefaultFolderName.sent)!.add(sentMsgMid);
    let isToIsSelf = false;
    for (const to of sentTo.values()) {
      if (this.accountDataHandler.getCurrentAccount().aliases.includes(to)) {
        isToIsSelf = true;
      }
    }
    if (isToIsSelf) {
      const receivedMsg = new FullMessage(
        new Message(this.accountDataHandler.getCurrentAccount().aliases[0], sentSubject, int64(Date.now()), 1, true),
        sentTo,
        sentBody,
      );
      const receivedMsgMid = int64(messages.size);
      messages.set(int64(messages.size), receivedMsg);
      this.model.accountDataHandler.getCurrentAccount().folderToMessages.get(DefaultFolderName.inbox)!.add(receivedMsgMid);
      this.model.accountDataHandler.getCurrentAccount().threads.push(new Set([sentMsgMid, receivedMsgMid]));
    }
    this.compose.composeDraft = null;
  }

  private createAndAddSentReceivedMessage(to: string, subject: string): MessageId[] {
    const sentFakeMid = this.createOnlySentMessage(to, subject);
    if (this.accountDataHandler.getCurrentAccount().aliases.includes(this.canonicalEmail(to))) {
      const selfEmail = this.accountDataHandler.getCurrentAccount().aliases[0];
      const timestamp = currentTimeMs();
      const receivedFakeMid = int64(this.model.accountDataHandler.getCurrentAccount().messages.size + 1);
      const receivedMessage = new FullMessage(
        new Message(to, subject, timestamp + int64(1), 2, false),
        new Set<string>([selfEmail]),
      );
      this.model.accountDataHandler.getCurrentAccount().messages.set(receivedFakeMid, receivedMessage);
      this.model.accountDataHandler.getCurrentAccount().folderToMessages.get(DefaultFolderName.inbox)!.add(receivedFakeMid);
      this.model.accountDataHandler.getCurrentAccount().threads.push(new Set([sentFakeMid, receivedFakeMid]));
      return [sentFakeMid, receivedFakeMid];
    }
    return [];
  }

  private canonicalEmail(email: string): string {
    let cnt = 0
    for (const i of range(0, email.length)) {
      if (email.slice(i, i + 1) === '-') {
        cnt += 1
      }
    }
    if (cnt <= 1) {
      return email
    }
    // passport performs yandex-team-47907-42601@yandex.ru -> yandex-team-47907.42601@yandex.ru
    const pos = email.lastIndexOf('-')
    return `${email.slice(0, pos)}.${email.slice(pos + 1, email.length)}`
  }

  private createOnlySentMessage(to: string, subject: string): MessageId {
    const selfEmail = this.accountDataHandler.getCurrentAccount().aliases[0];
    const timestamp = currentTimeMs();
    const sentMessage = new FullMessage(
      new Message(selfEmail, subject, timestamp, 2, true),
      new Set<string>([to]),
    );
    const msgSentMid = int64(this.model.accountDataHandler.getCurrentAccount().messages.size + 1);
    this.model.accountDataHandler.getCurrentAccount().messages.set(msgSentMid, sentMessage);
    this.model.accountDataHandler.getCurrentAccount().folderToMessages.get(DefaultFolderName.sent)!.add(msgSentMid)
    return msgSentMid;
  }
}
