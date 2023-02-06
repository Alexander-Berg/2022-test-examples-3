import { Int32, Nullable } from '../../../../ys/ys';
import { TestopithecusEvent } from '../testopithecus-event';
import { ValueMapBuilder } from '../value-map-builder';
import { EventNames } from './event-names';

/**
 * События для экрана написания письма.
 */
export class ComposeEvents {

  // TODO
  public addReceiver(): TestopithecusEvent {
    return new TestopithecusEvent(EventNames.COMPOSE_ADD_RECEIVER, ValueMapBuilder.userEvent())
  }

  // TODO
  public removeReceiver(): TestopithecusEvent {
    return new TestopithecusEvent(EventNames.COMPOSE_REMOVE_RECEIVER, ValueMapBuilder.userEvent())
  }

  // TODO
  public setSubject(): TestopithecusEvent {
    return new TestopithecusEvent(EventNames.COMPOSE_SET_SUBJECT, ValueMapBuilder.userEvent())
  }

  /**
   * При каждом изменении текста письма пользователем (именно текста, без учета аттачей)
   *
   * @param length Длина "текста" (для Android логируется длина контента, включая html теги)
   */
  public editBody(length: Nullable<Int32> = null): TestopithecusEvent {
    return new TestopithecusEvent(EventNames.COMPOSE_EDIT_BODY, ValueMapBuilder.userEvent().addLength(length))
  }

  /**
   * При каждом добавлении аттачей к письму
   *
   * @param count количество добавленных файлов
   */
  public addAttachments(count: Int32): TestopithecusEvent {
    return new TestopithecusEvent(EventNames.COMPOSE_ADD_ATTACHMENTS, ValueMapBuilder.userEvent().addCount(count))
  }

  // TODO
  public removeAttachment(): TestopithecusEvent {
    return new TestopithecusEvent(EventNames.COMPOSE_REMOVE_ATTACHMENT, ValueMapBuilder.userEvent())
  }

  /**
   * Нажатие на кнопку отправки сообщения
   */
  public sendMessage(): TestopithecusEvent {
    return new TestopithecusEvent(EventNames.COMPOSE_SEND_MESSAGE, ValueMapBuilder.userEvent())
  }

  /**
   * Выход из экрана написания письма нажатием на кнопку назад (в том числе при нажатии на кнопку назад телефона или планшета).
   * На данный момент возможно получить несколько подобных событий подряд при многократном нажатии.
   *
   * @param saveDraft Требуется ли сохранить(обновить) черновик. Для Android сохранение происходит форсировано в случае
   * наличия изменений, для iOS пользователь имеет возможность не делать сохранение
   */
  public pressBack(saveDraft: boolean): TestopithecusEvent {
    return new TestopithecusEvent(EventNames.COMPOSE_BACK, ValueMapBuilder.userEvent().addSaveDraft(saveDraft))
  }

}
