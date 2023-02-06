import RoomCardApi from '../RoomCardApi';

describe('RoomCardApi', () => {
  describe('getInfo', () => {
    test('должен отправлять запрос на получение информации для карточки', () => {
      const api = {
        post: jest.fn()
      };
      const roomCardApi = new RoomCardApi(api);

      roomCardApi.getInfo({email: 'test@ya.ru', login: 'test'});

      expect(api.post).toBeCalledWith(
        '/get-user-or-resource-info',
        {
          email: 'test@ya.ru',
          login: 'test'
        },
        {
          cache: true
        }
      );
    });
  });
});
