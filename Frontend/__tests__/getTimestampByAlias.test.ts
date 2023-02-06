import { getTimestampByAlias, convertDateTo, EDateType } from 'sport/lib/date';
import { EDateAlias } from 'sport/types/date';

const currentTimestamp = convertDateTo(EDateType.TIMESTAMP, '2022-03-21T18:52:00.000Z');
const timezone = 3;

test('getTimestampByAlias: начало года', () => {
  const date = {
    timestamp: convertDateTo(EDateType.TIMESTAMP, '2022-02-25T05:45:00.000Z'),
    timezone,
    currentTimestamp,
  };

  const actualTimestamp = getTimestampByAlias(EDateAlias.YEAR_BEGIN, date);
  const expectedTimestamp = convertDateTo(EDateType.TIMESTAMP, '2021-12-31T21:00:00.000Z');

  expect(actualTimestamp).toEqual(expectedTimestamp);
});

test('getTimestampByAlias: вчера', () => {
  const date = {
    timestamp: convertDateTo(EDateType.TIMESTAMP, '2022-02-25T05:45:00.000Z'),
    timezone,
    currentTimestamp,
  };

  const actualTimestamp = getTimestampByAlias(EDateAlias.YESTERDAY, date);
  const expectedTimestamp = convertDateTo(EDateType.TIMESTAMP, '2022-02-23T21:00:00.000Z');

  expect(actualTimestamp).toEqual(expectedTimestamp);
});

test('getTimestampByAlias: сегодня', () => {
  const date = {
    timestamp: convertDateTo(EDateType.TIMESTAMP, '2022-02-25T05:45:00.000Z'),
    timezone,
    currentTimestamp,
  };

  const actualTimestamp = getTimestampByAlias(EDateAlias.TODAY, date);
  const expectedTimestamp = convertDateTo(EDateType.TIMESTAMP, '2022-02-24T21:00:00.000Z');

  expect(actualTimestamp).toEqual(expectedTimestamp);
});

test('getTimestampByAlias: завтра', () => {
  const date = {
    timestamp: convertDateTo(EDateType.TIMESTAMP, '2022-02-25T05:45:00.000Z'),
    timezone,
    currentTimestamp,
  };

  const actualTimestamp = getTimestampByAlias(EDateAlias.TOMORROW, date);
  const expectedTimestamp = convertDateTo(EDateType.TIMESTAMP, '2022-02-25T21:00:00.000Z');

  expect(actualTimestamp).toEqual(expectedTimestamp);
});

test('getTimestampByAlias: начало следующего месяца', () => {
  const date = {
    timestamp: convertDateTo(EDateType.TIMESTAMP, '2022-02-25T05:45:00.000Z'),
    timezone,
    currentTimestamp,
  };

  const actualTimestamp = getTimestampByAlias(EDateAlias.NEXT_MONTH_BEGIN, date);
  const expectedTimestamp = convertDateTo(EDateType.TIMESTAMP, '2022-02-28T21:00:00.000Z');

  expect(actualTimestamp).toEqual(expectedTimestamp);
});

test('getTimestampByAlias: начало следующего года', () => {
  const date = {
    timestamp: convertDateTo(EDateType.TIMESTAMP, '2022-02-25T05:45:00.000Z'),
    timezone,
    currentTimestamp,
  };

  const actualTimestamp = getTimestampByAlias(EDateAlias.NEXT_YEAR_BEGIN, date);
  const expectedTimestamp = convertDateTo(EDateType.TIMESTAMP, '2022-12-31T21:00:00.000Z');

  expect(actualTimestamp).toEqual(expectedTimestamp);
});
