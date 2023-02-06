import SettingsApi from '../SettingsApi';

describe('SettingsApi', () => {
  describe('update', () => {
    test('должен отправлять запрос на изменение настроек', () => {
      const api = {
        post: jest.fn()
      };
      const settingsApi = new SettingsApi(api);

      settingsApi.update({
        tz: 'Europe/Minsk'
      });

      expect(api.post).toBeCalledWith('/do-update-user-settings', {
        tz: 'Europe/Minsk'
      });
    });
  });
});
