import {ActionTypes} from '../locationSuggestConstants';
import {loadSuggestions} from '../locationSuggestActions';

describe('locationSuggestActions', () => {
  describe('loadSuggestions', () => {
    test('должен вернуть экшен LOAD_SUGGESTIONS', () => {
      const payload = {
        part: 'St. P'
      };
      const resolve = () => {};

      expect(loadSuggestions(payload, resolve)).toEqual({
        type: ActionTypes.LOAD_SUGGESTIONS,
        payload,
        resolve
      });
    });
  });
});
