import {ActionTypes} from '../datetimeConstants';
import {update, forceDatetimeUpdate} from '../datetimeActions';

describe('datetimeActions', () => {
  describe('update', () => {
    test('должен вернуть экшен UPDATE', () => {
      const params = {
        dateTs: new Date(2020, 10, 20).getTime(),
        timeTs: new Date(2020, 10, 20, 12, 15).getTime()
      };

      expect(update(params)).toEqual({
        type: ActionTypes.UPDATE,
        dateTs: params.dateTs,
        timeTs: params.timeTs
      });
    });
  });
  describe('forceDatetimeUpdate', () => {
    test('должен вернуть экшен FORCE_UPDATE', () => {
      expect(forceDatetimeUpdate()).toEqual({type: ActionTypes.FORCE_UPDATE});
    });
  });
});
