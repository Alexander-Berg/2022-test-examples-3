import { TestopithecusEvent } from '../testopithecus-event';
import { ValueMapBuilder } from '../value-map-builder';
import { EventNames } from './event-names';

/**
 * Окно дополнительных действий с письмом или тредом (для Web отсутствует)
 */
export class MessageActionsEvents {

  /**
   * Нажатие на кнопку ответа (доступно только для писем) в меню дополнительных действий с письмом
   */
  public reply(): TestopithecusEvent {
    return new TestopithecusEvent(EventNames.MESSAGE_ACTION_REPLY, ValueMapBuilder.userEvent())
  }

  /**
   * Нажатие на кнопку ответа всем (доступно только для писем) в меню дополнительных действий с письмом
   */
  public replyAll(): TestopithecusEvent {
    return new TestopithecusEvent(EventNames.MESSAGE_ACTION_REPLY_ALL, ValueMapBuilder.userEvent())
  }

  /**
   * Нажатие на кнопку переслать (доступно только для писем) в меню дополнительных действий с письмом
   */
  public forward(): TestopithecusEvent {
    return new TestopithecusEvent(EventNames.MESSAGE_ACTION_FORWARD, ValueMapBuilder.userEvent())
  }

  /**
   * Нажатие на кнопку удаления в меню дополнительных действий с письмом
   */
  public delete(): TestopithecusEvent {
    return new TestopithecusEvent(EventNames.MESSAGE_ACTION_DELETE, ValueMapBuilder.userEvent())
  }

  /**
   * Нажатие на кнопку пометки как прочитанное в меню дополнительных действий с письмом
   */
  public markAsRead(): TestopithecusEvent {
    return new TestopithecusEvent(EventNames.MESSAGE_ACTION_MARK_AS_READ, ValueMapBuilder.userEvent())
  }

  /**
   * Нажатие на кнопку пометки как непрочитанное в меню дополнительных действий с письмом
   */
  public markAsUnread(): TestopithecusEvent {
    return new TestopithecusEvent(EventNames.MESSAGE_ACTION_MARK_AS_UNREAD, ValueMapBuilder.userEvent())
  }

  /**
   * Нажатие на кнопку пометки как важное в меню дополнительных действий с письмом
   */
  public markAsImportant(): TestopithecusEvent {
    return new TestopithecusEvent(EventNames.MESSAGE_ACTION_MARK_AS_IMPORTANT, ValueMapBuilder.userEvent())
  }

  /**
   * Нажатие на кнопку пометки как неважное в меню дополнительных действий с письмом
   */
  public markAsNotImportant(): TestopithecusEvent {
    return new TestopithecusEvent(EventNames.MESSAGE_ACTION_MARK_AS_NOT_IMPORTANT, ValueMapBuilder.userEvent())
  }

  /**
   * Нажатие на кнопку пометки как спам в меню дополнительных действий с письмом
   */
  public markAsSpam(): TestopithecusEvent {
    return new TestopithecusEvent(EventNames.MESSAGE_ACTION_MARK_AS_SPAM, ValueMapBuilder.userEvent())
  }

  /**
   * Нажатие на кнопку пометки как ек спам в меню дополнительных действий с письмом
   */
  public markAsNotSpam(): TestopithecusEvent {
    return new TestopithecusEvent(EventNames.MESSAGE_ACTION_MARK_AS_NOT_SPAM, ValueMapBuilder.userEvent())
  }

  // TODO not done
  public moveToFolder(): TestopithecusEvent {
    return new TestopithecusEvent(EventNames.MESSAGE_ACTION_MOVE_TO_FOLDER, ValueMapBuilder.userEvent())
  }

  // TODO not done
  public markAs(): TestopithecusEvent {
    return new TestopithecusEvent(EventNames.MESSAGE_ACTION_MARK_AS, ValueMapBuilder.userEvent())
  }

  /**
   * Нажатие на кнопку пометки как архивации в меню дополнительных действий с письмом
   */
  public archive(): TestopithecusEvent {
    return new TestopithecusEvent(EventNames.MESSAGE_ACTION_ARCHIVE, ValueMapBuilder.userEvent())
  }

  /**
   * Закрытие меню дополнительных действий:
   * <ul>
   *   <li>Нажатие на кнопку отмены</li>
   *   <li>Нажатие на область вне экрана</li>
   *   <li>Нажатие на системную кнопку назад</li>
   * </ul>
   */
  public cancel(): TestopithecusEvent {
    return new TestopithecusEvent(EventNames.MESSAGE_ACTION_CANCEL, ValueMapBuilder.userEvent())
  }

}
