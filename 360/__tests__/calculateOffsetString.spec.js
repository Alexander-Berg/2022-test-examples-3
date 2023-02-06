import calculateOffsetString from '../calculateOffsetString';
import {MINUTE, HOUR, DAY, WEEK, IN_MOMENT} from '../../notificationsFieldConstants';

describe('calculateOffsetString', () => {
  describe('событие длится весь день', () => {
    test('должен возвращать корректные значения для напоминания в 00:00 дня события', () => {
      expect(
        calculateOffsetString(
          {
            offset: 0,
            unit: IN_MOMENT,
            time: '00:00'
          },
          true
        )
      ).toEqual('0m');
    });
    test('должен возвращать корректные значения для напоминания в 23:59 дня события', () => {
      expect(
        calculateOffsetString(
          {
            offset: 0,
            unit: IN_MOMENT,
            time: '23:59'
          },
          true
        )
      ).toEqual('1439m');
    });
    test('должен возвращать корректные значения для напоминания в 23:59 за день до события', () => {
      expect(
        calculateOffsetString(
          {
            offset: 1,
            unit: DAY,
            time: '23:59'
          },
          true
        )
      ).toEqual('-1m');
    });
    test('должен возвращать корректные значения для напоминания в 23:59 за неделю до события', () => {
      expect(
        calculateOffsetString(
          {
            offset: 1,
            unit: WEEK,
            time: '23:59'
          },
          true
        )
      ).toEqual('-8641m');
    });
    test('должен возвращать корректные значения для напоминания в 00:00 за неделю до события', () => {
      expect(
        calculateOffsetString(
          {
            offset: 1,
            unit: WEEK,
            time: '00:00'
          },
          true
        )
      ).toEqual('-10080m');
    });

    test('должен возвращать корректные значения для напоминания в 00:00 за 9999 недель до события', () => {
      expect(
        calculateOffsetString(
          {
            offset: 9999,
            unit: WEEK,
            time: '00:00'
          },
          true
        )
      ).toEqual('-100789920m');
    });
  });

  describe('событие не длится весь день', () => {
    test('должен возвращать корректные значения для напоминания в момент события', () => {
      expect(
        calculateOffsetString(
          {
            offset: 0,
            unit: IN_MOMENT,
            time: '00:00'
          },
          false
        )
      ).toEqual('0m');
    });
    test('должен возвращать корректные значения для напоминания за 1 минуту до события', () => {
      expect(
        calculateOffsetString(
          {
            offset: 1,
            unit: MINUTE,
            time: '00:00'
          },
          false
        )
      ).toEqual('-1m');
    });
    test('должен возвращать корректные значения для напоминания за 15 минут до события', () => {
      expect(
        calculateOffsetString(
          {
            offset: 15,
            unit: MINUTE,
            time: '00:00'
          },
          false
        )
      ).toEqual('-15m');
    });
    test('должен возвращать корректные значения для напоминания за 23 часа до события', () => {
      expect(
        calculateOffsetString(
          {
            offset: 23,
            unit: HOUR,
            time: '00:00'
          },
          false
        )
      ).toEqual('-1380m');
    });
    test('должен возвращать корректные значения для напоминания за 1 день до события', () => {
      expect(
        calculateOffsetString(
          {
            offset: 1,
            unit: DAY,
            time: '00:00'
          },
          false
        )
      ).toEqual('-1440m');
    });
    test('должен возвращать корректные значения для напоминания за 6 дней до события', () => {
      expect(
        calculateOffsetString(
          {
            offset: 6,
            unit: DAY,
            time: '00:00'
          },
          false
        )
      ).toEqual('-8640m');
    });
    test('должен возвращать корректные значения для напоминания за 1 неделю до события', () => {
      expect(
        calculateOffsetString(
          {
            offset: 1,
            unit: WEEK,
            time: '00:00'
          },
          false
        )
      ).toEqual('-10080m');
    });
    test('должен возвращать корректные значения для напоминания за 9999 недель до события', () => {
      expect(
        calculateOffsetString(
          {
            offset: 9999,
            unit: WEEK,
            time: '00:00'
          },
          false
        )
      ).toEqual('-100789920m');
    });
  });
});
