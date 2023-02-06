import {ActionTypes} from '../suggestMeetingTimesConstants';
import {getMeetingTimes, checkResourcesAvailability} from '../suggestMeetingTimesActions';

describe('suggestMeetingTimesActions', () => {
  describe('getMeetingTimes', () => {
    test('должен вернуть экшен GET_MEETING_TIMES', () => {
      const payload = {
        start: 'start',
        end: 'end'
      };
      const resolve = () => {};

      expect(getMeetingTimes(payload, resolve)).toEqual({
        type: ActionTypes.GET_MEETING_TIMES,
        payload,
        resolve
      });
    });
  });

  describe('checkResourcesAvailability', () => {
    test('должен вернуть экшен CHECK_RESOURCES_AVAILABILITY', () => {
      const payload = {
        start: 'start',
        end: 'end',
        emails: ['zimniy_conf@yandex-team.ru']
      };
      const resolve = () => {};

      expect(checkResourcesAvailability(payload, resolve)).toEqual({
        type: ActionTypes.CHECK_RESOURCES_AVAILABILITY,
        payload,
        resolve
      });
    });
  });
});
