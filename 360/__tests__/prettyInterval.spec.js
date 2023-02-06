import moment from 'moment';

import prettyInterval from '../prettyInterval';

jest.mock('utils/i18n');

describe('suggestMeetingTimes/utils/prettyInterval', () => {
  test('должен возвращать правильный интервал для isToday == true', () => {
    const start = moment().valueOf();
    const end = moment()
      .add(1, 'day')
      .valueOf();

    expect(prettyInterval(start, end)).toBe('suggestMeetingTimes.prettyIntervalToday');
  });

  test('должен возвращать правильный интервал для isTomorrow == true', () => {
    const start = moment()
      .add(1, 'day')
      .valueOf();
    const end = moment()
      .add(2, 'day')
      .valueOf();

    expect(prettyInterval(start, end)).toBe('suggestMeetingTimes.prettyIntervalTomorrow');
  });

  test('должен возвращать правильный интервал для (isToday)&&(isTomorrow) == false', () => {
    const start = moment()
      .add(-1, 'day')
      .valueOf();
    const end = moment().valueOf();

    expect(prettyInterval(start, end)).toBe('suggestMeetingTimes.prettyIntervalDate');
  });
});
