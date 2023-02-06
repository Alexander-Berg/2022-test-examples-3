import { Int32 } from '../../../../ys/ys';
import { TestopithecusEvent } from '../testopithecus-event';
import { ValueMapBuilder } from '../value-map-builder';
import { EventNames } from './event-names';

/**
 * Экран просмотра письма или треда.
 */
export class MessageEvents {

  /**
   * Выход из экрана просмотра письма (или треда) нажатием на кнопку назад (в том числе при нажатии на кнопку назад телефона или планшета).
   */
  public backToMailList(): TestopithecusEvent {
    return new TestopithecusEvent(EventNames.MESSAGE_VIEW_BACK, ValueMapBuilder.userEvent())
  }

  /**
   * Нажатие на кнопку удаления при просмотре письма или треда
   */
  public deleteMessage(): TestopithecusEvent {
    return new TestopithecusEvent(EventNames.MESSAGE_VIEW_DELETE, ValueMapBuilder.userEvent())
  }

  public editDraft(order: Int32): TestopithecusEvent {
    return new TestopithecusEvent(EventNames.MESSAGE_VIEW_EDIT_DRAFT, ValueMapBuilder.userEvent().addOrder(order))
  }

  /**
   * Нажатие на кнопку ответа в просмотре письма
   *
   * @param order Номер письма в треде (0, если письмо открыто вне треда)
   */
  public reply(order: Int32): TestopithecusEvent {
    return new TestopithecusEvent(EventNames.MESSAGE_VIEW_REPLY, ValueMapBuilder.userEvent().addOrder(order))
  }

  /**
   * Нажатие на кнопку ответа всем в просмотре письма
   *
   * @param order Номер письма в треде (0, если письмо открыто вне треда)
   */
  public replyAll(order: Int32): TestopithecusEvent {
    return new TestopithecusEvent(EventNames.MESSAGE_VIEW_REPLY_ALL, ValueMapBuilder.userEvent().addOrder(order))
  }

  /**
   * Нажатие на кнопку открытия дополнительных действий с письмом
   *
   * @param order Номер письма в треде (0, если письмо открыто вне треда)
   */
  public openMessageActions(order: Int32): TestopithecusEvent {
    return new TestopithecusEvent(EventNames.MESSAGE_VIEW_OPEN_ACTIONS, ValueMapBuilder.userEvent().addOrder(order))
  }
  }
