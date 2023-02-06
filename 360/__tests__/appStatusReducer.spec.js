import appStatusReducer, {INITIAL_STATE} from '../appStatusReducer';
import {ActionTypes} from '../appStatusConstants';

describe('appStatusReducer', () => {
  test('должен обрабатывать начальное состояние', () => {
    expect(appStatusReducer(undefined, {})).toBe(INITIAL_STATE);
  });

  describe('GET_STATUS_SUCCESS', () => {
    test('должен сохранять полученное состояние', () => {
      const action = {
        type: ActionTypes.GET_STATUS_SUCCESS,
        readOnly: true,
        notifications: [{name: 'ololo', uid: 10}]
      };

      const expectedState = {
        readOnly: true,
        offline: false,
        notifications: [10]
      };

      expect(appStatusReducer(INITIAL_STATE, action)).toEqual(expectedState);
    });
  });
});
