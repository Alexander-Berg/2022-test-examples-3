import {take} from 'redux-saga/effects';
import {expectSaga} from 'redux-saga-test-plan';
import {call, getContext} from 'redux-saga-test-plan/matchers';
import {throwError} from 'redux-saga-test-plan/providers';

import SagaErrorReporter from 'utils/SagaErrorReporter';
import EventFormApi from 'features/eventForm/EventFormApi';

import {ActionTypes} from '../suggestMeetingTimesConstants';
import SuggestMeetingTimesApi from '../SuggestMeetingTimesApi';
import rootSaga, {getMeetingTimes, checkResourcesAvailability} from '../suggestMeetingTimesSagas';

const errorReporter = new SagaErrorReporter('suggestMeetingTimes');

describe('suggestMeetingTimesSagas', () => {
  describe('rootSaga', () => {
    test('должен записывать SuggestMeetingTimesApi в контекст', () => {
      return expectSaga(rootSaga)
        .provide([[getContext('api'), {}]])
        .setContext({
          suggestMeetingTimesApi: new SuggestMeetingTimesApi({}),
          eventFormApi: new EventFormApi({})
        })
        .silentRun(0);
    });

    test('должен подписываться на экшены', async () => {
      const {effects} = await expectSaga(rootSaga)
        .provide([[getContext('api'), {}]])
        .silentRun(0);

      expect(effects.take).toEqual([
        take(ActionTypes.GET_MEETING_TIMES),
        take(ActionTypes.CHECK_RESOURCES_AVAILABILITY)
      ]);
    });
  });

  describe('getMeetingTimes', () => {
    describe('успешное выполнение', () => {
      const suggestMeetingTimesApi = new SuggestMeetingTimesApi();
      const action = {
        payload: {},
        resolve() {}
      };
      const defaultProviders = [
        [getContext('suggestMeetingTimesApi'), suggestMeetingTimesApi],
        [call.fn(suggestMeetingTimesApi.suggestMeetingTimes), {}]
      ];

      test('должен делать запрос за местами и временем встречи', () => {
        return expectSaga(getMeetingTimes, action)
          .provide(defaultProviders)
          .call(
            [suggestMeetingTimesApi, suggestMeetingTimesApi.suggestMeetingTimes],
            action.payload
          )
          .run();
      });

      test('должен вызывать resolve с полученными данными', () => {
        return expectSaga(getMeetingTimes, action)
          .provide(defaultProviders)
          .call(action.resolve, {})
          .run();
      });
    });

    describe('неуспешное выполнение', () => {
      const action = {
        payload: {},
        resolve() {}
      };
      const defaultProviders = [
        [getContext('suggestMeetingTimesApi'), throwError({name: 'error'})],
        [call.fn(errorReporter.send)]
      ];

      test('должен логировать ошибку', () => {
        return expectSaga(getMeetingTimes, action)
          .provide(defaultProviders)
          .call([errorReporter, errorReporter.send], 'getMeetingTimes', {name: 'error'})
          .run();
      });
    });
  });

  describe('checkResourcesAvailability', () => {
    describe('успешное выполнение', () => {
      const eventFormApi = new EventFormApi();
      const action = {
        payload: {},
        resolve() {}
      };
      const defaultProviders = [
        [getContext('eventFormApi'), eventFormApi],
        [call.fn(eventFormApi.getAvailabilities), {}]
      ];

      test('должен делать запрос за занятостью переговорок', () => {
        return expectSaga(checkResourcesAvailability, action)
          .provide(defaultProviders)
          .call([eventFormApi, eventFormApi.getAvailabilities], action.payload)
          .run();
      });

      test('должен вызывать resolve с полученными данными', () => {
        return expectSaga(checkResourcesAvailability, action)
          .provide(defaultProviders)
          .call(action.resolve, {})
          .run();
      });
    });

    describe('неуспешное выполнение', () => {
      const action = {
        payload: {},
        resolve() {}
      };
      const defaultProviders = [
        [getContext('eventFormApi'), throwError({name: 'error'})],
        [call.fn(errorReporter.send)]
      ];

      test('должен логировать ошибку', () => {
        return expectSaga(checkResourcesAvailability, action)
          .provide(defaultProviders)
          .call([errorReporter, errorReporter.send], 'checkResourcesAvailability', {name: 'error'})
          .run();
      });
    });
  });
});
