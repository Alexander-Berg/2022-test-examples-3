import { DateUtils } from './DateUtils';

describe('DateUtils', () => {
  it.each([
    ['2022-02-07T11:05:31.751634Z', '07.02.2022 11:05:31'],
    ['2022-02-07T11:05:31.751Z', '07.02.2022 11:05:31'],
    ['2022-2-7T11:5:01.751Z', 'Invalid date'],
    ['2022-02-07', '07.02.2022 00:00:00'],
    ['2022-2-07', '07.02.2022 00:00:00'],
    ['', 'Invalid date'],
    [undefined, 'Invalid date'],
    [null, 'Invalid date'],
  ])('%s formats as %s', (a: string, expected: string) => {
    expect(DateUtils.formatDate(DateUtils.parseIsoDate(a))).toBe(expected);
  });
});
