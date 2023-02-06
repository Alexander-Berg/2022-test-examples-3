import getOffsetParams from '../getOffsetParams';
import {MINUTE, HOUR, DAY, WEEK, IN_MOMENT} from '../../notificationsFieldConstants';

describe('notificationsField/utils/getOffsetParams', () => {
  describe('событие длится весь день', () => {
    test('должен возвращать корректные значения для напоминания в 00:00 дня события', () => {
      const offset = '0m';

      expect(getOffsetParams(offset, true)).toEqual({
        offset: 0,
        unit: IN_MOMENT,
        time: '00:00',
        offsetStr: offset
      });
    });
    test('должен возвращать корректные значения для напоминания в 23:59 дня события', () => {
      const offset = '1439m';

      expect(getOffsetParams(offset, true)).toEqual({
        offset: 0,
        unit: IN_MOMENT,
        time: '23:59',
        offsetStr: offset
      });
    });
    test('должен возвращать корректные значения для напоминания в 23:59 за день до события', () => {
      const offset = '-1m';

      expect(getOffsetParams(offset, true)).toEqual({
        offset: 1,
        unit: DAY,
        time: '23:59',
        offsetStr: offset
      });
    });
    test('должен возвращать корректные значения для напоминания в 23:59 за неделю до события', () => {
      const offset = '-8641m';

      expect(getOffsetParams(offset, true)).toEqual({
        offset: 1,
        unit: WEEK,
        time: '23:59',
        offsetStr: offset
      });
    });
    test('должен возвращать корректные значения для напоминания в 00:00 за неделю до события', () => {
      const offset = '-10080m';

      expect(getOffsetParams(offset, true)).toEqual({
        offset: 1,
        unit: WEEK,
        time: '00:00',
        offsetStr: offset
      });
    });

    test('должен возвращать корректные значения для напоминания в 00:00 за 9999 недель до события', () => {
      const offset = '-100789920m';

      expect(getOffsetParams(offset, true)).toEqual({
        offset: 9999,
        unit: WEEK,
        time: '00:00',
        offsetStr: offset
      });
    });
  });
  describe('событие не длится весь день', () => {
    test('должен возвращать корректные значения для напоминания в момент события', () => {
      const offset = '0m';

      expect(getOffsetParams(offset, false)).toEqual({
        offset: 0,
        unit: IN_MOMENT,
        time: '00:00',
        offsetStr: offset
      });
    });
    test('должен возвращать корректные значения для напоминания за 1 минуту до события', () => {
      const offset = '-1m';

      expect(getOffsetParams(offset, false)).toEqual({
        offset: 1,
        unit: MINUTE,
        time: '00:00',
        offsetStr: offset
      });
    });
    test('должен возвращать корректные значения для напоминания за 15 минут до события', () => {
      const offset = '-15m';

      expect(getOffsetParams(offset, false)).toEqual({
        offset: 15,
        unit: MINUTE,
        time: '00:00',
        offsetStr: offset
      });
    });
    test('должен возвращать корректные значения для напоминания за 23 часа до события', () => {
      const offset = '-1380m';

      expect(getOffsetParams(offset, false)).toEqual({
        offset: 23,
        unit: HOUR,
        time: '00:00',
        offsetStr: offset
      });
    });
    test('должен возвращать корректные значения для напоминания за 1 день до события', () => {
      const offset = '-1440m';

      expect(getOffsetParams(offset, false)).toEqual({
        offset: 1,
        unit: DAY,
        time: '00:00',
        offsetStr: offset
      });
    });
    test('должен возвращать корректные значения для напоминания за 6 дней до события', () => {
      const offset = '-8640m';

      expect(getOffsetParams(offset, false)).toEqual({
        offset: 6,
        unit: DAY,
        time: '00:00',
        offsetStr: offset
      });
    });
    test('должен возвращать корректные значения для напоминания за 1 неделю до события', () => {
      const offset = '-10080m';

      expect(getOffsetParams(offset, false)).toEqual({
        offset: 1,
        unit: WEEK,
        time: '00:00',
        offsetStr: offset
      });
    });
    test('должен возвращать корректные значения для напоминания за 9999 недель до события', () => {
      const offset = '-100789920m';

      expect(getOffsetParams(offset, false)).toEqual({
        offset: 9999,
        unit: WEEK,
        time: '00:00',
        offsetStr: offset
      });
    });
  });
});
