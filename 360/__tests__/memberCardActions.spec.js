import {ActionTypes} from '../memberCardConstants';
import {getStaffCard, getStaffCardStart, getStaffCardDone} from '../memberCardActions';

describe('memberCardActions', () => {
  describe('getStaffCard', () => {
    test('должен вернуть экшен GET_STAFF_CARD', () => {
      const login = 'login';

      expect(getStaffCard(login)).toEqual({
        type: ActionTypes.GET_STAFF_CARD,
        login
      });
    });
  });
  describe('getStaffCardStart', () => {
    test('должен вернуть экшен GET_STAFF_CARD_START', () => {
      const login = 'login';

      expect(getStaffCardStart(login)).toEqual({
        type: ActionTypes.GET_STAFF_CARD_START,
        login
      });
    });
  });
  describe('getStaffCardDone', () => {
    test('должен вернуть экшен GET_STAFF_CARD', () => {
      const login = 'login';
      const member = {};

      expect(getStaffCardDone(login, member)).toEqual({
        type: ActionTypes.GET_STAFF_CARD_DONE,
        login,
        member
      });
    });
  });
});
