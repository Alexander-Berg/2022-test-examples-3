import { Int32, Int64 } from '../../../../ys/ys';
import { TestopithecusEvent } from '../testopithecus-event';
import { ValueMapBuilder } from '../value-map-builder';
import { EventNames } from './event-names';
import { Testopithecus } from './testopithecus';

/**
 * Действия связанные с групповыми операциями (ипспользуется в списке писем)
 */
export class GroupActionsEvents {

  /**
   * Добавление письма (или треда) к набору писем для применения групповых операций
   *
   * @param order Порядковый номер письма в общем списке писем
   * @param mid Идентификатор выделенного письма (в случае треда используется mid последнего сообщения треда)
   */
  public selectMessage(order: Int32, mid: Int64): TestopithecusEvent {
    if (order < 0) {
      return Testopithecus.eventCreationErrorEvent(EventNames.GROUP_MESSAGE_SELECT, `Order must be equal or greater then 0. Was: ${order}`)
    }
    return new TestopithecusEvent(
      EventNames.GROUP_MESSAGE_DESELECT,
      ValueMapBuilder.userEvent().addOrder(order).addMid(mid),
    )
  }

  /**
   * Убирание письма (или треда) из набора писем для применения групповых операций
   *
   * @param order Порядковый номер письма в общем списке писем
   * @param mid Идентификатор убранного письма (в случае треда используется mid последнего сообщения треда)
   */
  public deselectMessage(order: Int32, mid: Int64): TestopithecusEvent {
    if (order < 0) {
      return Testopithecus.eventCreationErrorEvent(EventNames.GROUP_MESSAGE_DESELECT, `Order must be equal or greater then 0. Was: ${order}`)
    }
    return new TestopithecusEvent(
      EventNames.GROUP_MESSAGE_DESELECT,
      ValueMapBuilder.userEvent().addOrder(order).addMid(mid),
    )
  }

  /**
   * Атомарное удаление всех выдленных писем
   *
   * Нажатие на кнопку удаления
   * Для Web дополнительно через перенос группы писем на кнопку удаления
   */
  public deleteSelectedMessages(): TestopithecusEvent {
    return new TestopithecusEvent(EventNames.GROUP_DELETE_SELECTED, ValueMapBuilder.userEvent())
  }

  /**
   * Атомарное пометка всех выдленных писем как прочитанные
   *
   * Нажатие на кнопку пометки как прочитанное
   * Для Web дополнительно через перенос группы писем на кнопку пометки как прочитанное
   */
  public markAsReadSelectedMessages(): TestopithecusEvent {
    return new TestopithecusEvent(EventNames.GROUP_MARK_AS_READ_SELECTED, ValueMapBuilder.userEvent())
  }

  /**
   * Атомарное пометка всех выдленных писем как непрочитанные
   *
   * Нажатие на кнопку пометки как непрочитанное
   * Для Web дополнительно через перенос группы писем на кнопку пометки как непрочитанное
   */
  public markAsUnreadSelectedMessages(): TestopithecusEvent {
    return new TestopithecusEvent(EventNames.GROUP_MARK_AS_UNREAD_SELECTED, ValueMapBuilder.userEvent())
  }

}
