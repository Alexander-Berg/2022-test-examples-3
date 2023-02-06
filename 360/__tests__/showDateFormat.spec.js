import moment from 'moment';

import showDateFormat from 'utils/dates/showDateFormat';

describe('showDateFormat', () => {
  test('должен возвращать дату в формате YYYY-MM-DD', () => {
    expect(showDateFormat(Number(moment('2017-01-01')))).toBe('2017-01-01');
  });
});
