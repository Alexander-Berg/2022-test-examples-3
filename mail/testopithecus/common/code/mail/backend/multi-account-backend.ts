import { MailboxClientHandler } from '../../client/mailbox-client';
import { Login, MultiAccount } from '../mail-features';

export class MultiAccountBackend implements MultiAccount {
  constructor(private clientsHandler: MailboxClientHandler) {}

  public switchToAccount(login: string): void {
    this.clientsHandler.switchToClientForAccountWithLogin(login)
  }

  public getLoggedInAccountsList(): Login[] {
    return this.clientsHandler.getLoggedInAccounts().map((acc) => acc.login);
  }

  public addNewAccount(): void {
    //на бекенде ничего не делаем
  }
}
