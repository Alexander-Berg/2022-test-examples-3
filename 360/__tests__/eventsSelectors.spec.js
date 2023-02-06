import moment from 'moment';
import {Map} from 'immutable';

import {refillList} from '../eventsReducer';
import {
  makeGetEventsForSelector,
  getActivatedEvent,
  getLastUpdateTs,
  getEventByUuid,
  getIsUpdatingDecision
} from '../eventsSelectors';
import EventRecord from '../EventRecord';

const getEventsForDaySelector = makeGetEventsForSelector('common');

const fixtures = {
  allday: [
    {
      id: 100,
      startTs: '2016-10-11T00:00:00',
      endTs: '2016-10-12T00:00:00',
      instanceStartTs: '2016-10-11T00:00:00',
      ownerId: 'rideorgtfo'
    },
    {
      id: 101,
      startTs: '2016-10-11T00:00:00',
      endTs: '2016-10-19T00:00:00',
      instanceStartTs: '2016-10-19T00:00:00',
      ownerId: 'rideorgtfo'
    },
    {
      id: 102,
      startTs: '2016-10-02T00:00:00',
      endTs: '2016-10-03T00:00:00',
      instanceStartTs: '2016-10-02T00:00:00',
      ownerId: 'rideorgtfo'
    },
    {
      id: 103,
      startTs: '2016-10-18T00:00:00',
      endTs: '2016-10-19T00:00:00',
      instanceStartTs: '2016-10-18T00:00:00',
      ownerId: 'rideorgtfo'
    },
    {
      id: 104,
      startTs: '2016-11-30T00:00:00',
      endTs: '2016-12-01T00:00:00',
      instanceStartTs: '2016-11-30T00:00:00',
      ownerId: 'rideorgtfo'
    },
    {
      id: 105,
      startTs: '2016-12-01T00:00:00',
      endTs: '2016-12-02T00:00:00',
      instanceStartTs: '2016-12-01T00:00:00',
      ownerId: 'rideorgtfo'
    }
  ],
  common: [
    {
      id: 200,
      startTs: '2016-10-11T04:00:00',
      endTs: '2016-10-11T04:30:00',
      instanceStartTs: '2016-10-11T04:00:00',
      ownerId: 'rideorgtfo'
    },
    {
      id: 201,
      startTs: '2016-09-11T00:00:00',
      endTs: '2016-09-11T00:30:00',
      instanceStartTs: '2016-09-11T00:00:00',
      ownerId: 'rideorgtfo'
    },
    {
      id: 202,
      startTs: '2016-10-18T00:00:00',
      endTs: '2016-10-18T03:00:00',
      instanceStartTs: '2016-10-18T00:00:00',
      ownerId: 'rideorgtfo'
    },
    {
      id: 203,
      startTs: '2016-11-30T00:00:00',
      endTs: '2016-11-30T23:00:00',
      instanceStartTs: '2016-11-30T23:00:00',
      ownerId: 'rideorgtfo'
    },
    {
      id: 204,
      startTs: '2016-12-01T00:00:00',
      endTs: '2016-12-01T23:00:00',
      instanceStartTs: '2016-12-01T23:00:00',
      ownerId: 'rideorgtfo'
    }
  ]
};

const initialState = new Map({
  byUuid: new Map(),
  byExternalId: new Map(),
  byTime: new Map({
    common: new Map(),
    allday: new Map()
  })
});

describe('eventsSelectors', function() {
  describe('eventsForDay', () => {
    test('должен вернуть события для переданного дня', () => {
      const state = {
        events: refillList(initialState, null, fixtures.common)
      };
      const props = {
        timestamp: Number(moment(fixtures.common[0].startTs).startOf('date'))
      };
      const getEvents = getEventsForDaySelector(state, props);
      const result = getEvents();

      expect(result.first()).toEqual(new EventRecord(fixtures.common[0]));
      expect(result.size).toBe(1);
    });

    test('должен правильно находить события на неделе, которая начинается в одном месяце, а заканчивается в другом', () => {
      const state = {
        events: refillList(initialState, null, fixtures.common)
      };
      const props = {
        timestamp: Number(moment(fixtures.common[4].startTs).startOf('date'))
      };
      const getEvents = getEventsForDaySelector(state, props);
      const result = getEvents();

      expect(result.first()).toEqual(new EventRecord(fixtures.common[4]));
      expect(result.size).toBe(1);
    });
  });

  describe('getActivatedEvent', () => {
    test('должен возвращать событие с флагом activated', () => {
      const state = {
        events: refillList(initialState, null, [
          {...fixtures.common[0], activated: true},
          {...fixtures.common[1], activated: false}
        ])
      };

      expect(getActivatedEvent(state)).toEqual(
        new EventRecord({
          ...fixtures.common[0],
          activated: true
        })
      );
    });

    test('должен возвращать undefined, если нет события с флагом activated', () => {
      const state = {
        events: refillList(initialState, null, [
          {...fixtures.common[0], activated: false},
          {...fixtures.common[1], activated: false}
        ])
      };

      expect(getActivatedEvent(state)).toBeUndefined();
    });
  });

  describe('getEventByUuid', () => {
    test('должен возвращать событие с нужным uuid', () => {
      const state = {
        events: refillList(initialState, null, fixtures.common)
      };
      const uuid = new EventRecord(fixtures.common[0]).uuid;

      expect(getEventByUuid(state, {uuid})).toEqual(new EventRecord(fixtures.common[0]));
    });
  });

  describe('getLastUpdateTs', () => {
    test('должен возвращать дату последнего обновления событий', () => {
      const state = {
        events: new Map({
          lastUpdateTs: Number(moment('2018-01-20'))
        })
      };

      expect(getLastUpdateTs(state)).toEqual(Number(moment('2018-01-20')));
    });
  });

  describe('getIsUpdatingDecision', () => {
    test('должен вернуть true, если событие с таким uuid есть в мапе', () => {
      const event = new EventRecord({});
      const state = {
        event: new Map({
          isUpdatingDecision: new Map({
            [event.uuid]: true
          })
        })
      };

      expect(getIsUpdatingDecision(state, {uuid: event.uuid})).toBe(true);
    });

    test('должен вернуть undefined, если такого ключа нет', () => {
      const event = new EventRecord({});
      const state = {
        event: new Map({
          isUpdatingDecision: new Map({
            [event.uuid + 'random']: true
          })
        })
      };

      expect(getIsUpdatingDecision(state, {uuid: event.uuid})).toBe(undefined);
    });
  });
});
