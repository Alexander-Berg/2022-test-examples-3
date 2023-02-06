import { getAlias, convertDateTo, EDateType } from 'sport/lib/date';
import { TimePeriods } from 'neo/lib/number';

const mskYearBegin = convertDateTo(EDateType.TIMESTAMP, '2021-12-31T21:00:00.000Z');
const currentTimestamp = convertDateTo(EDateType.TIMESTAMP, '2022-03-21T18:52:00.000Z');
const timezone = 3;

test('getAlias: раньше начала года', () => {
  const date = {
    timestamp: mskYearBegin,
    timezone: 2,
    currentTimestamp,
  };

  const actualAlias = getAlias(date);
  const expectedAlias = 'before_year_begin';

  expect(actualAlias).toEqual(expectedAlias);
});

test('getAlias: начало года', () => {
  const date = {
    timestamp: mskYearBegin,
    timezone,
    currentTimestamp,
  };

  const actualAlias = getAlias(date);
  const expectedAlias = 'year_begin';

  expect(actualAlias).toEqual(expectedAlias);
});

test('getAlias: вчера', () => {
  const date = {
    timestamp: currentTimestamp - TimePeriods.DAY,
    timezone,
    currentTimestamp,
  };

  const actualAlias = getAlias(date);
  const expectedAlias = 'yesterday';

  expect(actualAlias).toEqual(expectedAlias);
});

test('getAlias: сегодня', () => {
  const date = {
    timestamp: currentTimestamp + TimePeriods.HOUR,
    timezone,
    currentTimestamp,
  };

  const actualAlias = getAlias(date);
  const expectedAlias = 'today';

  expect(actualAlias).toEqual(expectedAlias);
});

test('getAlias: завтра', () => {
  const date = {
    timestamp: currentTimestamp + TimePeriods.DAY,
    timezone,
    currentTimestamp,
  };

  const actualAlias = getAlias(date);
  const expectedAlias = 'tomorrow';

  expect(actualAlias).toEqual(expectedAlias);
});

test('getAlias: конец года', () => {
  const date = {
    timestamp: currentTimestamp + 2 * TimePeriods.DAY,
    timezone,
    currentTimestamp,
  };

  const actualAlias = getAlias(date);
  const expectedAlias = 'future';

  expect(actualAlias).toEqual(expectedAlias);
});

test('getAlias: следующий год', () => {
  const date = {
    timestamp: 1640984400000 + 365 * TimePeriods.DAY,
    timezone,
    currentTimestamp,
  };

  const actualAlias = getAlias(date);
  const expectedAlias = 'next_year_begin';

  expect(actualAlias).toEqual(expectedAlias);
});
