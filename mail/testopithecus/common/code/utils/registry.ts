import { Nullable } from '../../ys/ys';
import { Logger } from './logger';
import { RandomProvider } from './random';

export class Registry {
  private static instance: Registry = new Registry();

  private constructor() {
  }

  public logger: Nullable<Logger> = null;

  public static get(): Registry {
    return this.instance;
  }
}
