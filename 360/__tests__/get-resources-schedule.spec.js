const filter = require('../get-resources-schedule');

describe('get-resources-schedule', () => {
  test('должен возвращать интервалы, если не передали событий', () => {
    const intervals = 'INTERVALS';

    expect(filter(intervals)).toEqual(intervals);
  });
  test('должен возвращать интервалы, если не передали событий в объекте ответа сервера', () => {
    const intervals = 'INTERVALS';
    const events = {};

    expect(filter(intervals, events)).toEqual(intervals);
  });
  test('должен возвращать интервалы, обогащённые событиями', () => {
    const testParam = 'TEST_PARAM';
    const intervals = {
      offices: [
        {
          resources: [
            {
              events: [{eventId: 777}]
            }
          ]
        }
      ]
    };
    const expectedIntervals = {
      offices: [
        {
          resources: [
            {
              events: [{eventId: 777, id: 777, testParam}]
            }
          ]
        }
      ]
    };
    const events = {
      events: [{id: 777, testParam}]
    };

    expect(filter(intervals, events)).toEqual(expectedIntervals);
  });
});
