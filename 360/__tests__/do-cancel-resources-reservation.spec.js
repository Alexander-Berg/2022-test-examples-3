const model = require('../do-cancel-resources-reservation');

describe('delete-event', () => {
  const tz = 'TIMEZONE';
  const lang = 'LANGUAGE';
  const uid = 1234567890;
  const login = 'tavria';
  const IS_CORP = true;
  let serviceFn;
  let requestFn;
  let coreMock;
  let methodHandler;

  beforeEach(() => {
    methodHandler = jest.fn();
    serviceFn = jest.fn();
    requestFn = jest.fn();
    serviceFn.mockReturnValue(methodHandler);

    coreMock = {
      service: serviceFn,
      request: requestFn,
      params: {
        _timezone: tz,
        _locale: lang
      },
      auth: {
        get: () => ({uid, login})
      },
      config: {IS_CORP, i18n: {locale: 'ru'}},
      hideParamInLog: jest.fn()
    };
  });

  test('должен использовать сервис calendar', () => {
    model({}, coreMock);

    expect(serviceFn).toHaveBeenCalledTimes(1);
    expect(serviceFn).toHaveBeenCalledWith('calendar');
  });

  test('должен ходить в ручку календаря с нужными параметрами', () => {
    const params = {a: 1};
    model(params, coreMock);

    expect(methodHandler).toHaveBeenCalledTimes(1);
    expect(methodHandler).toHaveBeenCalledWith('/cancel-resources-reservation', params);
  });
});
