import {ActionTypes} from '../resourcesSuggestConstants';
import {loadSuggestions, loadSuggestionsDetails} from '../resourcesSuggestActions';

describe('resorcesSuggestActions', () => {
  describe('loadSuggestions', () => {
    test('должен вернуть экшен LOAD_SUGGESTIONS', () => {
      const payload = {
        query: 'test',
        start: 'start',
        end: 'end',
        isAllDay: false
      };
      const resolve = () => {};

      expect(loadSuggestions(payload, resolve)).toEqual({
        type: ActionTypes.LOAD_SUGGESTIONS,
        payload,
        resolve
      });
    });
  });
  describe('loadSuggestionsDetails', () => {
    test('должен вернуть экшен LOAD_SUGGESTIONS_DETAILS', () => {
      const payload = {
        query: 'test',
        start: 'start',
        end: 'end',
        isAllDay: false
      };
      const resolve = () => {};

      expect(loadSuggestionsDetails(payload, resolve)).toEqual({
        type: ActionTypes.LOAD_SUGGESTIONS_DETAILS,
        payload,
        resolve
      });
    });
  });
});
