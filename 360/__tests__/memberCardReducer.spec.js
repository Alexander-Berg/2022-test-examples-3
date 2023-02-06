import memberCardReducer from '../memberCardReducer';
import {ActionTypes} from '../memberCardConstants';

describe('memberCardReducer', () => {
  describe('GET_STAFF_CARD_START', () => {
    test('должен записывать в стейт объект с нужным логином', () => {
      const state = {};
      const action = {
        type: ActionTypes.GET_STAFF_CARD_START,
        login: 'pistch'
      };
      const expectedState = {
        pistch: {isLoading: true}
      };

      expect(memberCardReducer(state, action)).toEqual(expectedState);
    });
    test('должен проставлять флаг загрузки для нужного логина', () => {
      const state = {
        pistch: {isLoading: false, member: {}}
      };
      const action = {
        type: ActionTypes.GET_STAFF_CARD_START,
        login: 'pistch'
      };
      const expectedState = {
        pistch: {isLoading: true, member: {}}
      };

      expect(memberCardReducer(state, action)).toEqual(expectedState);
    });
  });
  describe('GET_STAFF_CARD_DONE', () => {
    test('должен записывать в стейт объект с нужным логином', () => {
      const state = {
        pistch: {isLoading: false, member: null}
      };
      const member = {login: 'pistch'};
      const action = {
        type: ActionTypes.GET_STAFF_CARD_DONE,
        login: 'pistch',
        member
      };
      const expectedState = {
        pistch: {isLoading: false, member}
      };

      expect(memberCardReducer(state, action)).toEqual(expectedState);
    });
    test('должен проставлять флаг загрузки для нужного логина', () => {
      const member = {};
      const state = {
        pistch: {isLoading: true, member}
      };
      const action = {
        type: ActionTypes.GET_STAFF_CARD_DONE,
        login: 'pistch',
        member
      };
      const expectedState = {
        pistch: {isLoading: false, member}
      };

      expect(memberCardReducer(state, action)).toEqual(expectedState);
    });
  });
});
