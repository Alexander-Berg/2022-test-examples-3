import { Logger } from '../../code/utils/logger';

export class ConsoleLog implements Logger {
  public static LOGGER = new ConsoleLog();

  private constructor() {
  }

  public log(message: string): void {
    // tslint:disable-next-line:no-console
    console.log(message)
  }
}
