import { Int32, Nullable, range } from '../../../ys/ys';
import { ID } from '../../client/common/id';
import { FolderType } from '../../client/folder/folderDTO';
import { MailboxClientHandler } from '../../client/mailbox-client';
import { Email, stringToEmail } from '../../client/settings/settings-entities';
import { requireNonNull } from '../../utils/utils';
import { WriteMessage } from '../mail-features';
import { MessageId } from '../model/mail-model';
import { MessageListDisplayBackend } from './message-list-display-backend';

export class WriteMessageBackend implements WriteMessage {
  constructor(
    private messageListDisplayBackend: MessageListDisplayBackend,
    private clientsHandler: MailboxClientHandler,
  ) {}

  public sendMessage(to: string, subject: string): void {
    this.sendMessageWithText(to, subject, null);
  }

  public replyMessage(): void {
    // TODO
  }

  public openCompose(): void {
  }

  public getTo(): Set<string> {
    return new Set<string>();
  }

  public createThread(to: string, subject: string, size: Int32, texts: Nullable<string[]>): void {
    const targetTexts = texts === null ? [] : texts!;
    if (texts === null) {
      for (const i of range(0, size)) {
        targetTexts.push(`AUTOCREATED_THREAD_${i}`);
      }
    }
    const fid = this.messageListDisplayBackend.getFolderByType(FolderType.sent).fid
    const lastMid = this.getTopMessageMid(fid)
    this.clientsHandler.getCurrentClient().sendMessage(to, subject, targetTexts[0]);
    let newMid = lastMid;
    while (newMid === lastMid) {
      newMid = this.getTopMessageMid(fid)
    }
    const references = this.clientsHandler.getCurrentClient()
      .getMessageReference(requireNonNull(newMid, 'Message should appear!'));
    for (const i of range(1, size)) {
      this.clientsHandler.getCurrentClient().sendMessage(to, subject, targetTexts[i], references)
    }
  }

  public sendPrepared(): void {
  }

  private sendMessageWithText(to: string, subject: string, text: Nullable<string>): void {
    const toEmail = stringToEmail(to);
    let topMidToUpdate: Nullable<ID> = null;
    if (toEmail !== null && toEmail.login === this.getCurrentAccountEmail().login) {
      topMidToUpdate = this.getTopMessageMid(this.messageListDisplayBackend.getInbox().fid);
    }
    const textToSend = text !== null ? text! : 'AUTOCREATED_MESSAGE';
    this.clientsHandler.getCurrentClient().sendMessage(to, subject, textToSend);
    if (topMidToUpdate !== null) {
      let newMid: Nullable<MessageId> = topMidToUpdate;
      while (newMid === topMidToUpdate) {
        newMid = this.getTopMessageMid(this.messageListDisplayBackend.getInbox().fid)
      }
    }
  }

  private getCurrentAccountEmail(): Email {
    return this.clientsHandler.getCurrentClient().getSettings().payload!.accountInformation.emails[0]
  }

  private getTopMessageMid(fid: ID): Nullable<MessageId> {
    const messages = this.clientsHandler.getCurrentClient().getMessagesInFolder(fid, 1);
    if (messages.length === 0) {
      return null
    }
    return messages[0].mid
  }

}
