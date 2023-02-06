import { MailboxClient } from '../code/client/mailbox-client';
import { MailboxPreparer } from '../code/mail/mailbox-preparer';
import { OauthService } from '../code/users/oauth-service';
import { OAuthUserAccount, UserAccount } from '../code/users/user-pool';
import { MockPlatform } from '../code/utils/platform';
import { Nullable } from '../ys/ys';
import { ConsoleLog } from './pod/console-log';
import { DefaultSyncNetwork } from './pod/default-http';
import { DefaultJSONSerializer } from './pod/default-json';
import { SyncSleepImpl } from './pod/sleep'
import { PRIVATE_BACKEND_CONFIG } from './private-backend-config';

export function createSyncNetwork(): DefaultSyncNetwork {
  const jsonSerializer = new DefaultJSONSerializer();
  return new DefaultSyncNetwork(jsonSerializer, ConsoleLog.LOGGER);
}

export function createNetworkClient(oauthAccount: Nullable<OAuthUserAccount> = null): MailboxClient {
  let account: UserAccount;
  let token: string;
  if (oauthAccount === null) {
    account = PRIVATE_BACKEND_CONFIG.account
    token = createOauthService().getToken(PRIVATE_BACKEND_CONFIG.account)
  } else {
    account = oauthAccount!.account;
    token = oauthAccount!.oauthToken;
  }
  const jsonSerializer = new DefaultJSONSerializer();
  return new MailboxClient(
    MockPlatform.androidDefault,
    account,
    token,
    createSyncNetwork(),
    jsonSerializer,
    ConsoleLog.LOGGER,
  );
}

export function createMailboxPreparer(): MailboxPreparer {
  return new MailboxPreparer(createSyncNetwork(), new DefaultJSONSerializer(), SyncSleepImpl.instance, ConsoleLog.LOGGER);
}

export function createOauthService(): OauthService {
  return new OauthService(createSyncNetwork(), new DefaultJSONSerializer())
}
