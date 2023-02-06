import AppApi from '../AppApi';

describe('AppApi', () => {
  describe('getAvailabilities', () => {
    test('должен отправлять запрос на подписку на xiva', () => {
      const api = {
        post: jest.fn()
      };
      const appApi = new AppApi(api);

      const data = {a: 1};
      const uid = 123;

      appApi.createSubscription(data, uid);

      expect(api.post).toBeCalledWith('/create-subscription', {
        data,
        uid
      });
    });
  });
});
