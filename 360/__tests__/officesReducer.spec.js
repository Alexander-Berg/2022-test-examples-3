import {OrderedMap} from 'immutable';

import officesReducer, {initialState} from '../officesReducer';
import OfficeRecord from '../OfficeRecord';
import {ActionTypes} from '../officesConstants';

describe('officesReducer', () => {
  test('должен обрабатывать начальное состояние', () => {
    expect(officesReducer(undefined, {})).toEqual(initialState);
  });

  describe('GET_OFFICES_SUCCESS', () => {
    test('должен добавлять новые офисы в список', () => {
      const office1 = {
        id: 1,
        name: 'Office 1',
        cityName: 'Moscow'
      };
      const office2 = {
        id: 2,
        name: 'Office 2',
        cityName: 'Moscow'
      };

      const state = initialState;
      const expectedState = new OrderedMap([
        [office1.id, new OfficeRecord(office1)],
        [office2.id, new OfficeRecord(office2)]
      ]);
      const action = {
        type: ActionTypes.GET_OFFICES_SUCCESS,
        offices: [office1, office2]
      };

      expect(officesReducer(state, action)).toEqual(expectedState);
    });
  });

  describe('GET_OFFICES_TZ_OFFSETS_SUCCESS', () => {
    test('должен обновлять смещение часового пояса у каждого офиса', () => {
      const state = new OrderedMap([
        [1, new OfficeRecord({id: 1, tzOffset: 0})],
        [2, new OfficeRecord({id: 2, tzOffset: -3600000})]
      ]);
      const expectedState = new OrderedMap([
        [1, new OfficeRecord({id: 1, tzOffset: 0})],
        [2, new OfficeRecord({id: 2, tzOffset: 0})]
      ]);
      const action = {
        type: ActionTypes.GET_OFFICES_TZ_OFFSETS_SUCCESS,
        tzOffsets: {1: 0, 2: 0}
      };

      expect(officesReducer(state, action)).toEqual(expectedState);
    });
  });
});
