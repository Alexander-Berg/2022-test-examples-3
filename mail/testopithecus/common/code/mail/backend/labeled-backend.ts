import { Int32 } from '../../../ys/ys';
import { LabelType } from '../../client/label/label';
import { MailboxClientHandler } from '../../client/mailbox-client';
import { MarkableImportant } from '../mail-features';
import { MessageListDisplayBackend } from './message-list-display-backend';

export class MarkableImportantBackend implements MarkableImportant {

  constructor(private mailListDisplayBackend: MessageListDisplayBackend, private clientsHandler: MailboxClientHandler) {
  }

  public markAsImportant(order: Int32): void {
    const tid = this.mailListDisplayBackend.getThreadMessage(order).tid!;
    const lid = this.clientsHandler.getCurrentClient().getLabelList()
      .filter((label) => label.type === LabelType.important)[0].lid;
    this.clientsHandler.getCurrentClient().markThreadWithLabel(tid, lid);
  }

  public markAsUnimportant(order: Int32): void {
    const tid = this.mailListDisplayBackend.getThreadMessage(order).tid!;
    const lid = this.clientsHandler.getCurrentClient().getLabelList()
      .filter((label) => label.type === LabelType.important)[0].lid;
    this.clientsHandler.getCurrentClient().unmarkThreadWithLabel(tid, lid);
  }
}
