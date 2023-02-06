import moment from 'moment';

import getValuesFromURL from '../getValuesFromURL';

describe('eventForm/utils/getValuesFromURL', () => {
  test('должен уметь обрабатывать основные параметры', () => {
    const values = {
      name: 'event_name',
      description: 'event_description',
      start: '2017-01-01T10:30:00',
      end: '2017-01-01T11:00:00',
      isAllDay: 0,
      othersCanView: 0
    };
    const expectedValues = {
      name: 'event_name',
      description: 'event_description',
      start: Number(moment('2017-01-01T10:30:00')),
      end: Number(moment('2017-01-01T11:00:00')),
      isAllDay: false,
      othersCanView: false
    };

    expect(getValuesFromURL(values)).toEqual(expectedValues);
  });

  test('должен уметь обрабатывать startTs и endTs', () => {
    const values = {
      startTs: '2017-01-01T10:30:00',
      endTs: '2017-01-01T11:00:00'
    };
    const expectedValues = {
      start: Number(moment('2017-01-01T10:30:00')),
      end: Number(moment('2017-01-01T11:00:00'))
    };

    expect(getValuesFromURL(values)).toEqual(expectedValues);
  });
});
