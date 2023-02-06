import TimezonesApi from '../TimezonesApi';

describe('TimezonesApi', () => {
  describe('getTimezones', () => {
    test('должен отправлять запрос на получение часовых поясов', () => {
      const api = {
        post: jest.fn()
      };
      const timezonesApi = new TimezonesApi(api);

      timezonesApi.getTimezones();

      expect(api.post).toBeCalledWith('/get-timezones', {}, {cache: true});
    });
  });
});
