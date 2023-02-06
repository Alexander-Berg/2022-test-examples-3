import OfficesApi from '../OfficesApi';

describe('OfficesApi', () => {
  describe('getOffices', () => {
    test('должен отправлять запрос на получение офисов', () => {
      const api = {
        post: jest.fn()
      };
      const officeApi = new OfficesApi(api);

      officeApi.getOffices();

      expect(api.post).toBeCalledWith('/get-offices', {includeOthers: true}, {cache: true});
    });
  });

  describe('getOfficesTzOffsets', () => {
    test('должен отправлять запрос на получение смещения часового пояса для офисов', () => {
      const api = {
        post: jest.fn()
      };
      const officeApi = new OfficesApi(api);

      officeApi.getOfficesTzOffsets('2018-06-06T21:00:00');

      expect(api.post).toBeCalledWith(
        '/get-offices-tz-offsets',
        {
          ts: '2018-06-06T21:00:00'
        },
        {
          cache: true
        }
      );
    });
  });
});
