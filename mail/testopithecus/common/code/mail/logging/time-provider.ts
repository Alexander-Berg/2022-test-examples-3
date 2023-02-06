import { Int64 } from '../../../ys/ys'
import { currentTimeMs } from './logging-utils';

export interface TimeProvider {
  getCurrentTimeMs(): Int64
}

export class NativeTimeProvider implements TimeProvider {
  public getCurrentTimeMs(): Int64 {
    return currentTimeMs()
  }
}
