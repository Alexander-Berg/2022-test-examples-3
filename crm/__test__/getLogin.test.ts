import { getLogin } from '../getLogin';

describe('getLogin', () => {
  test('user with field login', () => {
    expect(
      getLogin({ user: { id: 1, login: 'user.login', name: 'name' }, login: 'login' }),
    ).toEqual('user.login');
  });

  test('without user', () => {
    expect(getLogin({ login: 'login' })).toEqual('login');
  });

  test('without user and without login', () => {
    expect(getLogin({})).toEqual('');
  });
});
