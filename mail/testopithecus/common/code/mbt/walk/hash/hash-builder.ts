import { int64, Int64, range, stringToInt64 } from '../../../../ys/ys';
import { MailboxModel } from '../../../mail/model/mail-model';

export class HashBuilder {

  private static mod: Int64 = stringToInt64('1125899839733759')!;
  private static multiplier: Int64 = int64(63);
  private hash: Int64 = int64(0);

  private static getHashOfString(str: string): Int64 {
    let hash: Int64 = int64(0);
    const multiplier: Int64 = int64(257);
    for (const i of range(0, str.length)) {
      const ch = str.charCodeAt(i);
      hash = (hash * multiplier + int64(ch)) % this.mod;
    }
    return hash;
  }

  public addInt64(number: Int64): HashBuilder {
    this.hash = (this.hash * HashBuilder.multiplier + number) % HashBuilder.mod;
    return this;
  }

  public addInt(number: number): HashBuilder {
    return this.addInt64(int64(number));
  }

  public addBoolean(condition: boolean): HashBuilder {
    return this.addInt64(int64(condition ? 1 : 0));
  }

  public addString(str: string): HashBuilder {
    return this.addInt64(HashBuilder.getHashOfString(str));
  }

  public build(): Int64 {
    return this.hash;
  }

}

export interface HashStrategy {
  getMailboxModelHash(model: MailboxModel): Int64
}
