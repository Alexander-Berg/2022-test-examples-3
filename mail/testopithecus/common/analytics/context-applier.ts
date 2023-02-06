import { TestopithecusEvent } from '../code/mail/logging/testopithecus-event';

export interface ContextApplier<C> {

  init(): C

  apply(event: TestopithecusEvent, context: C): C

}

export class NullContextApplier implements ContextApplier<null> {

  public init(): null {
    return null;
  }

  public apply(event: TestopithecusEvent, context: null): null {
    return null;
  }

}
