import AvatarsApi from '../AvatarsApi';

describe('AvatarsApi', () => {
  describe('getSocialAvatar', () => {
    test('должен отправлять запрос на получение социальной аватарки', () => {
      const api = {
        post: jest.fn()
      };
      const avatarsApi = new AvatarsApi(api);

      avatarsApi.getSocialAvatar('test@ya.ru');

      expect(api.post).toBeCalledWith(
        '/social-avatars',
        {
          email: 'test@ya.ru'
        },
        {
          cache: true
        }
      );
    });
  });
});
