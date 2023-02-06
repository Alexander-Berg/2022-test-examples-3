import {expectSaga, testSaga} from 'redux-saga-test-plan';
import {call, select, fork, take, getContext} from 'redux-saga-test-plan/matchers';
import {throwError} from 'redux-saga-test-plan/providers';
import {replace} from 'connected-react-router';
import {delay} from 'redux-saga';

import splashScreen from 'utils/splashScreen';
import metrika from 'utils/metrika';
import SagaErrorReporter from 'utils/SagaErrorReporter';
import mobileAppManager, {fatalError, cacheReady} from 'utils/applicationInterface';
import {getTimezones} from 'features/timezones/timezonesActions';
import * as eventsActions from 'features/events/eventsActions';
import {handleLayerInvitation} from 'features/layers/layersActions';
import {ensureSettingsLoaded} from 'features/settings/settingsActions';
import {getRouterLocation} from 'features/router/routerSelectors';
import {getCurrentUser} from 'features/settings/settingsSelectors';
import * as environment from 'configs/environment';

import AppApi from '../AppApi';
import {
  handleAppMount,
  removeInvalidDateParamFromUrl,
  removeTouchParamFromURL,
  registerBackButtonHandler,
  unregisterBackButtonHandler,
  watchNetworkStatus,
  offlineDataUpdate,
  runFeatureOfflineDataUpdate,
  createSubscription
} from '../appSagas';

const errorReporter = new SagaErrorReporter('app');
const applicationInterfaceSetup = require('../../../shared/index/helpers/application-interface');

describe('appSagas', () => {
  beforeEach(() => {
    applicationInterfaceSetup(window, {appType: 'android'});
  });

  describe('handleAppMount', () => {
    describe('успешное выполнение', () => {
      const defaultProviders = [
        [call.fn(metrika.send)],
        [call.fn(splashScreen.remove)],
        [call.fn(removeTouchParamFromURL)]
      ];

      test('должен отправлять метрику о типе пользователя', () => {
        return expectSaga(handleAppMount)
          .provide(defaultProviders)
          .call([metrika, metrika.send], ['Тип пользователя', 'обычный'])
          .run();
      });

      test('должен удалять сплеш экран', () => {
        return expectSaga(handleAppMount)
          .provide(defaultProviders)
          .call([splashScreen, splashScreen.remove])
          .run();
      });

      test('должен запрашивать таймзоны', () => {
        return expectSaga(handleAppMount)
          .provide(defaultProviders)
          .put(getTimezones())
          .run();
      });

      test('должен обрабатывать приглашение на встречу', () => {
        return expectSaga(handleAppMount)
          .provide(defaultProviders)
          .put(eventsActions.handleEventInvitation())
          .run();
      });

      test('должен обрабатывать приглашение в слой', () => {
        return expectSaga(handleAppMount)
          .provide(defaultProviders)
          .put(handleLayerInvitation())
          .run();
      });

      test('должен обрабатывать ошибку при загрузке настроек', () => {
        return expectSaga(handleAppMount)
          .provide(defaultProviders)
          .put(ensureSettingsLoaded())
          .run();
      });

      test('должен мониторить изменение статуса сети, если это оффлайн календарь', function() {
        sinon.stub(environment, 'isStaticMobileApp').value(true);

        return expectSaga(handleAppMount)
          .provide(defaultProviders.concat([[fork.fn(watchNetworkStatus)]]))
          .fork(watchNetworkStatus)
          .run();
      });

      test('должен отправлять метрику об открытии в браузере для тача', () => {
        sinon.stub(environment, 'isTouch').value(true);

        return expectSaga(handleAppMount)
          .provide(defaultProviders)
          .call([metrika, metrika.send], ['Платформа', 'Браузер'])
          .run();
      });

      test('должен отправлять в метрику платформу WebView', () => {
        sinon.stub(environment, 'isTouch').value(true);
        sinon.stub(environment, 'isMobileApp').value(true);
        sinon.stub(environment, 'appType').value('android');

        return expectSaga(handleAppMount)
          .provide(defaultProviders)
          .call([metrika, metrika.send], ['Платформа', 'Мобильное приложение', 'android'])
          .run();
      });
    });

    describe('неуспешное выполнение', () => {
      test('должен логировать ошибку', () => {
        return expectSaga(handleAppMount)
          .provide([
            [call.fn(metrika.send), throwError({name: 'error'})],
            [call.fn(errorReporter.send)]
          ])
          .call([errorReporter, errorReporter.send], 'handleAppMount', {name: 'error'})
          .run();
      });
    });
  });

  describe('removeTouchParamFromURL', () => {
    describe('успешное выполнение', () => {
      test('должен удалять параметр touch из URL', () => {
        return expectSaga(removeTouchParamFromURL)
          .provide([[select(getRouterLocation), {pathname: '/week', search: '?touch=1&test=1'}]])
          .put(
            replace({
              pathname: '/week',
              search: 'test=1'
            })
          )
          .run();
      });
    });

    describe('неуспешное выполнение', () => {
      test('должен логировать ошибку', () => {
        return expectSaga(removeTouchParamFromURL)
          .provide([
            [select(getRouterLocation), throwError({name: 'error'})],
            [call.fn(errorReporter.send)]
          ])
          .call([errorReporter, errorReporter.send], 'removeTouchParamFromURL', {name: 'error'})
          .run();
      });
    });
  });

  describe('removeInvalidDateParamFromUrl', () => {
    describe('успешное выполнение', () => {
      test('должен удалять параметр show_date из URL', () => {
        return expectSaga(removeInvalidDateParamFromUrl)
          .provide([
            [select(getRouterLocation), {pathname: '/week', search: '?show_date=2019-22-22&test=1'}]
          ])
          .put(
            replace({
              pathname: '/week',
              search: 'test=1'
            })
          )
          .run();
      });
    });

    describe('неуспешное выполнение', () => {
      test('должен логировать ошибку', () => {
        return expectSaga(removeInvalidDateParamFromUrl)
          .provide([
            [select(getRouterLocation), throwError({name: 'error'})],
            [call.fn(errorReporter.send)]
          ])
          .call([errorReporter, errorReporter.send], 'removeInvalidDateParamFromUrl', {
            name: 'error'
          })
          .run();
      });
    });
  });

  describe('createSubscription', () => {
    test('успешное выполнение', () => {
      const payload = {a: 1};
      const appApi = new AppApi();

      return expectSaga(createSubscription, {payload})
        .provide([
          [select(getCurrentUser), {uid: 123}],
          [getContext('appApi'), appApi],
          [call.fn(appApi.createSubscription), {}]
        ])
        .call.fn(appApi.createSubscription)
        .run();
    });
  });

  describe('registerBackButtonHandler', () => {
    test('должен регистрировать обработчик в mobileAppManager', () => {
      const handler = () => {};

      return expectSaga(registerBackButtonHandler, {handler})
        .call([mobileAppManager, mobileAppManager.registerBackButtonHandler], handler)
        .run();
    });
  });

  describe('unregisterBackButtonHandler', () => {
    test('должен снимать с регистрации обработчик в mobileAppManager', () => {
      const handler = () => {};

      return expectSaga(unregisterBackButtonHandler, {handler})
        .call([mobileAppManager, mobileAppManager.unregisterBackButtonHandler], handler)
        .run();
    });
  });

  describe('runFeatureOfflineDataUpdate', () => {
    test('должен бросать updateOfflineData, дожидаясь updateOfflineDataSuccess или updateOfflineDataFailure', () => {
      const updateStartAction = Symbol();
      const successType = 'successType';
      const failureType = 'failureType';
      const actions = {
        updateOfflineData: () => updateStartAction,
        updateOfflineDataSuccess: {
          type: successType
        },
        updateOfflineDataFailure: {
          type: failureType
        }
      };
      const expectedResult = {success: actions.updateOfflineDataSuccess};

      testSaga(runFeatureOfflineDataUpdate, actions)
        .next()
        .put(updateStartAction)
        .next()
        .race({
          success: take(successType),
          failure: take(failureType)
        })
        .next(expectedResult)
        .returns(expectedResult);
    });
  });

  describe('offlineDataUpdate', () => {
    test('должен выполнять обновление офлайн-данных', () => {
      testSaga(offlineDataUpdate)
        .next()
        .call(runFeatureOfflineDataUpdate, eventsActions)
        .next()
        .call(delay, 1000)
        .next()
        .call(cacheReady)
        .next()
        .isDone();
    });

    describe('неуспешное выполнение', () => {
      test('должен логировать ошибку', () => {
        const error = Symbol();

        return expectSaga(offlineDataUpdate)
          .provide([
            [call.fn(runFeatureOfflineDataUpdate), throwError(error)],
            [call.fn(errorReporter.send)]
          ])
          .call([errorReporter, errorReporter.send], 'offlineDataUpdate', error)
          .run();
      });

      test('должен бросать сигнал fatalError', () => {
        const error = Symbol();

        return expectSaga(offlineDataUpdate)
          .provide([
            [call.fn(runFeatureOfflineDataUpdate), throwError(error)],
            [call.fn(fatalError)],
            [call.fn(errorReporter.send)]
          ])
          .call(fatalError)
          .run();
      });
    });
  });
});
