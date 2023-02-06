import {expectSaga, testSaga} from 'redux-saga-test-plan';
import {call, take, getContext, select} from 'redux-saga-test-plan/matchers';
import {throwError} from 'redux-saga-test-plan/providers';

import {uid} from 'configs/session';
import commonPersistor from 'persist/commonPersistor.js';
import pageParams from 'utils/pageParams';
import SagaErrorReporter from 'utils/SagaErrorReporter';
import {offlineSelector} from 'features/appStatus/appStatusSelectors';
import {isEmbed} from 'features/embed/embedSelectors';

import {createChannel, authCheck, addUidToURL} from '../authSagas';
import AuthApi from '../AuthApi';

const errorReporter = new SagaErrorReporter('auth');

describe('authSagas', () => {
  describe('authCheck', () => {
    describe('успешное выполнение', () => {
      const channel = 'channel';
      const authApi = new AuthApi();
      const defaultProviders = [
        [call(createChannel), channel],
        [select(offlineSelector), false],
        [call.fn(commonPersistor.flushFormData)],
        [getContext('authApi'), authApi],
        [call.fn(authApi.check)],
        [select(isEmbed), false]
      ];

      describe('вкладка стала активной', () => {
        test('должен выгружать данные в localStorage', () => {
          return expectSaga(authCheck)
            .provide([[take(channel), false], ...defaultProviders])
            .call([commonPersistor, commonPersistor.flushFormData])
            .silentRun();
        });

        test('должен отправлять запрос на проверку авторизации', () => {
          return expectSaga(authCheck)
            .provide([[take(channel), false], ...defaultProviders])
            .call([authApi, authApi.check])
            .silentRun();
        });
      });

      describe('вкладка стала неактивной', () => {
        test('не должен выгружать данные в localStorage', () => {
          return expectSaga(authCheck)
            .provide([[take(channel), true], ...defaultProviders])
            .not.call([commonPersistor, commonPersistor.flushFormData])
            .silentRun();
        });

        test('не должен отправлять запрос на проверку авторизации', () => {
          return expectSaga(authCheck)
            .provide([[take(channel), true], ...defaultProviders])
            .not.call([authApi, authApi.check])
            .silentRun();
        });
      });
    });

    describe('неуспешное выполнение', () => {
      test('не должен ничего делать в embed', () => {
        testSaga(authCheck)
          .next()
          .select(isEmbed)
          .next(true)
          .isDone();
      });

      test('должен логировать ошибку', () => {
        return expectSaga(authCheck)
          .provide([
            [call(createChannel), throwError({name: 'error'})],
            [call.fn(errorReporter.send)],
            [select(isEmbed), false]
          ])
          .call([errorReporter, errorReporter.send], 'authCheck', {name: 'error'})
          .run();
      });
    });
  });

  describe('addUidToURL', () => {
    describe('успешное выполнение', () => {
      test('должен добавлять uid в url', () => {
        return expectSaga(addUidToURL)
          .provide([[call.fn(pageParams.update)]])
          .call([pageParams, pageParams.update], {uid})
          .run();
      });
    });

    describe('неуспешное выполнение', () => {
      test('должен логировать ошибку', () => {
        return expectSaga(addUidToURL)
          .provide([
            [call.fn(pageParams.update), throwError({name: 'error'})],
            [call.fn(errorReporter.send)]
          ])
          .call([errorReporter, errorReporter.send], 'addUidToURL', {name: 'error'})
          .run();
      });
    });
  });
});
