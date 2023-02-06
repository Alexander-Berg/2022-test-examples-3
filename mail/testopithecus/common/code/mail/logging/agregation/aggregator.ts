import { Nullable } from '../../../../ys/ys';
import { TestopithecusEvent } from '../testopithecus-event';

export interface Aggregator {

  accepts(event: TestopithecusEvent): boolean

  accept(event: TestopithecusEvent): Nullable<TestopithecusEvent>

  finalize(): Nullable<TestopithecusEvent>

}

export class EmptyAggregator implements Aggregator {

  public accept(event: TestopithecusEvent): Nullable<TestopithecusEvent> {
    return event;
  }

  public accepts(event: TestopithecusEvent): boolean {
    return true;
  }

  public finalize(): Nullable<TestopithecusEvent> {
    return null;
  }

}
