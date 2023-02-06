import InviteApi from '../InviteApi';

describe('InviteApi', () => {
  describe('getResourcesSchedule', () => {
    test('должен отправлять запрос за моделью', () => {
      const api = {
        post: jest.fn()
      };
      const inviteApi = new InviteApi(api);
      const date = new Date(2020, 0, 1);

      inviteApi.getResourcesSchedule({
        date,
        bookableOnly: false,
        filter: '123',
        officeId: 1
      });

      expect(api.post).toHaveBeenCalledTimes(1);
      expect(api.post).toBeCalledWith('/get-resources-schedule', {
        date: '2020-01-01',
        bookableOnly: false,
        filter: '123',
        officeId: 1
      });
    });
  });
});
