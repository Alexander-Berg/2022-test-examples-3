import { Int32, Nullable, setToArray } from '../../../ys/ys'
import { requireNonNull } from '../../utils/utils'
import { ComposeMessage, DraftView, WysiwygView } from '../mail-features'
import { MailAppModelHandler } from './mail-model'
import { WysiwygModel } from './wysiwyg-model'

export class Draft implements DraftView {
  public subject: Nullable<string> = ''
  public to: Set<string> = new Set<string>()

  constructor(public wysiwig: WysiwygModel) {
  }

  public static matches(first: DraftView, second: DraftView): boolean {
    return first.getWysiwyg().getRichBody() === second.getWysiwyg().getRichBody()
      && first.subject === second.subject
      && Draft.isToEquals(first.to, second.to)
  }

  public static isToEquals(first: Set<string>, second: Set<string>): boolean {
    if (first.size !== second.size) { return false }
    for (const a of first.values()) { if (!second.has(a)) { return false } }
    return true
  }

  public tostring(): string {
    const subj = this.subject !== null ? this.subject! : ''
    let tos = ''
    for (const to of this.to.values()) {
      tos += `${to},`
    }
    return `Compose(to=${tos}, subject=${subj}, body=${this.getWysiwyg().getRichBody()})`
  }

  public copy(): Draft {
    const copy = new Draft(this.wysiwig)
    copy.subject = this.subject
    copy.to = this.to
    return copy
  }

  public getWysiwyg(): WysiwygView {
    return new Wysiwyg(this.wysiwig)
  }
}

export class Wysiwyg implements WysiwygView {

  constructor(public wysiwig: WysiwygModel) {
  }

  public getStyles(i: Int32): Set<string> {
    return this.wysiwig.getStyles()[i]
  }

  public getText(): string {
    return this.wysiwig.getSymbols().join('')
  }

  public getRichBody(): string {
    return this.wysiwig.getBody()
  }
}

export class ComposeMessageModel implements ComposeMessage {
  public composeDraft: Nullable<Draft> = null

  constructor(private wysiwyg: WysiwygModel, private readonly accountDataHandler: MailAppModelHandler) {
  }

  public getDraft(): DraftView {
    return this.demandDraft()
  }

  public goToMessageReply(): void {
    this.composeDraft = new Draft(this.wysiwyg)
    // TODO: fill draft values
  }

  public setBody(body: string): void {
    const draft = this.demandDraft()
    this.clearBody()
    draft.wysiwig.appendText(0, body)
  }

  public setSubject(subject: string): void {
    this.demandDraft().subject = subject
  }

  public addTo(to: string): void {
    this.demandDraft().to.add(to)
  }

  public addToUsingSuggest(to: string): void {
    this.demandDraft().to.add(this.accountDataHandler.getCurrentAccount().contacts.filter((contact) => contact.startsWith(to))[0])
  }

  public openCompose(): void {
    this.composeDraft = new Draft(this.wysiwyg)
  }

  public getTo(): Set<string> {
    return this.getDraft().to
  }

  public clearBody(): void {
    this.demandDraft().wysiwig.clear()
  }

  public clearSubject(): void {
    this.demandDraft().subject = null
  }

  public removeTo(order: number): void {
    let toToDelete = ''
    let currentToNumber = 0
    for (const to of setToArray(this.demandDraft().to)) {
      if (currentToNumber === order) {
        toToDelete = to
        break
      }
      currentToNumber += 1
    }
    this.demandDraft().to.delete(toToDelete)
  }

  private demandDraft(): Draft {
    return requireNonNull(this.composeDraft, `Draft wasn't created!`)
  }
}
