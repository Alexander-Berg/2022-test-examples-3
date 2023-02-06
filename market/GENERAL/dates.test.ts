import { parseISO } from 'date-fns';
import MockDate from 'mockdate';

import { parseTimestamp, formatDateTime, formatDate, formatISODate, getTodayISO } from './dates';

const MOCK_ISO_DATE = '2021-05-21';
const MOCK_TIMESTAMP = parseISO(MOCK_ISO_DATE).getTime();

const MOCK_DATE = '21.05.2021';
const MOCK_DATETIME = `${MOCK_DATE}, 00:00:00`;

const BROKEN_STRING_DATE = 'BROKEN_STRING_DATE';

describe('Replenishment::Utils::Dates', () => {
  afterEach(() => {
    MockDate.reset();
  });

  // parseTimestamp
  it('`parseTimestamp`: should parse timestamp', () => {
    expect(() => {
      expect(parseTimestamp(MOCK_ISO_DATE)).toStrictEqual(MOCK_TIMESTAMP);
    }).not.toThrow();
  });

  it('`parseTimestamp`: should set default timestamp as 0 for invalid date', () => {
    expect(() => {
      expect(parseTimestamp(BROKEN_STRING_DATE)).toStrictEqual(0);
    }).not.toThrow();
  });

  // formatDateTime
  it('`formatDateTime`: should format String date correctly', () => {
    expect(() => {
      expect(formatDateTime(MOCK_ISO_DATE)).toStrictEqual(MOCK_DATETIME);
    }).not.toThrow();
  });

  it('`formatDateTime`: should format Date date correctly', () => {
    expect(() => {
      expect(formatDateTime(new Date(MOCK_TIMESTAMP))).toStrictEqual(MOCK_DATETIME);
    }).not.toThrow();
  });

  it('`formatDateTime`: should format Null date as empty string', () => {
    expect(() => {
      expect(formatDateTime(null)).toStrictEqual('');
    }).not.toThrow();
  });

  it('`formatDateTime`: should format undefined date as empty string', () => {
    expect(() => {
      expect(formatDateTime()).toStrictEqual('');
    }).not.toThrow();
  });

  it('`formatDateTime`: should format invalid date as empty string', () => {
    expect(() => {
      expect(formatDateTime(BROKEN_STRING_DATE)).toStrictEqual('');
    }).not.toThrow();
  });

  // formatDate
  it('`formatDate`: should format String date correctly', () => {
    expect(() => {
      expect(formatDate(MOCK_ISO_DATE)).toStrictEqual(MOCK_DATE);
    }).not.toThrow();
  });

  it('`formatDate`: should format Date date correctly', () => {
    expect(() => {
      expect(formatDate(new Date(MOCK_TIMESTAMP))).toStrictEqual(MOCK_DATE);
    }).not.toThrow();
  });

  it('`formatDate`: should format Null date as empty string', () => {
    expect(() => {
      expect(formatDate(null)).toStrictEqual('');
    }).not.toThrow();
  });

  it('`formatDate`: should format undefined date as empty string', () => {
    expect(() => {
      expect(formatDate()).toStrictEqual('');
    }).not.toThrow();
  });

  it('`formatDate`: should format invalid date as empty string', () => {
    expect(() => {
      expect(formatDate(BROKEN_STRING_DATE)).toStrictEqual('');
    }).not.toThrow();
  });

  // formatISODate
  it('`formatISODate`: should format String date correctly', () => {
    expect(() => {
      expect(formatISODate(MOCK_ISO_DATE)).toStrictEqual(MOCK_ISO_DATE);
    }).not.toThrow();
  });

  it('`formatISODate`: should format Date date correctly', () => {
    expect(() => {
      expect(formatISODate(new Date(MOCK_TIMESTAMP))).toStrictEqual(MOCK_ISO_DATE);
    }).not.toThrow();
  });

  it('`formatISODate`: should throw RangeError on Null date', () => {
    expect(() => {
      formatISODate(null);
    }).toThrowError(RangeError);
  });

  it('`formatISODate`: should throw RangeError on undefined date', () => {
    expect(() => {
      formatISODate();
    }).toThrowError(RangeError);
  });

  it('`formatISODate`: should throw RangeError on invalid date', () => {
    expect(() => {
      formatISODate(BROKEN_STRING_DATE);
    }).toThrowError(RangeError);
  });

  // getTodayISO
  it('`getTodayISO`: should return correctly formatted current ISO date', () => {
    MockDate.set(MOCK_ISO_DATE);

    expect(() => {
      expect(getTodayISO()).toStrictEqual(MOCK_ISO_DATE);
    }).not.toThrow();
  });
});
