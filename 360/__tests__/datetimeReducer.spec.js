import {Map} from 'immutable';

import datetime, {initialState} from '../datetimeReducer';
import {ActionTypes} from '../datetimeConstants';

describe('datetimeReducer', () => {
  test('должен обрабатывать начальное состояние', () => {
    expect(datetime(undefined, {})).toEqual(initialState);
  });

  describe('UPDATE', () => {
    test('должен обновлять текущее время', () => {
      const action = {
        type: ActionTypes.UPDATE,
        dateTs: new Date(2020, 10, 20).getTime(),
        timeTs: new Date(2020, 10, 20, 12, 15).getTime()
      };

      const state = initialState;
      const expectedState = new Map({
        date: action.dateTs,
        time: action.timeTs
      });

      expect(datetime(state, action)).toEqual(expectedState);
    });
  });
});
