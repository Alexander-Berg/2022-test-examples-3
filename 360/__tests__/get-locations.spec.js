const getRegionByIp = jest.fn();

jest.mock('@yandex-int/yandex-geobase', () => ({
  default: {
    v6: () => ({
      getRegionByIp
    })
  }
}));

const getLocations = require('../get-locations');

describe('models:maps -> get-locations', () => {
  let coreMock;
  let serviceFn;
  let mapsFn;
  const uid = 1234567890;
  const connectionid = 'MAYA-1234567890';
  const USER_IP = '127.0.0.1';
  const yandexuid = 123;

  beforeEach(() => {
    serviceFn = jest.fn();
    mapsFn = jest.fn();
    coreMock = {
      service: serviceFn,
      auth: {
        get: () => ({uid})
      },
      config: {
        connectionid,
        USER_IP
      },
      req: {cookies: {yandexuid}}
    };

    serviceFn.mockReturnValue(mapsFn);

    getRegionByIp.mockReset();
  });

  test('должен вызывать сервис maps', async () => {
    mapsFn.mockResolvedValue({results: []});

    await getLocations({}, coreMock);

    expect(serviceFn).toHaveBeenCalledTimes(1);
    expect(serviceFn).toHaveBeenCalledWith('maps');
  });

  test('должен ходить в ручку maps с нужными параметрами', async () => {
    const params = {a: 'b'};

    mapsFn.mockResolvedValue({results: []});

    coreMock.config.USER_IP = Symbol();

    const region = {
      longitude: Math.random(),
      latitude: Math.random(),
      longitude_size: Math.random(),
      latitude_size: Math.random()
    };

    getRegionByIp.mockReturnValueOnce(region);

    await getLocations(params, coreMock);

    expect(getRegionByIp).toHaveBeenCalledWith(coreMock.config.USER_IP);

    expect(mapsFn).toHaveBeenCalledTimes(1);
    expect(mapsFn).toHaveBeenCalledWith(
      '/suggest-geo',
      Object.assign({}, params, {
        callback: '',
        search_type: 'calendar',
        fullpath: 0,
        v: 9,
        results: 5,
        yu: yandexuid,
        ll: `${region.longitude},${region.latitude}`,
        spn: `${region.longitude_size},${region.latitude_size}`
      })
    );
  });
});
