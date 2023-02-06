import 'utils/date/toLocalISOString';
import { dateFormat, dateParse } from '../dateConvertor';

describe('date parse test', () => {
  test('format', () => {
    expect(dateFormat('2017-02-14T00:00:00.000Z')).toBe('14.02.2017');
  });

  test('formatWithTime', () => {
    expect(dateFormat('2017-02-14T20:30:00.000Z', true)).toBe('14.02.2017 20:30');
  });

  test('parse', () => {
    expect(dateParse('14.02.2017')).toBe('2017-02-14T00:00:00.000Z');
  });

  test('parseWithTime', () => {
    expect(dateParse('14.02.2017 20:30', true)).toBe('2017-02-14T20:30:00.000Z');
  });
});
