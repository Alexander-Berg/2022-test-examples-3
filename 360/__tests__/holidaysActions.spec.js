import moment from 'moment';

import createActionMetaInfo from 'middlewares/offlineMiddleware/utils/createActionMetaInfo';

import {ActionTypes} from '../holidaysConstants';
import {getHolidays, getHolidaysNetwork, getHolidaysSuccess} from '../holidaysActions';

describe('holidaysActions', () => {
  describe('getHolidays', () => {
    test('должен вернуть экшен GET_HOLIDAYS', () => {
      const payload = {
        from: moment().format(moment.HTML5_FMT.DATE),
        to: moment().format(moment.HTML5_FMT.DATE)
      };

      expect(getHolidays(payload)).toEqual({
        type: ActionTypes.GET_HOLIDAYS,
        payload,
        meta: createActionMetaInfo({network: getHolidaysNetwork(payload)})
      });
    });
  });

  describe('getHolidaysSuccess', () => {
    test('должен вернуть экшен GET_HOLIDAYS_SUCCESS', () => {
      const holidays = [];

      expect(getHolidaysSuccess(holidays)).toEqual({
        type: ActionTypes.GET_HOLIDAYS_SUCCESS,
        holidays
      });
    });
  });
});
