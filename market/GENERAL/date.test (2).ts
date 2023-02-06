import { getDate, getDateString, secondsToDate } from './date';

describe('utils/date.ts', () => {
  const originalToLocaleString = Date.prototype.toLocaleString;
  beforeAll(() => {
    // nodejs does not know about ru locale
    // eslint-disable-next-line no-extend-native
    Date.prototype.toLocaleString = function toLocaleString(this: Date) {
      const dateString = [
        `00${this.getUTCDate()}`.slice(-2),
        `00${this.getUTCMonth() + 1}`.slice(-2),
        `${this.getUTCFullYear()}`,
      ].join('.');

      const timeString = [
        `00${this.getUTCHours()}`.slice(-2),
        `00${this.getUTCMinutes()}`.slice(-2),
        `00${this.getUTCSeconds()}`.slice(-2),
      ].join(':');

      return `${dateString}, ${timeString}`;
    };
  });

  afterAll(() => {
    // eslint-disable-next-line no-extend-native
    Date.prototype.toLocaleString = originalToLocaleString;
  });

  it.each([
    [undefined, undefined],
    [null, undefined],
    ['', undefined],
    [NaN, undefined],
    [Infinity, undefined],
    [-Infinity, undefined],
    [new Date(Date.UTC(1994, 5, 24)).getTime() / 1000, new Date(Date.UTC(1994, 5, 24))],
  ])('secondsToDate(%s)', (seconds: undefined | string | number | null, expected) => {
    expect(secondsToDate(seconds as unknown as any)).toStrictEqual(expected);
  });

  it.each([
    [undefined, ''],
    [null, ''],
    [new Date(Date.UTC(1995, 8, 22, 0, 0, 0, 0)), '22.09.1995, 00:00:00'],
  ])('getDateString(%s)', (seconds: undefined | Date | null, expected) => {
    expect(getDateString(seconds as unknown as any)).toBe(expected);
  });

  it.each([
    [undefined, ''],
    [null, ''],
    [new Date(Date.UTC(1964, 8, 2)).getTime() / 1000, '02.09.1964, 00:00:00'],
  ])('getDate(%s)', (seconds: undefined | number | null, expected) => {
    expect(getDate(seconds as unknown as any)).toBe(expected);
  });
});
