jest.mock('../../../filters/calendar/get-resources-schedule');

const filter = require('../../../filters/calendar/get-resources-schedule');
const model = require('../get-resources-schedule');

describe('get-resources-schedule', () => {
  describe('ходит в ручки', () => {
    test('должен ходить за интервалами занятости переговорок', async () => {
      const tz = 'TIMEZONE';
      const lang = 'LANGUAGE';
      const date = 'DATE';
      const params = {date};
      const service = jest.fn();
      const modelHandler = jest.fn();
      service.mockImplementation(() => modelHandler);
      const request = jest.fn();
      request.mockImplementation(() => Promise.resolve());
      const core = {
        service,
        request,
        params: {
          _timezone: tz,
          _locale: lang
        }
      };

      await model(params, core);

      expect(core.service).toHaveBeenCalledTimes(1);
      expect(core.service).toHaveBeenCalledWith('calendar');
      expect(modelHandler).toHaveBeenCalledTimes(1);
      expect(modelHandler).toHaveBeenCalledWith('/get-resources-schedule', {tz, lang, date});
    });
    test('должен ходить за событиями пользователя', async () => {
      const tz = 'TIMEZONE';
      const lang = 'LANGUAGE';
      const date = 'DATE';
      const params = {date};
      const service = jest.fn();
      const modelHandler = jest.fn();
      service.mockImplementation(() => modelHandler);
      const request = jest.fn();
      request.mockImplementation(() => Promise.resolve());
      const core = {
        service,
        request,
        params: {
          _timezone: tz,
          _locale: lang
        }
      };

      await model(params, core);

      expect(core.request).toHaveBeenCalledTimes(1);
      expect(core.request).toHaveBeenCalledWith('get-events', {from: date, to: date});
    });
  });

  describe('обрабатывает полученные данные', () => {
    beforeEach(() => {
      filter.mockReset();
      filter.mockImplementation((...args) => args);
    });

    test('должен вызывать filter с интервалами и событиями', async () => {
      const tz = 'TIMEZONE';
      const lang = 'LANGUAGE';
      const date = 'DATE';
      const events = 'EVENTS';
      const intervals = 'INTERVALS';
      const params = {date};
      const service = jest.fn();
      const modelHandler = jest.fn();
      modelHandler.mockImplementation(() => intervals);
      service.mockImplementation(() => modelHandler);
      const request = jest.fn();
      request.mockImplementation(() => Promise.resolve(events));
      const core = {
        service,
        request,
        params: {
          _timezone: tz,
          _locale: lang
        }
      };

      await model(params, core);

      expect(filter).toHaveBeenCalledTimes(1);
      expect(filter).toHaveBeenCalledWith(intervals, events);
    });
    test('должен возвращать результат работы filter и события', async () => {
      const tz = 'TIMEZONE';
      const lang = 'LANGUAGE';
      const date = 'DATE';
      const events = {events: 'EVENTS'};
      const intervals = 'INTERVALS';
      const params = {date};
      const service = jest.fn();
      const modelHandler = jest.fn();
      modelHandler.mockImplementation(() => intervals);
      service.mockImplementation(() => modelHandler);
      const request = jest.fn();
      request.mockImplementation(() => Promise.resolve(events));
      const core = {
        service,
        request,
        params: {
          _timezone: tz,
          _locale: lang
        }
      };

      const response = await model(params, core);

      expect(response).toEqual({
        intervals: [intervals, events],
        events: events
      });
    });
    test('не должен ломаться на походе за событиями пользователя', async () => {
      const tz = 'TIMEZONE';
      const lang = 'LANGUAGE';
      const date = 'DATE';
      const params = {date};
      const service = jest.fn();
      const modelHandler = jest.fn();
      service.mockImplementation(() => modelHandler);
      const request = jest.fn();
      request.mockImplementation(() => Promise.reject());
      const core = {
        service,
        request,
        params: {
          _timezone: tz,
          _locale: lang
        }
      };

      await model(params, core);

      expect(filter).toHaveBeenCalledTimes(1);
    });
  });
});
