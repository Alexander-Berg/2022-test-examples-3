import Availability from 'constants/Availability';
import {checkFavoriteContactsAvailabilitySuccess} from 'features/eventForm/eventFormActions';

import {ActionTypes} from '../abookSuggestConstants';
import abookSuggestReducer, {initialState} from '../abookSuggestReducer';

describe('abookSuggestReducer', () => {
  test('должен обрабатывать начальное состояние', () => {
    expect(abookSuggestReducer(undefined, {})).toBe(initialState);
  });

  describe('GET_FAVORITE_CONTACTS_SUCCESS', () => {
    test('должен сохранять список избранных участников', () => {
      const action = {
        type: ActionTypes.GET_FAVORITE_CONTACTS_SUCCESS,
        payload: [
          {
            email: 'test@ya.ru',
            login: 'test'
          }
        ]
      };

      const expectedState = {
        favoriteContacts: [
          {
            email: 'test@ya.ru',
            login: 'test'
          }
        ]
      };

      expect(abookSuggestReducer(initialState, action)).toEqual(expectedState);
    });
  });

  describe('checkFavoriteContactsAvailabilitySuccess', () => {
    test('должен обновлять занятость избранных участников', () => {
      const action = {
        type: checkFavoriteContactsAvailabilitySuccess.type,
        payload: {
          availabilities: [
            {
              email: 'test@ya.ru',
              availability: Availability.BUSY
            }
          ]
        }
      };

      const state = {
        favoriteContacts: [
          {
            email: 'test@ya.ru'
          }
        ]
      };

      const expectedState = {
        favoriteContacts: [
          {
            email: 'test@ya.ru',
            availability: Availability.BUSY
          }
        ]
      };

      expect(abookSuggestReducer(state, action)).toEqual(expectedState);
    });
  });
});
