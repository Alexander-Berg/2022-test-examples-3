import {take} from 'redux-saga/effects';
import {expectSaga} from 'redux-saga-test-plan';
import {call, getContext} from 'redux-saga-test-plan/matchers';
import {throwError} from 'redux-saga-test-plan/providers';

import SagaErrorReporter from 'utils/SagaErrorReporter';

import TimezonesApi from '../TimezonesApi';
import {ActionTypes} from '../timezonesConstants';
import rootSaga, {getTimezones} from '../timezonesSagas';
import * as actions from '../timezonesActions';

const errorReporter = new SagaErrorReporter('timezones');

describe('timezonesSagas', () => {
  describe('rootSaga', () => {
    test('должен записывать TimezonesApi в контекст', () => {
      return expectSaga(rootSaga)
        .provide([[getContext('api'), {}]])
        .setContext({timezonesApi: new TimezonesApi({})})
        .silentRun(0);
    });

    test('должен подписываться на экшены', async () => {
      const {effects} = await expectSaga(rootSaga)
        .provide([[getContext('api'), {}]])
        .silentRun(0);

      expect(effects.take).toEqual([take(ActionTypes.GET_TIMEZONES)]);
    });
  });

  describe('getTimezones', () => {
    describe('успешное выполнение', () => {
      const timezonesApi = new TimezonesApi();
      const defaultProviders = [
        [getContext('timezonesApi'), timezonesApi],
        [call.fn(timezonesApi.getTimezones), {}]
      ];

      test('должен делать запрос за таймзонами', () => {
        return expectSaga(getTimezones)
          .provide(defaultProviders)
          .call([timezonesApi, timezonesApi.getTimezones])
          .run();
      });

      test('должен записывать в стейт полученные данные', () => {
        return expectSaga(getTimezones)
          .provide(defaultProviders)
          .put(actions.getTimezonesSuccess({}))
          .run();
      });
    });

    describe('неуспешное выполнение', () => {
      const defaultProviders = [
        [getContext('timezonesApi'), throwError({name: 'error'})],
        [call.fn(errorReporter.send)]
      ];

      test('должен логировать ошибку', () => {
        return expectSaga(getTimezones)
          .provide(defaultProviders)
          .call([errorReporter, errorReporter.send], 'getTimezones', {name: 'error'})
          .run();
      });
    });
  });
});
