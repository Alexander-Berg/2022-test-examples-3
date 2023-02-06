import {Map} from 'immutable';

import createTimezonesReducer, {createInitialState} from '../timezonesReducer';
import TimezoneRecord from '../TimezoneRecord';
import {ActionTypes} from '../timezonesConstants';

describe('timezonesReducer', () => {
  test('должен обрабатывать начальное состояние', () => {
    expect(createTimezonesReducer()(undefined, {})).toEqual(createInitialState());
  });

  describe('GET_TIMEZONES_SUCCESS', () => {
    test('должен обновлять список таймзон', () => {
      const state = new Map({
        'Europe/Moscow': new TimezoneRecord({
          id: 'Europe/Moscow',
          offset: -180
        })
      });
      const expectedState = new Map({
        'Europe/Moscow': new TimezoneRecord({
          id: 'Europe/Moscow',
          offset: -180
        }),
        'Europe/Minsk': new TimezoneRecord({
          id: 'Europe/Minsk',
          offset: -120
        })
      });
      const action = {
        type: ActionTypes.GET_TIMEZONES_SUCCESS,
        timezones: [
          {
            id: 'Europe/Minsk',
            offset: -120
          }
        ]
      };
      expect(createTimezonesReducer()(state, action)).toEqual(expectedState);
    });
  });
});
