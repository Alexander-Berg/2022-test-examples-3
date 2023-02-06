import moment from 'moment';

import eventDateFormat from 'utils/dates/eventDateFormat';

describe('eventDateFormat', () => {
  test('должен возвращать дату в формате YYYY-MM-DDTHH:mm:ss', () => {
    expect(eventDateFormat(Number(moment('2017-01-01T10:00:00')))).toBe('2017-01-01T10:00:00');
  });

  test('должен возвращать дату в формате YYYY-MM-DDTHH:mm:ss в utc', () => {
    expect(eventDateFormat(Number(moment('2017-01-01T10:00:00')), {utc: true})).toBe(
      '2017-01-01T07:00:00'
    );
  });
});
