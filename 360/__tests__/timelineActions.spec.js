import {ActionTypes} from '../timelineConstants';
import {getEventsInfo, getAvailabilityIntervals} from '../timelineActions';

describe('timelineActions', () => {
  describe('getEventsInfo', () => {
    test('должен вернуть экшен GET_EVENTS_INFO', () => {
      const payload = {
        eventIds: [100500],
        forResource: false
      };
      const resolve = () => {};

      expect(getEventsInfo(payload, resolve)).toEqual({
        type: ActionTypes.GET_EVENTS_INFO,
        payload,
        resolve
      });
    });
  });

  describe('getAvailabilityIntervals', () => {
    test('должен вернуть экшен GET_AVAILABILITY_INTERVALS', () => {
      const payload = {
        emails: [],
        shape: 'ids-only'
      };
      const resolve = () => {};

      expect(getAvailabilityIntervals(payload, resolve)).toEqual({
        type: ActionTypes.GET_AVAILABILITY_INTERVALS,
        payload,
        resolve
      });
    });
  });
});
