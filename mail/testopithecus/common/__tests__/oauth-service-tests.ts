import * as assert from 'assert';
import { OauthService } from '../code/users/oauth-service';
import { UserAccount } from '../code/users/user-pool';
import { DefaultJSONSerializer } from './pod/default-json';
import { createSyncNetwork } from './test-utils';

describe('default oauth service', () => {
  it('should get oauth token', (done) => {
    const oauthService = new OauthService(createSyncNetwork(), new DefaultJSONSerializer())
    const token = oauthService.getToken(new UserAccount('yandex-team-77175-41375', 'simple123456'))
    console.log(token)
    assert.ok(token.length > 0)
    done()
  });
});
