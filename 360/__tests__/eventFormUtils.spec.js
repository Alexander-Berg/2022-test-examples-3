import {getSingleEventUrl, getEventSeriesUrl} from '../utils/getEventUrl';

describe('eventForm/utils', () => {
  describe('getEventUrl', () => {
    describe('getSingleEventUrl', () => {
      test('должен сформировать ссылку из переданных параметров', () => {
        const event = {
          id: '42',
          layerId: 'event.layerId',
          instanceStartTs: 0
        };

        const eventUrl = getSingleEventUrl(event);
        expect(eventUrl).toBe(
          '/event/42?applyToFuture=0&event_date=1970-01-01T03%3A00%3A00&layerId=event.layerId'
        );
      });

      test('должен сохранить query-параметр show_date', () => {
        const event = {
          id: '42',
          layerId: 'event.layerId',
          instanceStartTs: 0
        };

        const query = {
          show_date: '2018-01-01'
        };

        const eventUrl = getSingleEventUrl(event, query);
        expect(eventUrl).toBe(
          '/event/42?applyToFuture=0&event_date=1970-01-01T03%3A00%3A00&layerId=event.layerId&show_date=2018-01-01'
        );
      });
    });

    describe('getEventSeriesUrl', () => {
      test('должен сформировать ссылку из переданных параметров для редактирования всех событий серии', () => {
        const event = {
          id: '42',
          layerId: 'event.layerId',
          instanceStartTs: 0,
          isRecurrence: true
        };

        const eventUrl = getEventSeriesUrl(event);
        expect(eventUrl).toBe(
          '/event/42?applyToFuture=1&event_date=1970-01-01T03%3A00%3A00&layerId=event.layerId&recurrenceAsOccurrence=1'
        );
      });

      test('должен сформировать ссылку из переданных параметров для редактирования исключения из серии', () => {
        const event = {
          id: '42',
          layerId: 'event.layerId',
          instanceStartTs: 0,
          isRecurrence: false
        };

        const eventUrl = getEventSeriesUrl(event);
        expect(eventUrl).toBe(
          '/event/42?applyToFuture=1&event_date=1970-01-01T03%3A00%3A00&layerId=event.layerId'
        );
      });

      test('должен сохранить переданный query-параметр show_date', () => {
        const event = {
          id: '42',
          layerId: 'event.layerId',
          instanceStartTs: 0,
          isRecurrence: true
        };

        const query = {
          show_date: '2018-01-01'
        };

        const eventUrl = getEventSeriesUrl(event, query);
        expect(eventUrl).toBe(
          '/event/42?applyToFuture=1' +
            '&event_date=1970-01-01T03%3A00%3A00' +
            '&layerId=event.layerId' +
            '&recurrenceAsOccurrence=1' +
            '&show_date=2018-01-01'
        );
      });
    });
  });
});
