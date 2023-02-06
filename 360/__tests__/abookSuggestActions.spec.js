import {ActionTypes} from '../abookSuggestConstants';
import {loadSuggestions, makeGetFavoriteContacts} from '../abookSuggestActions';

describe('abookSuggestActions', () => {
  describe('loadSuggestions', () => {
    test('должен вернуть экшен LOAD_SUGGESTIONS', () => {
      const payload = {
        q: 'test',
        start: 'start',
        end: 'end',
        isAllDay: false
      };
      const resolve = () => {};

      expect(loadSuggestions(payload, resolve)).toEqual({
        type: ActionTypes.LOAD_SUGGESTIONS,
        payload: {
          ...payload
        },
        resolve
      });
    });
  });

  describe('getFavoriteContacts', () => {
    test('должен вернуть экшен GET_FAVORITE_CONTACTS', () => {
      const form = 'form-id';
      expect(makeGetFavoriteContacts({form})()).toEqual({
        type: ActionTypes.GET_FAVORITE_CONTACTS,
        shouldCheckAvailability: false,
        meta: {form}
      });
    });
  });
});
