import { LoggingEvent } from './testopithecus-event'

/**
 * Интерфейс для сообщения о происходящих событиях.
 *
 * В базовом варианте предназначен для логирования событий.
 */
export interface EventReporter {

  /**
   * Сообщает о произошедшем событии
   *
   * @param event Событие, о котором необходимо сообщить
   */
  report(event: LoggingEvent): void

}

/**
 * Пустая заглушка, которая игнорирует все события
 */
export class EmptyEventReporter implements EventReporter {

  public report(event: LoggingEvent): void {}

}
