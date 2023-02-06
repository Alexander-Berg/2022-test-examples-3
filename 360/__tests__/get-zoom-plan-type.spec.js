const getZoomLink = require('../get-zoom-plan-type');

describe('models:zoom -> get-zoom-plan-type', () => {
  let coreMock;
  let serviceFn;
  let zoomFn;
  const uid = 1234567890;
  const login = 'tet4enko';
  const connectionid = 'MAYA-1234567890';

  beforeEach(() => {
    serviceFn = jest.fn();
    zoomFn = jest.fn();
    coreMock = {
      service: serviceFn,
      auth: {
        get: () => ({uid, login})
      },
      config: {
        connectionid
      }
    };

    serviceFn.mockReturnValue(zoomFn);
  });

  test('должен вызывать сервис zoom', () => {
    zoomFn.mockResolvedValue({});

    getZoomLink({}, coreMock);

    expect(serviceFn).toHaveBeenCalledTimes(1);
    expect(serviceFn).toHaveBeenCalledWith('zoom');
  });

  test('должен ходить в ручку зума с нужными переданным логином, если они есть', () => {
    const params = {login: 'tavria'};

    zoomFn.mockResolvedValue('');

    getZoomLink(params, coreMock);

    expect(zoomFn).toHaveBeenCalledTimes(1);
    expect(zoomFn).toHaveBeenCalledWith(`/api/v1/zoom/users/${params.login}@yandex-team.ru`, null, {
      method: 'get'
    });
  });

  test('должен ходить в ручку зума с текущим юзером, если логин не передан', () => {
    const params = {};

    zoomFn.mockResolvedValue('');

    getZoomLink(params, coreMock);

    expect(zoomFn).toHaveBeenCalledTimes(1);
    expect(zoomFn).toHaveBeenCalledWith(`/api/v1/zoom/users/${login}@yandex-team.ru`, null, {
      method: 'get'
    });
  });

  test('должен возвращать только тип лицензии', async () => {
    const params = {};
    const type = 2;
    const zoomResponse = {type};
    const expectedClientResponse = type;

    zoomFn.mockResolvedValue(zoomResponse);

    expect(await getZoomLink(params, coreMock)).toEqual(expectedClientResponse);
  });
});
