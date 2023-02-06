import { TestopithecusEvent } from '../testopithecus-event';
import { ValueMapBuilder } from '../value-map-builder';
import { EventNames } from './event-names';

/**
 * События означающие начало работы пользователя с приложением
 */
export class StartEvents {

  /**
   * Событие инициализации приложения с отображения списка писем
   */
  public startWithMessageListShow(): TestopithecusEvent {
    return new TestopithecusEvent(EventNames.START_WITH_MESSAGE_LIST, ValueMapBuilder.userEvent().addStartEvent())
  }

  /**
   * Событие инициализации приложения с открытия по нотификации
   */
  public startFromMessageNotification(): TestopithecusEvent {
    return new TestopithecusEvent(
      EventNames.START_FROM_MESSAGE_NOTIFICATION,
      ValueMapBuilder.userEvent().addStartEvent(),
    )
  }

  /**
   * Событие инициализации приложения с открытия из виджета
   */
  public startFromWidget(): TestopithecusEvent {
    return new TestopithecusEvent(EventNames.START_FROM_WIDGET, ValueMapBuilder.userEvent().addStartEvent())
  }

}
