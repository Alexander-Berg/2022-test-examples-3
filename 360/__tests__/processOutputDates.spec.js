import moment from 'moment';

import processOutputDates from '../processOutputDates';

describe('eventForm/utils/processOutputDates', () => {
  test('должен вернуть начало и конец события не изменяя их, если событие не на весь день', () => {
    const data = {
      start: Number(moment('2018-04-26T10:00:00')),
      end: Number(moment('2018-04-26T10:30:00')),
      isAllDay: false
    };

    expect(processOutputDates(data)).toEqual({
      start: '2018-04-26T10:00:00',
      end: '2018-04-26T10:30:00'
    });
  });

  test('должен вернуть начало события = налачу дня и конец события = началу следующего дня, если событие на весь день', () => {
    const data = {
      start: Number(moment('2018-04-26T10:00:00')),
      end: Number(moment('2018-04-26T10:30:00')),
      isAllDay: true
    };

    expect(processOutputDates(data)).toEqual({
      start: '2018-04-26T00:00:00',
      end: '2018-04-27T00:00:00'
    });
  });
});
