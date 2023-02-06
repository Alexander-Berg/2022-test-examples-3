import { Logger } from "../../code/utils/logger";
import { ConsoleLog } from './console-log';

export class MapReduceLog implements Logger {
  public static LOGGER = new MapReduceLog();

  private constructor() {
  }

  public log(message: string): void {
    // tslint:disable-next-line:no-console
    console.log(`{ "event": "${message}" }`)
  }
}
