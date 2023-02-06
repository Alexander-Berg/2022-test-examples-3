import { TestopithecusEvent } from '../../code/mail/logging/testopithecus-event';

export interface Evaluation<T, C> {

  name(): string

  /**
   * Принимает событие, произошедшие в данном контексте
   *
   * @param event произошедшие событие
   * @param context контекст, в котором произошло событие (до его применения)
   */
  acceptEvent(event: TestopithecusEvent, context: C): void

  result(): T

}
