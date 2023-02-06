import { expect } from 'chai';
import { getUserFromEnvOrYav } from '../utils/user';

const PASSPORT_URL = 'https://passport.yandex-team.ru'

describe('Авторизация', function () {
  it('Неавторизированного пользователя должно направлять на авторизацию', function () {
    return this.browser
      .url('/')
      .getUrl()
      .then( (url) => {
        expect(url).to.satisfy((url: string) => url.startsWith(PASSPORT_URL));
      });
  });

  it('Авторизованного пользователя должно пропускать на портал', async function () {
    const user = await getUserFromEnvOrYav();

    await this.browser
      .yaLogin(user.login, user.password)
      .url('/')
      .getUrl()
      .then((url: string) => {
        expect(url).to.satisfy((url: string) => url.startsWith(this.browser.options.baseUrl as string));
      });

    await this.browser.yaLogout();
  });
});
