import {Map, Seq} from 'immutable';

import EventRecord from '../../events/EventRecord';
import LayerRecord from '../../layers/LayerRecord';
import {
  getScheduleRange,
  getShowDate,
  getDays,
  getDaysEvents,
  getFilteredDaysEvents,
  getScheduleEvents,
  getEventsCount,
  getIsOwnSchedule,
  getUserFromUrl
} from '../scheduleSelectors';

const event1 = new EventRecord({
  id: 100,
  startTs: '2019-09-09T00:00:00',
  endTs: '2019-09-10T00:00:00',
  instanceStartTs: '2019-09-09T00:00:00',
  layerId: '201',
  ownerId: 'rideorgtfo'
});

const event2 = new EventRecord({
  id: 101,
  startTs: '2019-09-09T00:00:00',
  endTs: '2019-09-09T04:00:00',
  instanceStartTs: '2019-09-09T00:00:00',
  layerId: '202',
  ownerId: 'rideorgtfo'
});

const event3 = new EventRecord({
  id: 102,
  startTs: '2019-09-09T00:00:00',
  endTs: '2019-09-09T12:00:00',
  instanceStartTs: '2019-09-09T00:00:00',
  layerId: '202',
  ownerId: 'pistch'
});

describe('scheduleSelectors', () => {
  describe('getScheduleRange', () => {
    test('должен вернуть scheduleRange', () => {
      const range = {
        start: 1,
        end: 100500
      };

      const state = {
        schedule: {
          range
        }
      };

      expect(getScheduleRange(state)).toEqual(range);
    });
  });
  describe('getShowDate', () => {
    test('должен вернуть showDate', () => {
      const showDate = Date.now();

      const state = {
        schedule: {
          showDate
        }
      };

      expect(getShowDate(state)).toEqual(showDate);
    });
  });
  describe('getDays', () => {
    test('должен вернуть массив ts начала дней', () => {
      const start = 1567976400000;
      const range = {
        start,
        end: start + 86400000 * 3
      };

      const state = {
        schedule: {range}
      };

      expect(getDays(state)).toEqual([start, start + 86400000, start + 86400000 * 2]);
    });
  });
  describe('getDaysEvents', () => {
    test('должен возвратить массив дней с встречами', () => {
      const start = 1567976400000;

      const daysStart = [start, start + 86400000, start + 86400000 * 2];
      const events = {
        allDay: new Map({
          '1546290000000': new Map({
            '1567285200000': new Map({
              '1567976400000': new Map({
                '1567976400000': new Map({
                  [event2.uuid]: event2
                })
              })
            })
          })
        }),
        common: new Map({
          '1546290000000': new Map({
            '1567285200000': new Map({
              '1567976400000': new Map({
                '1567976400000': new Map({
                  [event1.uuid]: event1,
                  [event3.uuid]: event3
                })
              })
            })
          })
        })
      };

      const getCommonEvents = jest.fn(() => events.common);
      const getAllDayEvents = jest.fn(() => events.allDay);

      const days = getDaysEvents.resultFunc(
        daysStart,
        {getCommonEvents, getAllDayEvents},
        {login: 'rideorgtfo'}
      );

      const expectedEventsCount = 3;
      const actualEventsCount = days.reduce(
        (acc, day) => acc + day.common.size + day.allDay.size,
        0
      );

      expect(days).toHaveLength(daysStart.length);
      expect(days).toContainEqual(
        expect.objectContaining({
          start: expect.anything(),
          common: expect.anything(),
          allDay: expect.anything()
        })
      );

      expect(expectedEventsCount).toEqual(actualEventsCount);
      expect(getCommonEvents).toBeCalledWith('rideorgtfo');
      expect(getAllDayEvents).toBeCalledWith('rideorgtfo');
    });
  });
  describe('getFilteredDaysEvents', () => {
    const daysBefore = [
      {
        common: Seq([event1, event3]),
        allDay: Seq([event2]),
        start: 1567976400000
      },
      {
        common: Seq(),
        allDay: Seq(),
        start: 1568062800000
      },
      {
        common: Seq(),
        allDay: Seq(),
        start: 1568149200000
      }
    ];
    test('должен вернуть массив дней с встречами слои которых включены', () => {
      const layers = new Map({
        '201': new LayerRecord({
          isToggledOn: true,
          id: 201
        }),
        '202': new LayerRecord({
          isToggledOn: false,
          id: 202
        })
      });

      const days = getFilteredDaysEvents.resultFunc(daysBefore, layers, true);

      const expectedEventsCount = 1;
      const actualEventsCount = days.reduce(
        (acc, day) => acc + day.common.size + day.allDay.size,
        0
      );

      expect(days).toHaveLength(daysBefore.length);
      expect(days).toContainEqual(
        expect.objectContaining({
          start: expect.anything(),
          common: expect.anything(),
          allDay: expect.anything()
        })
      );

      expect(expectedEventsCount).toEqual(actualEventsCount);
    });
    test('должен вернуть массив дней без фильтрации, если чужое расписание', () => {
      const layers = new Map({
        '201': new LayerRecord({
          isToggledOn: true,
          id: 201
        }),
        '202': new LayerRecord({
          isToggledOn: false,
          id: 202
        })
      });

      const days = getFilteredDaysEvents.resultFunc(daysBefore, layers, false);

      const expectedEventsCount = 3;
      const actualEventsCount = days.reduce(
        (acc, day) => acc + day.common.size + day.allDay.size,
        0
      );

      expect(days).toHaveLength(daysBefore.length);
      expect(days).toContainEqual(
        expect.objectContaining({
          start: expect.anything(),
          common: expect.anything(),
          allDay: expect.anything()
        })
      );

      expect(expectedEventsCount).toEqual(actualEventsCount);
    });
  });

  describe('getScheduleEvents', () => {
    test('должен вернуть массив дней с полем isToday', () => {
      const daysBefore = [
        {
          common: Seq([event1, event3]),
          allDay: Seq([event2]),
          start: 1567976400000
        },
        {
          common: Seq(),
          allDay: Seq(),
          start: 1568062800000
        }
      ];

      const today = daysBefore[0].start;

      const days = getScheduleEvents.resultFunc(daysBefore, today);

      expect(days[0].isToday).toBe(true);
      expect(days[1].isToday).toBe(false);
    });
  });

  describe('getEventsCount', () => {
    test('должен вернуть количество событий', () => {
      const expectedCount = 3;

      const days = [
        {
          common: Seq([event1, event3]),
          allDay: Seq([event2]),
          start: 1567976400000
        },
        {
          common: Seq(),
          allDay: Seq(),
          start: 1568062800000
        }
      ];

      const count = getEventsCount.resultFunc(days);

      expect(count).toBe(expectedCount);
    });
  });
});

describe('getUserLoginFromUrl', () => {
  test('должен вернуть null, если в урле нет логина', () => {
    const expected = getUserFromUrl.resultFunc({
      pathname: '/schedule'
    });

    expect(expected).toBe(null);
  });
  test('должен вернуть логин, если он есть в урле', () => {
    const expected = getUserFromUrl.resultFunc({
      pathname: '/schedule/rideorgtfo'
    });

    expect(expected).toEqual({login: 'rideorgtfo'});
  });
  test('должен вернуть логин, если передан email', () => {
    const expected = getUserFromUrl.resultFunc({
      pathname: '/schedule/rideorgtfo@yandex-team.ru'
    });

    expect(expected).toEqual({login: 'rideorgtfo', email: 'rideorgtfo@yandex-team.ru'});
  });

  describe('getIsOwnSchedule', () => {
    test('должен вернуть true если нет логина', () => {
      const expected = getIsOwnSchedule.resultFunc(null);
      expect(expected).toBe(true);
    });
    test('должен вернуть false если есть логин', () => {
      const expected = getIsOwnSchedule.resultFunc({login: 'rideorgtfo'});
      expect(expected).toBe(false);
    });
  });
});
