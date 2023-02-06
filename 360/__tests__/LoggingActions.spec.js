import {suggestReport, logDeleteEvent} from '../loggingActions';
import {ActionNames} from '../loggingConstants';

describe('loggingActions', () => {
  describe('suggestReport', () => {
    test('должен вернуть экшен SUGGEST_REPORT', () => {
      const payload = {};
      expect(suggestReport(payload)).toEqual({
        type: 'Logging/SUGGEST_REPORT',
        payload
      });
    });
  });

  describe('logDeleteEvent', () => {
    test('должен вернуть экшен DELETE_EVENT', () => {
      const payload = {};
      expect(logDeleteEvent(payload)).toEqual({
        type: `Logging/${ActionNames.DELETE_EVENT}`,
        payload
      });
    });
  });
});
