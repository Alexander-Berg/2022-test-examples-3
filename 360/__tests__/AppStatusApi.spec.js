import AppStatusApi from '../AppStatusApi';

describe('AppStatusApi', () => {
  describe('getStatus', () => {
    test('должен запрашивать статус приложения', () => {
      const api = {
        post: jest.fn()
      };

      const appStatusApi = new AppStatusApi(api);

      appStatusApi.getStatus();

      expect(api.post).toBeCalledWith('/get-status');
    });
  });
});
