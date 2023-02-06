const model = require('../get-offices');

describe('get-offices', () => {
  test('должен использовать сервис calendar', () => {
    const tz = 'TIMEZONE';
    const lang = 'LANGUAGE';
    const methodHandler = jest.fn();
    const service = jest.fn();
    service.mockImplementation(() => methodHandler);

    const core = {
      service,
      params: {
        _timezone: tz,
        _locale: lang
      }
    };
    model({}, core);

    expect(service).toHaveBeenCalledTimes(1);
    expect(service).toHaveBeenCalledWith('calendar');
  });
  test('должен дёргать ручку get-offices', () => {
    const tz = 'TIMEZONE';
    const lang = 'LANGUAGE';
    const methodHandler = jest.fn();
    const service = jest.fn();
    service.mockImplementation(() => methodHandler);

    const core = {
      service,
      params: {
        _timezone: tz,
        _locale: lang
      }
    };

    model({}, core);

    expect(methodHandler).toHaveBeenCalledTimes(1);
    expect(methodHandler).toHaveBeenCalledWith('/get-offices', {
      tz,
      lang,
      includeOthers: false
    });
  });
  test('должен возвращать результат ручки get-offices', () => {
    const tz = 'TIMEZONE';
    const lang = 'LANGUAGE';
    const methodHandler = jest.fn();
    const service = jest.fn();
    service.mockImplementation(() => methodHandler);
    methodHandler.mockReturnValue('/get-offices');

    const core = {
      service,
      params: {
        _timezone: tz,
        _locale: lang
      }
    };

    expect(model({}, core)).toEqual('/get-offices');
  });
});
