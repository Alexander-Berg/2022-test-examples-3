jest.mock('../../../filters/calendar/get-availability-intervals.js');
const filter = require('../../../filters/calendar/get-availability-intervals.js');
const model = require('../get-availability-intervals');

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
    filter.mockClear();
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
    const params = {a: 1};
    model(params, coreMock);

    expect(serviceFn).toHaveBeenCalledTimes(1);
    expect(serviceFn).toHaveBeenCalledWith('calendar');
  });

  test('должен вызывать ручку calendar с нужными параметрами', () => {
    const params = {a: 1};
    model(params, coreMock);

    expect(methodHandler).toHaveBeenCalledTimes(1);
    expect(methodHandler).toHaveBeenCalledWith('/get-availability-intervals', {
      ...params,
      tz
    });
  });

  test('должен возвращать ответ, пропущенный через фильтр', async () => {
    const params = {
      q: '123',
      user_type: 'pdd'
    };
    const response = {subjectAvailabilities: [{a: 1}, {b: 2}]};
    const filterResult = [{a: 1}];

    filter.mockReturnValue(filterResult);
    methodHandler.mockReturnValue(response);

    const modelResult = await model(params, coreMock);

    expect(filter).toHaveBeenCalledTimes(1);
    expect(filter).toHaveBeenCalledWith(response);
    expect(modelResult).toBe(filterResult);
  });
});
