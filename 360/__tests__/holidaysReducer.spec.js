import moment from 'moment';
import {Map} from 'immutable';

import holidaysReducer, {initialState} from '../holidaysReducer';
import {ActionTypes} from '../holidaysConstants';
import HolidayRecord from '../HolidayRecord';

describe('holidaysReducer', () => {
  test('должен обрабатывать начальное состояние', () => {
    expect(holidaysReducer(undefined, {})).toEqual(initialState);
  });

  describe('GET_HOLIDAYS_SUCCESS', () => {
    test('должен обновлять список праздников', () => {
      const state = new Map({
        [Number(moment('2018-05-01'))]: new HolidayRecord({
          type: 'holiday',
          date: '2018-05-01'
        })
      });
      const expectedState = new Map({
        [Number(moment('2018-05-01'))]: new HolidayRecord({
          type: 'holiday',
          date: '2018-05-01'
        }),
        [Number(moment('2018-05-09'))]: new HolidayRecord({
          type: 'holiday',
          date: '2018-05-09'
        })
      });
      const action = {
        type: ActionTypes.GET_HOLIDAYS_SUCCESS,
        holidays: [
          {
            type: 'holiday',
            date: '2018-05-09'
          }
        ]
      };

      expect(holidaysReducer(state, action)).toEqual(expectedState);
    });
  });
});
