import RemindersApi from '../RemindersApi';

describe('RemindersApi', () => {
  describe('update', () => {
    test('должен отправлять запрос на изменение напоминания', () => {
      const api = {
        post: jest.fn()
      };
      const remindersApi = new RemindersApi(api);

      remindersApi.update({
        id: 100,
        name: 'new name'
      });

      expect(api.post).toBeCalledWith('/do-update-reminder', {
        id: 100,
        name: 'new name'
      });
    });
  });

  describe('delete', () => {
    test('должен отправлять запрос на удаление напоминания', () => {
      const api = {
        post: jest.fn()
      };
      const remindersApi = new RemindersApi(api);

      remindersApi.delete(100);

      expect(api.post).toBeCalledWith('/do-delete-reminder', {
        id: 100
      });
    });
  });
});
