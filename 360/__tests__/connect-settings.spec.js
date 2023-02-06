const connectSettings = require('../connect-settings');

describe('models:directory -> connect-settings', () => {
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

    connectSettings({}, coreMock);

    expect(serviceFn).toHaveBeenCalledTimes(1);
    expect(serviceFn).toHaveBeenCalledWith('directory');
  });
});
