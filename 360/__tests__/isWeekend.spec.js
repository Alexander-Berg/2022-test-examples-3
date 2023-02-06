import moment from 'moment';

import isWeekend from 'utils/dates/isWeekend';

describe('isWeekend', () => {
  test('должен возвращать false для понедельника', () => {
    expect(isWeekend(moment().day(1))).toBe(false);
  });

  test('должен возвращать false для вторника', () => {
    expect(isWeekend(moment().day(2))).toBe(false);
  });

  test('должен возвращать false для среды', () => {
    expect(isWeekend(moment().day(3))).toBe(false);
  });

  test('должен возвращать false для четверга', () => {
    expect(isWeekend(moment().day(4))).toBe(false);
  });

  test('должен возвращать false для пятницы', () => {
    expect(isWeekend(moment().day(5))).toBe(false);
  });

  test('должен возвращать true для субботы', () => {
    expect(isWeekend(moment().day(6))).toBe(true);
  });

  test('должен возвращать true для воскресенья', () => {
    expect(isWeekend(moment().day(0))).toBe(true);
  });
});
