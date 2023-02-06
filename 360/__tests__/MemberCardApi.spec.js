import moment from 'moment';

import MemberCardApi from '../MemberCardApi';

describe('MemberCardApi', () => {
  describe('getStaffCard', () => {
    test('должен отправлять запрос на получение карточки со стаффа', () => {
      const api = {
        post: jest.fn()
      };
      const memberCardApi = new MemberCardApi(api);

      memberCardApi.getStaffCard('fresk');
      const ts = moment()
        .startOf('day')
        .valueOf();

      expect(api.post).toBeCalledWith('/get-staff-card', {login: 'fresk', ts}, {cache: true});
    });
  });
});
