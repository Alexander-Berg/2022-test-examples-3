import { Login, MultiAccount } from '../mail-features';
import { MailAppModelHandler } from './mail-model';

export class MultiAccountModel implements MultiAccount {
  constructor(private accountDataHandler: MailAppModelHandler) {
  }

  public switchToAccount(login: string): void {
    this.accountDataHandler.switchToAccount(login);
  }

  public getLoggedInAccountsList(): Login[] {
    return this.accountDataHandler.getLoggedInAccounts().map((acc) => acc.login);
  }

  public addNewAccount(): void {
   // В модели ничего не делаем
  }
}