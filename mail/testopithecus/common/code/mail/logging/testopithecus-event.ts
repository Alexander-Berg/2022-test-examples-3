import { Int32, Int64, Nullable, undefinedToNull } from '../../../ys/ys'
import { EventReporter} from './event-reporter'
import { IntegerJSONItem, JSONItem, JSONItemGetValue, JSONItemKind } from './json-types'
import { TestopithecusRegistry } from './testopithecus-registry'
import { ValueMapBuilder } from './value-map-builder'

/**
 * Описывает произошедшее в приложении событие. Состоит из имени и словаря атрибутов
 */
export class TestopithecusEvent {

  private readonly value: Map<string, JSONItem>

  /**
   * Создает новое событие. В момент создания в словарь дополнительно добавляется атрибут с его именем.
   *
   * @param name Имя произошедшего события
   * @param builder Словарь с атрибутами
   */
  constructor(public readonly name: string, builder: ValueMapBuilder) {
    this.value = builder.setEventName(name).build()
  }

  public getAttributes(): Map<string, any> {
    const result = new Map<string, any>()
    this.value.forEach((v, k) => {
      const val = JSONItemGetValue(v)
      if (val !== null) {
        result.set(k, val)
      }
    })
    return result
  }

  public getInt64(attribute: string): Nullable<Int64> {
    const value = undefinedToNull(this.value.get(attribute))
    if (value === null) {
      return null
    }
    if (value.kind !== JSONItemKind.integer) {
      return null
    }
    return (value as IntegerJSONItem).asInt64()
  }

  public getInt32(attribute: string): Nullable<Int32> {
    const value = undefinedToNull(this.value.get(attribute))
    if (value === null) {
      return null
    }
    if (value.kind !== JSONItemKind.integer) {
      return null
    }
    return (value as IntegerJSONItem).asInt32()
  }

  /**
   * Уведомляет о произошедшем событии используя переданный обработчик
   *
   * В момент обработки добавляется текущий timestamp
   *
   * @param reporter Обработчик, который необходимо использовать
   */
  public reportVia(reporter: EventReporter): void {
    let aggregator = TestopithecusRegistry.aggregatorProvider().getAggregator()
    if (TestopithecusRegistry.aggregatorProvider().updateAggregator(this)) {
      TestopithecusEvent.reportIfPresent(reporter, aggregator.finalize())
      aggregator = TestopithecusRegistry.aggregatorProvider().getAggregator()
    }
    TestopithecusEvent.reportIfPresent(reporter, aggregator.accept(this))
  }

  /**
   * Уведомляет о произошедшем событии используя стандартный обработчик.
   *
   * В момент обработки добавляется текущий timestamp
   */
  public report(): void {
    this.reportVia(TestopithecusRegistry.eventReporter())
  }

  /**
   * Сообщает о событии, которое может быть null. В случаее переданного null, событие игнорируется
   *
   * @param reporter Необходимый обработчик
   * @param event Событие, о котором необходимо сообщить
   */
  private static reportIfPresent(reporter: EventReporter, event: Nullable<TestopithecusEvent>): void {
    if (event !== null) {
      reporter.report(LoggingEvent.fromTestopithecusEvent(event))
    }
  }
}

export class LoggingEvent {
  constructor(public readonly name: string, public readonly attributes: Map<string, any>) {
  }

  public static fromTestopithecusEvent(event: TestopithecusEvent): LoggingEvent {
    const attributes = event.getAttributes()
    attributes.set('timestamp', TestopithecusRegistry.timeProvider.getCurrentTimeMs())
    return new LoggingEvent(LoggingEvent.getFormattedName(event), attributes)
  }

  private static getFormattedName(event: TestopithecusEvent): string {
    return 'TESTOPITHECUS_EVENT_' + event.name
  }

}
