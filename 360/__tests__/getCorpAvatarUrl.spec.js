import config from 'configs/config';

import getCorpAvatarUrl from '../getCorpAvatarUrl';

describe('getCorpAvatarUrl', () => {
  beforeEach(() => {
    sinon.stub(config.urls, 'corpAvatars').value('{{login}}');
  });

  test('должен возвращать урл корповой аватарки по логину, если он есть', () => {
    const login = 'olologin';
    const userData = {login};

    expect(getCorpAvatarUrl(userData)).toEqual(login);
  });

  test('должен возвращать урл корповой аватарки по имейлу, если нет логина', () => {
    const login = 'olologin';
    const email = `${login}@yan.ru`;
    const userData = {email};

    expect(getCorpAvatarUrl(userData)).toEqual(login);
  });
});
