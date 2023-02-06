import { Int32, Int64, Nullable } from '../../../../ys/ys';
import { TestopithecusEvent } from '../testopithecus-event';
import { ValueMapBuilder } from '../value-map-builder';
import { EventNames } from './event-names';

/**
 * Набор событий связанных с различными действиями с нотификациями
 */
export class PushEvents {

  // TODO Offline? Grouped? Updates?
  /**
   * Отображение нотификации о новых сообщениях.
   *
   * @param uid Идентификатор аккаунта для которого была показана нотификация
   * @param fid Идентификатор папки в которой лежит письмо
   * @param mids Идентификаторы новых писем для отображения
   * @param repliesNumbers Количество быстрых ответов для каждой нотификации
   */
  public messagesReceivedPushShown(uid: Int64, fid: Int64, mids: Int64[], repliesNumbers: Nullable<Int32[]> = null): TestopithecusEvent {
    return new TestopithecusEvent(
      EventNames.PUSH_MESSAGES_RECEIVED_SHOWN,
      ValueMapBuilder.systemEvent().addUid(uid).addFid(fid).addMids(mids).addRepliesNumbers(repliesNumbers),
    );
  }

  /**
   * Открытие одного письма из нотификации
   *
   * @param uid идентификатор аккаунта для которого была показана нотификация
   * @param mid идентификатор открытого письма
   * @param fid идентификатор папки в которой лежит письмо
   * @param repliesNumber количество предложенных быстрых ответов
   */
  public singleMessagePushClicked(uid: Int64, mid: Int64, fid: Int64, repliesNumber: Nullable<Int32> = null): TestopithecusEvent {
    return new TestopithecusEvent(
      EventNames.PUSH_SINGLE_MESSAGE_CLICKED,
      ValueMapBuilder.userEvent().addUid(uid).addMid(mid).addFid(fid).addRepliesNumber(repliesNumber),
    )
  }

  /**
   * Открытие компоуза через использование кнопки ответа в нотификации письма
   *
   * @param uid идентификатор аккаунта для которого была использована нотификация
   * @param mid идентификатор письма нотификации
   * @param fid идентификатор папки в которой лежит письмо
   * @param repliesNumber количество предложенных быстрых ответов
   */
  public replyMessagePushClicked(uid: Int64, mid: Int64, fid: Int64, repliesNumber: Nullable<Int32> = null): TestopithecusEvent {
    return new TestopithecusEvent(
      EventNames.PUSH_REPLY_MESSAGE_CLICKED,
      ValueMapBuilder.userEvent().addUid(uid).addMid(mid).addFid(fid).addRepliesNumber(repliesNumber),
    )
  }

  /**
   * Открытие копоуза через использование кнопки умного ответа в нотификации письма
   *
   * @param uid идентификатор аккаунта для которого была показана нотификация
   * @param mid идентификатор письма нотификации
   * @param fid идентификатор папки в которой лежит письмо
   * @param order номер использованного умного ответа (отсчет с нуля)
   * @param repliesNumber количество предложенных быстрых ответов
   */
  public smartReplyMessagePushClicked(uid: Int64, mid: Int64, fid: Int64, order: Int32, repliesNumber: Nullable<Int32> = null): TestopithecusEvent {
    return new TestopithecusEvent(
      EventNames.PUSH_SMART_REPLY_MESSAGE_CLICKED,
      ValueMapBuilder.userEvent().addUid(uid).addMid(mid).addFid(fid).addOrder(order).addRepliesNumber(repliesNumber),
    )
  }

  /**
   * Открытие треда через нажатие на групповую нотификацию о новых письмах
   *
   * @param uid идентификатор аккаунта для которого была использована нотификация
   * @param mids идентификаторы письмем, отображенных в нотификации
   * @param fid идентификатор папки в которой лежит письмо
   * @param tid номер использованного умного ответа (отсчет с нуля)
   * @param repliesNumber количество предложенных быстрых ответов
   */
  public threadPushClicked(uid: Int64, mids: Int64[], fid: Int64, tid: Int64, repliesNumber: Nullable<Int32> = null): TestopithecusEvent {
    return new TestopithecusEvent(
      EventNames.PUSH_THREAD_CLICKED,
      ValueMapBuilder.userEvent().addUid(uid).addMids(mids).addTid(tid).addFid(fid).addRepliesNumber(repliesNumber),
    )
  }

  /**
   * Открытие папки через нажатие на групповую нотификацию о новых письмах
   *
   * @param uid идентификатор аккаунта для которого была использована нотификация
   * @param mids идентификаторы письмем, отображенных в нотификации
   * @param fid идентификатор папки в которой лежит письмо
   */
  public folderPushClicked(uid: Int64, mids: Int64[], fid: Int64): TestopithecusEvent {
    return new TestopithecusEvent(
      EventNames.PUSH_FOLDER_CLICKED,
      ValueMapBuilder.userEvent().addUid(uid).addMids(mids).addFid(fid),
    )
  }

}
