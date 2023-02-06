import { Nullable } from '../../ys/ys'

export class StringBuilder {
  private readonly strings: string[] = [];
  private result: Nullable<string> = null;

  // tslint:disable-next-line:no-empty
  public constructor() { }

  public add(value: string): StringBuilder {
    this.strings.push(value);
    return this
  }

  public addLine(value: string): StringBuilder {
    return this.add(value).add('\n')
  }

  public build(): string {
    if (this.result !== null) {
      return this.result!
    }
    this.result = this.strings.join('');
    return this.result!
  }
}
