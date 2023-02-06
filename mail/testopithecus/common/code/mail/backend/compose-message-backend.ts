import { MailboxClientHandler } from '../../client/mailbox-client';
import { ComposeMessage, DraftView } from '../mail-features';
import { Draft } from '../model/compose-message-model';
import { WysiwygModel } from '../model/wysiwyg-model';

export class ComposeMessageBackend implements ComposeMessage {
  constructor(private clientsHandler: MailboxClientHandler) {
  }

  public goToMessageReply(): void {
    // TODO
  }

  public setBody(body: string): void {
  }

  public setSubject(subject: string): void {
  }

  public addTo(to: string): void {
  }

  public addToUsingSuggest(to: string): void {
  }

  public clearBody(): void {
  }

  public clearSubject(): void {
  }

  public getDraft(): DraftView {
    return new Draft(new WysiwygModel());
  }

  public getTo(): Set<string> {
    return new Set<string>();
  }

  public removeTo(order: number): void {
  }
}
