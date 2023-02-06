const createConferenceCall = require('../do-create-conference-call');

describe('models:vconf -> do-create-conference-call', () => {
  let coreMock;
  let serviceFn;
  let vconfFn;
  const uid = 1234567890;
  const login = 'tet4enko';
  const connectionid = 'MAYA-1234567890';

  beforeEach(() => {
    serviceFn = jest.fn();
    vconfFn = jest.fn();
    coreMock = {
      service: serviceFn,
      auth: {
        get: () => ({uid, login})
      },
      config: {
        connectionid
      }
    };

    serviceFn.mockReturnValue(vconfFn);
  });

  test('должен вызывать сервис vconf', () => {
    createConferenceCall({}, coreMock);

    expect(serviceFn).toHaveBeenCalledTimes(1);
    expect(serviceFn).toHaveBeenCalledWith('vconf');
  });

  test('должен ходить в ручку vconf с нужными параметрами', () => {
    const params = {a: 'b'};

    createConferenceCall(params, coreMock);

    expect(vconfFn).toHaveBeenCalledTimes(1);
    expect(vconfFn).toHaveBeenCalledWith(
      '/create',
      Object.assign({}, params, {
        author_login: login
      })
    );
  });
});
