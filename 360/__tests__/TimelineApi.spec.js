import TimelineApi from '../TimelineApi';

describe('TimelineApi', () => {
  describe('getEventsBrief', () => {
    test('должен отправлять запрос на получение краткой информации о событиях', () => {
      const api = {
        post: jest.fn()
      };
      const timelineApi = new TimelineApi(api);
      const params = {
        eventIds: [1, 2],
        forResource: true
      };

      timelineApi.getEventsBrief(params);

      expect(api.post).toBeCalledWith('/get-events-brief', params);
    });
  });

  describe('getAvailabilityIntervals', () => {
    test('должен отправлять запрос на получение интервалов занятости', () => {
      const api = {
        post: jest.fn()
      };
      const timelineApi = new TimelineApi(api);
      const params = {
        date: '2018-01-01',
        emails: ['test@ya.ru'],
        shape: 'ids-only',
        exceptEventId: 1
      };

      timelineApi.getAvailabilityIntervals(params);

      expect(api.post).toBeCalledWith('/get-availability-intervals', params);
    });
  });
});
