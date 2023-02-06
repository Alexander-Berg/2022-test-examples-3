const connectUserInfo = require('../connect-userinfo');

describe('models:directory -> connect-userinfo', () => {
  let coreMock;
  let serviceFn;
  let directoryFn;
  const uid = 1234567890;
  const connectionid = 'MAYA-1234567890';

  beforeEach(() => {
    serviceFn = jest.fn();
    directoryFn = jest.fn();
    coreMock = {
      service: serviceFn,
      auth: {
        get: () => ({uid})
      },
      config: {
        connectionid
      }
    };

    serviceFn.mockReturnValue(directoryFn);
  });

  test('должен вызывать сервис directory', () => {
    directoryFn.mockResolvedValue({});

    connectUserInfo({}, coreMock);

    expect(serviceFn).toHaveBeenCalledTimes(1);
    expect(serviceFn).toHaveBeenCalledWith('directory');
  });

  test('должен ходить в ручку directory с нужными параметрами', () => {
    const params = {a: '1'};

    directoryFn.mockResolvedValue({});

    connectUserInfo(params, coreMock);

    expect(directoryFn).toHaveBeenCalledTimes(1);
    expect(directoryFn).toHaveBeenCalledWith(`/users/${uid}`, params);
  });
});
