import {take} from 'redux-saga/effects';
import {expectSaga} from 'redux-saga-test-plan';
import {call, getContext} from 'redux-saga-test-plan/matchers';
import {throwError} from 'redux-saga-test-plan/providers';

import SagaErrorReporter from 'utils/SagaErrorReporter';

import {ActionTypes} from '../timelineConstants';
import TimelineApi from '../TimelineApi';
import rootSaga, {getEventsInfo, getAvailabilityIntervals} from '../timelineSagas';

const errorReporter = new SagaErrorReporter('timeline');

describe('timelineSagas', () => {
  describe('rootSaga', () => {
    test('должен записывать TimelineApi в контекст', () => {
      return expectSaga(rootSaga)
        .provide([[getContext('api'), {}]])
        .setContext({timelineApi: new TimelineApi({})})
        .silentRun(0);
    });

    test('должен подписываться на экшены', async () => {
      const {effects} = await expectSaga(rootSaga)
        .provide([[getContext('api'), {}]])
        .silentRun(0);

      expect(effects.take).toEqual([
        take(ActionTypes.GET_EVENTS_INFO),
        take(ActionTypes.GET_AVAILABILITY_INTERVALS)
      ]);
    });
  });

  describe('getEventsInfo', () => {
    describe('успешное выполнение', () => {
      const timelineApi = new TimelineApi();
      const action = {
        payload: {
          eventIds: [1, 2],
          forResource: false
        },
        resolve() {}
      };
      const defaultProviders = [
        [getContext('timelineApi'), timelineApi],
        [call.fn(timelineApi.getEventsBrief), {events: []}]
      ];

      test('должен делать запрос за событиями', () => {
        return expectSaga(getEventsInfo, action)
          .provide(defaultProviders)
          .call([timelineApi, timelineApi.getEventsBrief], action.payload)
          .run();
      });

      test('должен вызывать resolve с полученными событиями', () => {
        return expectSaga(getEventsInfo, action)
          .provide(defaultProviders)
          .call(action.resolve, [])
          .run();
      });
    });

    describe('неуспешное выполнение', () => {
      const action = {
        payload: {
          eventIds: [1, 2],
          forResource: false
        },
        resolve() {}
      };
      const defaultProviders = [
        [getContext('timelineApi'), throwError({name: 'error'})],
        [call.fn(errorReporter.send)]
      ];

      test('должен логировать ошибку', () => {
        return expectSaga(getEventsInfo, action)
          .provide(defaultProviders)
          .call([errorReporter, errorReporter.send], 'getEventsInfo', {name: 'error'})
          .run();
      });
    });
  });

  describe('getAvailabilityIntervals', () => {
    describe('успешное выполнение', () => {
      const timelineApi = new TimelineApi();
      const action = {
        payload: {
          date: '2018-01-01',
          emails: ['test@ya.ru'],
          shape: 'ids-only',
          exceptEventId: 1
        },
        resolve() {}
      };
      const defaultProviders = [
        [getContext('timelineApi'), timelineApi],
        [call.fn(timelineApi.getAvailabilityIntervals), []]
      ];

      test('должен делать запрос за интервалами занятости', () => {
        return expectSaga(getAvailabilityIntervals, action)
          .provide(defaultProviders)
          .call([timelineApi, timelineApi.getAvailabilityIntervals], action.payload)
          .run();
      });

      test('должен вызывать resolve с полученными интервалами занятости', () => {
        return expectSaga(getAvailabilityIntervals, action)
          .provide(defaultProviders)
          .call(action.resolve, [])
          .run();
      });
    });

    describe('неуспешное выполнение', () => {
      const action = {
        payload: {
          date: '2018-01-01',
          emails: ['test@ya.ru'],
          shape: 'ids-only',
          exceptEventId: 1
        },
        resolve() {}
      };
      const defaultProviders = [
        [getContext('timelineApi'), throwError({name: 'error'})],
        [call.fn(errorReporter.send)]
      ];

      test('должен логировать ошибку', () => {
        return expectSaga(getAvailabilityIntervals, action)
          .provide(defaultProviders)
          .call([errorReporter, errorReporter.send], 'getAvailabilityIntervals', {name: 'error'})
          .run();
      });
    });
  });
});
