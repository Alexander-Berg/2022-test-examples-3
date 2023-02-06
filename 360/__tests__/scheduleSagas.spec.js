import {LOCATION_CHANGE} from 'connected-react-router';
import {expectSaga, testSaga} from 'redux-saga-test-plan';
import {call, select} from 'redux-saga-test-plan/matchers';
import {throwError} from 'redux-saga-test-plan/providers';
import {takeLatest, takeEvery} from 'redux-saga/effects';

import conditionalSaga from 'utils/conditionalSaga';
import {getEvents, getEventsDone, getEventsByLogin} from 'features/events/eventsActions';
import {ActionTypes as EventsActionTypes} from 'features/events/eventsConstants';
import {ActionTypes as LayersActionTypes} from 'features/layers/layersConstants';
import SagaErrorReporter from 'utils/SagaErrorReporter';

import makeEventsDonePattern from '../utils/makeEventsDonePattern';
import {
  getEventsCount,
  getScheduleEvents,
  getScheduleRange,
  getUserFromUrl,
  getShowDate,
  getIsInitialLoading
} from '../scheduleSelectors';
import * as actions from '../scheduleActions';
import {ActionTypes} from '../scheduleConstants';
import rootSaga, {
  extendRange,
  extendRangeUp,
  extendRangeDown,
  loadEvents,
  updateRange,
  updateRangeWithShowDate,
  handleShowDateChange,
  handleShowDateChangeOnEmptyState,
  syncShowDateWithLocation,
  updateScheduleList,
  getUserInfo,
  init,
  handleSetActiveEvent
} from '../scheduleSagas';

const errorReporter = new SagaErrorReporter('schedule');

jest.mock('utils/conditionalSaga');

conditionalSaga.mockImplementation((selector, saga) => saga);

describe('scheduleSagas', () => {
  describe('root', () => {
    test('должен подписываться на экшены', async () => {
      const {effects} = await expectSaga(rootSaga).silentRun(0);

      expect(effects.fork).toEqual([
        takeEvery(ActionTypes.INIT, init),
        takeLatest(ActionTypes.UPDATE_RANGE, updateRange),
        takeLatest(ActionTypes.EXTEND_RANGE, extendRange),
        takeLatest(ActionTypes.EXTEND_RANGE_UP, extendRangeUp),
        takeLatest(ActionTypes.EXTEND_RANGE_DOWN, extendRangeDown),
        takeLatest(ActionTypes.UPDATE_RANGE_WITH_SHOW_DATE, updateRangeWithShowDate),
        takeLatest(LayersActionTypes.TOGGLE_LAYER_SUCCESS, updateScheduleList),
        takeLatest(EventsActionTypes.GET_EVENTS_FOR_LAYER_SUCCESS, updateScheduleList),
        takeLatest(LOCATION_CHANGE, handleShowDateChange),
        takeLatest(EventsActionTypes.SET_ACTIVE_EVENT, handleSetActiveEvent)
      ]);
    });
  });
  describe('loadEvents', () => {
    test('должен вызывать поход за событиями для интервала времени', async () => {
      const start = 0;
      const end = 10;
      const getEventsDoneAction = getEventsDone({
        from: start,
        to: end
      });

      return expectSaga(loadEvents, {start, end})
        .provide([[select(getUserFromUrl), null]])
        .dispatch(getEventsDoneAction)
        .put(getEvents({from: start, to: end}))
        .run();
    });
    test('должен вызывать поход за чужими событиями для интервала времени', async () => {
      const start = 0;
      const end = 10;
      const getEventsDoneAction = getEventsDone({
        from: start,
        to: end
      });

      return expectSaga(loadEvents, {start, end})
        .provide([[select(getUserFromUrl), {login: 'login'}]])
        .dispatch(getEventsDoneAction)
        .put(getEventsByLogin({login: 'login', from: start, to: end, opaqueOnly: true}))
        .run();
    });
    test('должен дожидаться экшна GET_EVENTS_DONE', async () => {
      const start = 0;
      const end = 10;
      const eventsDonePattern = makeEventsDonePattern(start, end);
      const getEventsDoneAction = getEventsDone({
        from: start,
        to: end
      });

      return expectSaga(loadEvents, {start, end})
        .provide([
          [select(getUserFromUrl), null],
          [call.fn(makeEventsDonePattern), eventsDonePattern]
        ])
        .dispatch(getEventsDoneAction)
        .take(eventsDonePattern)
        .run();
    });
    test('должен логировать ошибку', async () => {
      const start = 0;
      const end = 10;
      return expectSaga(loadEvents, {start, end})
        .provide([[call.fn(makeEventsDonePattern), throwError({name: 'error'})]])
        .call([errorReporter, errorReporter.send], 'loadEvents', {name: 'error'})
        .run();
    });
  });
  describe('extendRange', () => {
    test('должен вызывать саги расширения интервала в обе стороны', () => {
      testSaga(extendRange)
        .next()
        .all([call(extendRangeUp), call(extendRangeDown)])
        .next()
        .isDone();
    });
    test('должен логировать ошибку', async () => {
      const error = {name: 'error'};

      return expectSaga(extendRange)
        .provide([[call(extendRangeUp)], [call.fn(extendRangeDown), throwError(error)]])
        .call([errorReporter, errorReporter.send], 'extendRange', error)
        .run();
    });
  });
  describe('extendRangeUp', () => {
    test('должен вызывать загрузку событий с расширением интервала вверх', async () => {
      const start = Number(new Date(2020, 0, 4));
      const step = 3;
      const expectedNewStart = Number(new Date(2020, 0, 1));
      const state = {isTopLoading: false, start, step};

      return expectSaga(extendRangeUp)
        .provide([[select(getScheduleRange), state], [call.fn(loadEvents), {}]])
        .call.fn(loadEvents)
        .put(actions.extendRangeUpDone(expectedNewStart))
        .run();
    });
    test('должен бросать экшн начала загрузки верхней части интервала', async () => {
      const start = Number(new Date(2020, 0, 4));
      const step = 3;
      const state = {isTopLoading: false, start, step};

      return expectSaga(extendRangeUp)
        .provide([[select(getScheduleRange), state], [call.fn(loadEvents), {}]])
        .put(actions.extendRangeUpStart())
        .run();
    });
    test('должен логировать ошибку', async () => {
      const start = Number(new Date(2020, 0, 4));
      const step = 3;
      const state = {isTopLoading: false, start, step};

      return expectSaga(extendRangeUp)
        .provide([
          [select(getScheduleRange), state],
          [call.fn(loadEvents), throwError({name: 'error'})]
        ])
        .call([errorReporter, errorReporter.send], 'extendRangeUp', {name: 'error'})
        .run();
    });
  });
  describe('extendRangeDown', () => {
    test('должен вызывать загрузку событий с расширением интервала вниз', async () => {
      const end = Number(new Date(2020, 0, 1));
      const step = 3;
      const expectedNewEnd = Number(new Date(2020, 0, 4));
      const state = {isBottomLoading: false, end, step};

      return expectSaga(extendRangeDown)
        .provide([[select(getScheduleRange), state], [call.fn(loadEvents), {}]])
        .call.fn(loadEvents)
        .put(actions.extendRangeDownDone(expectedNewEnd))
        .run();
    });
    test('должен бросать экшн начала загрузки нижней части интервала', async () => {
      const end = Number(new Date(2020, 0, 4));
      const step = 3;
      const state = {isBottomLoading: false, end, step};

      return expectSaga(extendRangeDown)
        .provide([[select(getScheduleRange), state], [call.fn(loadEvents), {}]])
        .put(actions.extendRangeDownStart())
        .run();
    });
    test('должен логировать ошибку', async () => {
      const end = Number(new Date(2020, 0, 4));
      const step = 3;
      const state = {isBottomLoading: false, end, step};

      return expectSaga(extendRangeDown)
        .provide([
          [select(getScheduleRange), state],
          [call.fn(loadEvents), throwError({name: 'error'})]
        ])
        .call([errorReporter, errorReporter.send], 'extendRangeDown', {name: 'error'})
        .run();
    });
  });
  describe('updateRange', () => {
    test('должен вызывать загрузку событий для заданного интервала', async () => {
      const start = Number(new Date(2020, 0, 1));
      const end = Number(new Date(2020, 0, 4));
      const state = {isLoading: false};

      return expectSaga(updateRange, {start, end})
        .provide([[select(getScheduleRange), state], [call.fn(loadEvents), {}]])
        .call.fn(loadEvents)
        .put(actions.updateRangeDone(start, end))
        .run();
    });
    test('должен вызывать загрузку событий для границ интервала из стейта, если не передали start и end', async () => {
      const start = Number(new Date(2020, 0, 1));
      const end = Number(new Date(2020, 0, 4));
      const state = {isLoading: false, start, end};

      return expectSaga(updateRange, {})
        .provide([[select(getScheduleRange), state], [call.fn(loadEvents), {}]])
        .put(actions.updateRangeDone(start, end))
        .run();
    });
    test('должен бросать экшн начала загрузки событий', async () => {
      const start = Number(new Date(2020, 0, 1));
      const end = Number(new Date(2020, 0, 4));
      const state = {isLoading: false};

      return expectSaga(updateRange, {start, end})
        .provide([[select(getScheduleRange), state], [call.fn(loadEvents), {}]])
        .put(actions.updateRangeStart())
        .run();
    });
    test('должен логировать ошибку', async () => {
      const start = Number(new Date(2020, 0, 1));
      const end = Number(new Date(2020, 0, 4));
      const state = {isLoading: false};

      return expectSaga(updateRange, {start, end})
        .provide([
          [select(getScheduleRange), state],
          [call.fn(loadEvents), throwError({name: 'error'})]
        ])
        .call([errorReporter, errorReporter.send], 'updateRange', {name: 'error'})
        .run();
    });
  });
  describe('handleShowDateChange', () => {
    describe('action: PUSH, pathname: /schedule', () => {
      const action = {
        type: LOCATION_CHANGE,
        payload: {
          action: 'PUSH',
          location: {
            search: 'show_date=2019-09-13',
            pathname: '/schedule'
          }
        }
      };

      test('должен синхронизировать showDate расписания', () => {
        return expectSaga(handleShowDateChange, action)
          .provide([
            [call.fn(getUserInfo)],
            [call.fn(syncShowDateWithLocation), {}],
            [call.fn(updateScheduleList), {}],
            [call.fn(handleShowDateChangeOnEmptyState), {}]
          ])
          .call(syncShowDateWithLocation, action.payload.location)
          .run();
      });
      test('должен вызвать сагу, отвечающую за перемещение расписания к нужному дню', () => {
        return expectSaga(handleShowDateChange, action)
          .provide([
            [call.fn(getUserInfo)],
            [call.fn(syncShowDateWithLocation), {}],
            [call.fn(updateScheduleList), {}],
            [call.fn(handleShowDateChangeOnEmptyState), {}]
          ])
          .call(updateScheduleList)
          .run();
      });
      test('должен вызвать сагу, отвечающую за поведение при emptyState', () => {
        return expectSaga(handleShowDateChange, action)
          .provide([
            [call.fn(getUserInfo)],
            [call.fn(syncShowDateWithLocation), {}],
            [call.fn(updateScheduleList), {}],
            [call.fn(handleShowDateChangeOnEmptyState), {}]
          ])
          .call(handleShowDateChangeOnEmptyState)
          .run();
      });
      test('должен логировать ошибку', () => {
        return expectSaga(handleShowDateChange, action)
          .provide([[call.fn(syncShowDateWithLocation), throwError({name: 'error'})]])
          .call([errorReporter, errorReporter.send], 'handleShowDateChange', {name: 'error'})
          .run();
      });
    });

    test('не должен ничего делать, если LOCATION_CHANGE вызван расписанием', async () => {
      const action = {
        type: LOCATION_CHANGE,
        payload: {
          action: 'REPLACE',
          location: {
            search: 'show_date=2019-09-13',
            pathname: '/schedule',
            schedule: true
          }
        }
      };

      const {effects} = await expectSaga(handleShowDateChange, action).silentRun(0);
      expect(Object.keys(effects)).toHaveLength(0);
    });
  });

  describe('handleShowDateChangeOnEmptyState', () => {
    test('должен вызвать updateRangeWithShowDate, если нет событий и события не загружаются', () => {
      const state = {isLoading: false};

      return expectSaga(handleShowDateChangeOnEmptyState)
        .provide([
          [select(getIsInitialLoading), false],
          [select(getScheduleRange), state],
          [select(getEventsCount), 0]
        ])
        .put(actions.updateRangeWithShowDate())
        .run();
    });
    test('не должен вызвать updateRangeWithShowDate, если есть хоть одно событие', () => {
      const state = {isLoading: false};

      return expectSaga(handleShowDateChangeOnEmptyState)
        .provide([
          [select(getIsInitialLoading), false],
          [select(getScheduleRange), state],
          [select(getEventsCount), 1]
        ])
        .not.put(actions.updateRangeWithShowDate())
        .run();
    });
    test('не должен вызвать updateRangeWithShowDate, если уже идет загрузка', () => {
      const state = {isLoading: true};

      return expectSaga(handleShowDateChangeOnEmptyState)
        .provide([
          [select(getIsInitialLoading), false],
          [select(getScheduleRange), state],
          [select(getEventsCount), 0]
        ])
        .not.put(actions.updateRangeWithShowDate())
        .run();
    });
    test('не должен делать ничего, если расписание еще загружается', async () => {
      const {effects} = await expectSaga(handleShowDateChangeOnEmptyState)
        .provide([[select(getIsInitialLoading), true]])
        .silentRun(0);
      expect(Object.keys(effects)).toHaveLength(1);
    });
  });
  describe('syncShowDateWithLocation', () => {
    test('должен бросать экшн SET_SHOW_DATE с датой из location', () => {
      const location = {
        search: 'show_date=2019-09-13'
      };
      const date = 1568322000000;

      return expectSaga(syncShowDateWithLocation, location)
        .put(actions.setShowDate(date))
        .run();
    });
  });
  describe('updateScheduleList', () => {
    test('должен бросить экшен UPDATE_RANGE_WITH_SHOW_DATE, если день не найден', () => {
      return expectSaga(updateScheduleList)
        .provide([
          [select(getIsInitialLoading), false],
          [select(getShowDate), 86400 * 2],
          [select(getScheduleEvents), [{start: 0}, {start: 86400}]]
        ])
        .put(actions.updateRangeWithShowDate())
        .run();
    });
    test('не должен делать ничего, если расписание еще загружается', async () => {
      const {effects} = await expectSaga(updateScheduleList)
        .provide([[select(getIsInitialLoading), true]])
        .silentRun(0);
      expect(Object.keys(effects)).toHaveLength(1);
    });
  });
});
