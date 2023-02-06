import { MessageDTO } from '../../code/mail/logging/objects/message';
import { Int32, Int64, Nullable } from '../../ys/ys';

export class MailContext {

  public messages: Map<Int64, MessageDTO> = new Map<Int64, MessageDTO>()

  // mid to reply number
  public pushes: Map<Int64, Int32> = new Map<Int64, Int32>()

  public currentMessageId: Nullable<Int64> = null

}
