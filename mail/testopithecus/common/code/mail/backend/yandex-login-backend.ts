import { MailboxClientHandler } from '../../client/mailbox-client'
import { UserAccount } from '../../users/user-pool'
import { YandexLogin } from '../mail-features'

export class YandexLoginBackend implements YandexLogin {
  public constructor(private clientsHandler: MailboxClientHandler) {}

  public loginWithYandexAccount(account: UserAccount): void {
    this.clientsHandler.loginToAccount(account)
  }
}
