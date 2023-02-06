import { getSign } from '../getSign';

describe('getSign', () => {
  test('user with first_name and last_name', () => {
    expect(
      getSign({
        user: { id: 1, login: 'user.login', name: 'name', first_name: 'Ivan', last_name: 'Petrov' },
        login: 'login',
      }),
    ).toEqual('PI');
  });

  test('user with first_name', () => {
    expect(
      getSign({
        user: { id: 1, login: 'user.login', name: '', first_name: 'Ivan' },
        login: 'login',
      }),
    ).toEqual('I');
  });

  test('user with last_name', () => {
    expect(
      getSign({
        user: { id: 1, login: 'user.login', name: '', last_name: 'Petrov' },
        login: 'login',
      }),
    ).toEqual('P');
  });

  test('user with name', () => {
    expect(
      getSign({
        user: { id: 1, login: 'user.login', name: 'User Name' },
        login: 'login',
      }),
    ).toEqual('UN');
  });

  test('user with login', () => {
    expect(
      getSign({
        user: { id: 1, login: 'user.login', name: '' },
        login: 'login',
      }),
    ).toEqual('u');
  });
});
