import { Int32, Int64, Nullable } from '../../../ys/ys'
import { ArrayJSONItem, BooleanJSONItem, IntegerJSONItem, JSONItem, StringJSONItem } from './json-types'
import { MessageDTO } from './objects/message'

/**
 * Класс позваляющий в итеративном формате создавать словари атрибутов для событий
 */
export class ValueMapBuilder {

  private readonly map: Map<string, JSONItem> = new Map<string, JSONItem>()

  private constructor(map: ReadonlyMap<string, JSONItem> = new Map<string, JSONItem>()) {
    const self = this
    map.forEach((v, k) => self.map.set(k, v))
  }

  public static userEvent(): ValueMapBuilder {
    return new ValueMapBuilder().setString('event_type', 'user')
  }

  public static systemEvent(): ValueMapBuilder {
    return new ValueMapBuilder().setString('event_type', 'system')
  }

  public static modelSyncEvent(): ValueMapBuilder {
    return new ValueMapBuilder().setString('event_type', 'model_sync')
  }

  public static customEvent(source: string, map: Map<string, JSONItem> = new Map<string, JSONItem>()): ValueMapBuilder {
    return new ValueMapBuilder(map)
      .setString('event_type', 'other')
      .setString('event_source', source)
  }

  public static __parse(map: ReadonlyMap<string, JSONItem>): ValueMapBuilder {
    return new ValueMapBuilder(map)
  }

  public setEventName(name: string): ValueMapBuilder {
    return this.setString('event_name', name)
  }

  /**
   * Добавляет индикатор стартового события
   */
  public addStartEvent(): ValueMapBuilder {
    return this.setBoolean('start_event', true)
  }

  /**
   * Добавляет значения для порядкового номера
   *
   * @param order Необходимое значение для добавления
   */
  public addOrder(order: Int32): ValueMapBuilder {
    return this.setInt32('order', order)
  }

  /**
   * Добавляет значения для счетчика количества
   *
   * @param count Необходимое значение для добавления
   */
  public addCount(count: Int32): ValueMapBuilder {
    return this.setInt32('count', count)
  }

  /**
   * Добавляет значения для счетчика количества быстрых ответов
   *
   * @param repliesNumber Необходимое значение для добавления
   */
  public addRepliesNumber(repliesNumber: Nullable<Int32>): ValueMapBuilder {
    if (repliesNumber !== null) {
      return this.setInt32('repliesNumber', repliesNumber)
    }
    return this
  }

  /**
   * Добавляет значения для счетчика количества быстрых ответов для списка писем
   *
   * @param repliesNumbers Необходимое значение для добавления
   */
  public addRepliesNumbers(repliesNumbers: Nullable<Int32[]>): ValueMapBuilder {
    if (repliesNumbers !== null) {
      return this.setInt32Array('repliesNumbers', repliesNumbers)
    }
    return this
  }

  /**
   * Добавляет значения для длины поля
   *
   * @param length Необходимое значение для добавления
   */
  public addLength(length: Nullable<Int32>): ValueMapBuilder {
    if (length !== null) {
      return this.setInt32('length', length)
    }
    return this
  }

  /**
   * Добавляет значения для идентификатора пользователя
   *
   * @param uid Необходимое значение для добавления
   */
  public addUid(uid: Nullable<Int64>): ValueMapBuilder {
    if (uid !== null) {
      return this.setInt64('uid', uid)
    }
    return this
  }

  /**
   * Добавляет значения для идентификатора письма
   *
   * @param mid Необходимое значение для добавления
   */
  public addMid(mid: Nullable<Int64>): ValueMapBuilder {
    if (mid !== null) {
      return this.setInt64('mid', mid)
    }
    return this
  }

  /**
   * Добавляет значения для идентификаторов писем
   *
   * @param mids Необходимое значение для добавления
   */
  public addMids(mids: Nullable<Int64[]>): ValueMapBuilder {
    if (mids !== null) {
      return this.setInt64Array('mids', mids)
    }
    return this
  }

  /**
   * Добавляет значения для идентификатора папки
   *
   * @param fid Необходимое значение для добавления
   */
  public addFid(fid: Int64): ValueMapBuilder {
    return this.setInt64('fid', fid)
  }

  /**
   * Добавляет значения для идентификатора треда
   *
   * @param tid Необходимое значение для добавления
   */
  public addTid(tid: Int64): ValueMapBuilder {
    return this.setInt64('tid', tid)
  }

  public addMessages(messages: Nullable<MessageDTO[]>): ValueMapBuilder {
    if (messages !== null) {
      const array = new ArrayJSONItem()
      for (const message of messages) {
        array.add(message.toJson())
      }
      this.map.set('messages', array)
    }
    return this
  }

  public addDebug(): ValueMapBuilder {
    return this.setBoolean('debug', true)
  }

  public addError(): ValueMapBuilder {
    return this.setBoolean('error', true)
  }

  public addReason(reason: string): ValueMapBuilder {
    return this.setString('reason', reason)
  }

  public addEvent(event: string): ValueMapBuilder {
    return this.setString('event', event)
  }

  public addSaveDraft(saveDraft: boolean): ValueMapBuilder {
    return this.setBoolean('saveDraft', saveDraft)
  }

  /**
   * Создает словарь атрибутов
   */
  public build(): Map<string, JSONItem> {
    return this.map
  }

  private setBoolean(name: string, value: boolean): ValueMapBuilder {
    this.map.set(name, new BooleanJSONItem(value))
    return this
  }

  private setInt32(name: string, value: Int32): ValueMapBuilder {
    this.map.set(name, IntegerJSONItem.fromInt32(value))
    return this
  }

  private setInt64(name: string, value: Int64): ValueMapBuilder {
    this.map.set(name, IntegerJSONItem.fromInt64(value))
    return this
  }

  private setString(name: string, value: string): ValueMapBuilder {
    this.map.set(name, new StringJSONItem(value))
    return this
  }

  private setInt32Array(name: string, value: Int32[]): ValueMapBuilder {
    this.map.set(name, new ArrayJSONItem(value.map((n) => IntegerJSONItem.fromInt32(n))))
    return this
  }

  private setInt64Array(name: string, value: Int64[]): ValueMapBuilder {
    this.map.set(name, new ArrayJSONItem(value.map((n) => IntegerJSONItem.fromInt64(n))))
    return this
  }
}
