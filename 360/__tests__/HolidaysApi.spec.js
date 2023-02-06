import HolidaysApi from '../HolidaysApi';

describe('HolidaysApi', () => {
  describe('getHolidays', () => {
    test('должен отправлять запрос на получение праздников', () => {
      const api = {
        post: jest.fn()
      };
      const holidaysApi = new HolidaysApi(api);

      holidaysApi.getHolidays({
        from: '2018-01-01',
        to: '2019-01-01',
        outMode: 'overrides'
      });

      expect(api.post).toBeCalledWith(
        '/get-holidays',
        {
          from: '2018-01-01',
          to: '2019-01-01',
          outMode: 'overrides'
        },
        {
          cache: true
        }
      );
    });
  });
});
