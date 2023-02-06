const getZoomLink = require('../get-zoom-link');

describe('models:zoom -> get-zoom-link', () => {
  let coreMock;
  let serviceFn;
  let zoomFn;
  const uid = 1234567890;
  const connectionid = 'MAYA-1234567890';

  beforeEach(() => {
    serviceFn = jest.fn();
    zoomFn = jest.fn();
    coreMock = {
      service: serviceFn,
      auth: {
        get: () => ({uid})
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

  test('должен ходить в ручку зума с нужными параметрами', () => {
    const params = {login: 'tet4enko'};

    zoomFn.mockResolvedValue('');

    getZoomLink(params, coreMock);

    expect(zoomFn).toHaveBeenCalledTimes(1);
    expect(zoomFn).toHaveBeenCalledWith(`/api/v1/zoom/meetings?username=${params.login}`, {
      type: 3,
      topic: 'Yandex meeting',
      settings: {
        join_before_host: true
      }
    });
  });

  test('должен возвращать только join_url', async () => {
    const params = {login: 'tet4enko'};
    const join_url = Symbol();
    const zoomResponse = {join_url};
    const expectedClientResponse = join_url;

    zoomFn.mockResolvedValue(zoomResponse);

    expect(await getZoomLink(params, coreMock)).toEqual(expectedClientResponse);
  });
});
