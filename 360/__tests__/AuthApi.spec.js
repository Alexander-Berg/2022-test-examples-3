import AuthApi from '../AuthApi';

describe('AuthApi', () => {
  describe('check', () => {
    test('должен отправлять запрос на проверку авторизации', () => {
      const api = {
        post: jest.fn()
      };
      const authApi = new AuthApi(api);

      authApi.check();

      expect(api.post).toBeCalledWith('/get-user-status');
    });
  });
});
