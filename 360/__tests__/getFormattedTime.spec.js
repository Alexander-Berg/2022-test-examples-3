import moment from 'moment';

import getFormattedTime from '../getFormattedTime';

describe('eventForm/utils/getFormattedTime', () => {
  describe('событие на весь день', () => {
    test('должен возвращать время для события, которое начинается я заканчивается в один день', () => {
      const start = Number(moment('2018-01-01T00:00'));
      const end = Number(moment('2018-01-01T00:00'));
      const isAllDay = true;

      expect(getFormattedTime(start, end, isAllDay)).toMatchSnapshot();
    });

    test('должен возвращать время для события, которое начинается в один день, а заканчивается в другой', () => {
      const start = Number(moment('2018-01-01T00:00'));
      const end = Number(moment('2018-01-03T00:00'));
      const isAllDay = true;

      expect(getFormattedTime(start, end, isAllDay)).toMatchSnapshot();
    });
  });

  describe('событие не на весь день', () => {
    test('должен возвращать время для события, которое начинается и заканчивается в один день', () => {
      const start = Number(moment('2018-01-01T10:00'));
      const end = Number(moment('2018-01-01T11:00'));
      const isAllDay = false;

      expect(getFormattedTime(start, end, isAllDay)).toMatchSnapshot();
    });

    test('должен возвращать время для события, которое начинается и заканчивается в одно время', () => {
      const start = Number(moment('2018-01-01T10:00'));
      const end = Number(moment('2018-01-01T10:00'));
      const isAllDay = false;

      expect(getFormattedTime(start, end, isAllDay)).toMatchSnapshot();
    });

    test('должен возвращать время для события, которое начинается в один день, а заканчивается в другой', () => {
      const start = Number(moment('2018-01-01T10:00'));
      const end = Number(moment('2018-01-03T11:00'));
      const isAllDay = false;

      expect(getFormattedTime(start, end, isAllDay)).toMatchSnapshot();
    });
  });
});
