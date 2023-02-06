import createActionMetaInfo from 'middlewares/offlineMiddleware/utils/createActionMetaInfo';

import {ActionTypes} from '../timezonesConstants';
import {getTimezones, getTimezonesNetwork, getTimezonesSuccess} from '../timezonesActions';

describe('timezonesActions', () => {
  describe('getTimezones', () => {
    test('должен вернуть экшен GET_TIMEZONES', () => {
      expect(getTimezones()).toEqual({
        type: ActionTypes.GET_TIMEZONES,
        meta: createActionMetaInfo({network: getTimezonesNetwork()})
      });
    });
  });

  describe('getTimezonesSuccess', () => {
    test('должен вернуть экшен GET_TIMEZONES_SUCCESS', () => {
      const timezones = [];

      expect(getTimezonesSuccess(timezones)).toEqual({
        type: ActionTypes.GET_TIMEZONES_SUCCESS,
        timezones
      });
    });
  });
});
