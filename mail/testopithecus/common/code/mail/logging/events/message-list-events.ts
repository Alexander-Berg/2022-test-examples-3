import { Int32, Int64 } from '../../../../ys/ys';
import { TestopithecusEvent } from '../testopithecus-event';
import { ValueMapBuilder } from '../value-map-builder';
import { EventNames } from './event-names';
import { Testopithecus } from './testopithecus';

/**
 * События для списков писем. Используются для различных папок, поиска
 */
export class MessageListEvents {

  /**
   * Открытие письма (или треда) из списка нажатием на него (должен открыться именно просмотр письма)
   *
   * Для Android и iOS учитывается так же и открытие треда (когда после открывается отдельный экран с просмотром)
   *
   * @param order Порядковый номер письма в списке
   * @param mid Идентификатор открытого сообщения (в случае треда используется mid последнего сообщения треда)
   */
  public openMessage(order: Int32, mid: Int64): TestopithecusEvent {
    if (order < 0) {
      return Testopithecus.eventCreationErrorEvent(EventNames.LIST_MESSAGE_OPEN, `Order must be equal or greater then 0. Was: ${order}`)
    }
    return new TestopithecusEvent(EventNames.LIST_MESSAGE_OPEN, ValueMapBuilder.userEvent().addOrder(order).addMid(mid))
  }

  /**
   * Удаление письма (или треда) из списка при помощи атомарного действия.
   *
   * Для Android, iOS, touch через длинный свайп влево или короткий свайп + клик на удаление
   * Для Web удаление письма через его перенос
   *
   * @param order Порядковый номер письма в списке
   * @param mid Идентификатор удаленного сообщения (в случае треда используется mid последнего сообщения треда)
   */
  public deleteMessage(order: Int32, mid: Int64): TestopithecusEvent {
    if (order < 0) {
      return Testopithecus.eventCreationErrorEvent(EventNames.LIST_MESSAGE_DELETE, `Order must be equal or greater then 0. Was: ${order}`)
    }
    return new TestopithecusEvent(EventNames.LIST_MESSAGE_DELETE, ValueMapBuilder.userEvent().addOrder(order).addMid(mid))
  }

  /**
   * Открытие списка дополнительных действий с письмом из списка писем.
   *
   * Для Android, iOS, свайп + нажатие на три точки
   * Для Web отутсвует
   *
   * @param order Порядковый номер письма в списке
   * @param mid Идентификатор сообщения (в случае треда используется mid последнего сообщения треда)
   */
  public openMessageActions(order: Int32, mid: Int64): TestopithecusEvent {
    if (order < 0) {
      return Testopithecus.eventCreationErrorEvent(EventNames.LIST_MESSAGE_OPEN_ACTIONS, `Order must be equal or greater then 0. Was: ${order}`)
    }
    return new TestopithecusEvent(EventNames.LIST_MESSAGE_OPEN_ACTIONS, ValueMapBuilder.userEvent().addOrder(order).addMid(mid))
  }

  // TODO
  public refreshMessageList(): TestopithecusEvent {
    return new TestopithecusEvent(EventNames.LIST_MESSAGE_REFRESH, ValueMapBuilder.userEvent());
  }

  /**
   * Нажатие на кнопку написания нового сообщения
   */
  public writeNewMessage(): TestopithecusEvent {
    return new TestopithecusEvent(EventNames.LIST_MESSAGE_WRITE_NEW_MESSAGE, ValueMapBuilder.userEvent());
  }

  /**
   * Пометка письма (или треда) прочитанным при помощи атомарного действия
   *
   * Для Android, iOS, touch через коороткий свайп вправо
   * Для Web пометка письма через его перенос на кнопку "Прочитано"
   *
   * @param order Порядковый номер письма в списке
   * @param mid Идентификатор помеченного сообщения (в случае треда используется mid последнего сообщения треда)
   */
  public markMessageAsRead(order: Int32, mid: Int64): TestopithecusEvent {
    if (order < 0) {
      return Testopithecus.eventCreationErrorEvent(EventNames.LIST_MESSAGE_MARK_AS_READ, `Order must be equal or greater then 0. Was: ${order}`)
    }
    return new TestopithecusEvent(EventNames.LIST_MESSAGE_MARK_AS_READ, ValueMapBuilder.userEvent().addOrder(order).addMid(mid))
  }

  /**
   * Пометка письма (или треда) непрочитанным при помощи атомарного действия
   *
   * Для Android, iOS, touch через короткий свайп вправо
   * Для Web пометка письма через его перенос на кнопку "Непрочитно"
   *
   * @param order Порядковый номер письма в списке
   * @param mid Идентификатор помеченного сообщения (в случае треда используется mid последнего сообщения треда)
   */
  public markMessageAsUnread(order: Int32, mid: Int64): TestopithecusEvent {
    if (order < 0) {
      return Testopithecus.eventCreationErrorEvent(EventNames.LIST_MESSAGE_MARK_AS_UNREAD, `Order must be equal or greater then 0. Was: ${order}`)
    }
    return new TestopithecusEvent(EventNames.LIST_MESSAGE_MARK_AS_UNREAD, ValueMapBuilder.userEvent().addOrder(order).addMid(mid))
  }

  /**
   * Пометка письма (или треда) прочитаным или непрочитанным при помощи атомарного действия
   *
   * Для Android, iOS, touch через короткий свайп вправо
   * Для Web пометка письма через его перенос на соответствующую кнопку
   *
   * Внути вызывает один из методов {@link markMessageAsUnread} {@link markMessageAsRead}
   *
   * @param read True, если письмо помечено как прочитанное и false в ином случае
   * @param order Порядковый номер письма в списке
   * @param mid Идентификатор помеченного сообщения (в случае треда используется mid последнего сообщения треда)
   */
  public toggleMarkMessageAsRead(read: boolean, order: Int32, mid: Int64): TestopithecusEvent {
    return read ? this.markMessageAsRead(order, mid) : this.markMessageAsUnread(order, mid)
  }

}
