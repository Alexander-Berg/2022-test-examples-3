import {delay} from 'redux-saga';
import {all, takeEvery, setContext} from 'redux-saga/effects';
import {expectSaga, testSaga} from 'redux-saga-test-plan';
import * as matchers from 'redux-saga-test-plan/matchers';
import {throwError} from 'redux-saga-test-plan/providers';
import moment from 'moment';

import i18n from 'utils/i18n';
import SagaErrorReporter from 'utils/SagaErrorReporter';
import {
  notificationHelpers,
  notifyFailure,
  notifyWarning,
  notifySuccess
} from 'features/notifications/notificationsActions';

import SettingsApi from '../SettingsApi';
import {ActionTypes} from '../settingsConstants';
import {getSettings} from '../settingsSelectors';
import rootSaga, {
  updateSettingsNetwork,
  reloadPage,
  ensureSettingsLoaded,
  updateSettingsOffline
} from '../settingsSagas';
import SettingsRecord from '../SettingsRecord';
import * as settingsActions from '../settingsActions';

jest.mock('utils/i18n');

const errorReporter = new SagaErrorReporter('settings');

describe('settingsSagas', () => {
  beforeEach(() => {
    jest.spyOn(notificationHelpers, 'generateId').mockReturnValue('id');
  });

  describe('rootSaga', () => {
    const gen = rootSaga();

    test('должен записать settingsApi в контекст', () => {
      gen.next();
      expect(gen.next().value).toEqual(setContext({settingsApi: new SettingsApi()}));
    });
    test('должен подписаться на экшены', () => {
      expect(gen.next().value).toEqual(
        all([
          takeEvery(settingsActions.updateSettingsNetwork.type, updateSettingsNetwork),
          takeEvery(settingsActions.updateSettingsOffline.type, updateSettingsOffline),
          takeEvery(ActionTypes.ENSURE_SETTINGS_LOADED, ensureSettingsLoaded)
        ])
      );
    });
    test('должен завершить сагу', () => {
      expect(gen.next().done).toBe(true);
    });
  });

  describe('updateSettingsNetwork', () => {
    test('common success way', () => {
      const action = {
        payload: {
          values: {
            showWeekNumber: false
          },
          resolveForm() {},
          rejectForm() {}
        }
      };
      const settings = new SettingsRecord();
      const api = new SettingsApi();

      testSaga(updateSettingsNetwork, action)
        .next()
        .select(getSettings)
        .next(settings)
        .getContext('settingsApi')
        .next(api)
        .call([api, api.update], action.payload.values)
        .next(action.payload.values)
        .put(
          settingsActions.updateSettingsSuccess({
            newSettings: action.payload.values,
            oldSettings: settings
          })
        )
        .next()
        .call(action.payload.resolveForm)
        .next()
        .isDone();
    });

    test('success way, optimistic update', () => {
      const action = {
        payload: {
          values: {
            showWeekNumber: false
          },
          resolveForm() {},
          rejectForm() {},
          options: {
            optimisticUpdate: true
          }
        }
      };
      const settings = new SettingsRecord();
      const api = new SettingsApi();

      testSaga(updateSettingsNetwork, action)
        .next()
        .select(getSettings)
        .next(settings)
        .put(
          settingsActions.updateSettingsSuccess({
            newSettings: action.payload.values,
            oldSettings: settings
          })
        )
        .next()
        .getContext('settingsApi')
        .next(api)
        .call([api, api.update], action.payload.values)
        .next(action.payload.values)
        .call(action.payload.resolveForm)
        .next()
        .isDone();
    });

    test('success way and notifying', () => {
      const action = {
        payload: {
          values: {
            showWeekNumber: false
          },
          options: {
            notify: true
          },
          resolveForm() {},
          rejectForm() {}
        }
      };
      const settings = new SettingsRecord();
      const api = new SettingsApi();

      testSaga(updateSettingsNetwork, action)
        .next()
        .select(getSettings)
        .next(settings)
        .getContext('settingsApi')
        .next(api)
        .call([api, api.update], action.payload.values)
        .next(action.payload.values)
        .put(
          settingsActions.updateSettingsSuccess({
            newSettings: action.payload.values,
            oldSettings: settings
          })
        )
        .next()
        .put(
          notifySuccess({
            message: 'notifications.changesSaved'
          })
        )
        .next()
        .call(action.payload.resolveForm)
        .next()
        .isDone();
    });

    test('success way and updating tz', () => {
      const action = {
        payload: {
          values: {
            tz: 'Europe/Minsk'
          },
          resolveForm() {},
          rejectForm() {}
        }
      };
      const settings = new SettingsRecord({tz: 'Europe/Moscow'});
      const api = new SettingsApi();

      testSaga(updateSettingsNetwork, action)
        .next()
        .select(getSettings)
        .next(settings)
        .getContext('settingsApi')
        .next(api)
        .call([api, api.update], action.payload.values)
        .next(action.payload.values)
        .put(
          settingsActions.updateSettingsSuccess({
            newSettings: action.payload.values,
            oldSettings: settings
          })
        )
        .next()
        .call(action.payload.resolveForm)
        .next()
        .call(delay, 500)
        .next()
        .call(reloadPage)
        .next()
        .isDone();
    });

    test('success way and updating weekStartDay', () => {
      const action = {
        payload: {
          values: {
            weekStartDay: 2
          },
          resolveForm() {},
          rejectForm() {}
        }
      };
      const settings = new SettingsRecord({weekStartDay: 1});
      const api = new SettingsApi();

      testSaga(updateSettingsNetwork, action)
        .next()
        .select(getSettings)
        .next(settings)
        .getContext('settingsApi')
        .next(api)
        .call([api, api.update], action.payload.values)
        .next(action.payload.values)
        .call([moment, 'updateLocale'], 'ru', {
          week: {
            dow: action.payload.values.weekStartDay
          }
        })
        .next()
        .put(
          settingsActions.updateSettingsSuccess({
            newSettings: action.payload.values,
            oldSettings: settings
          })
        )
        .next()
        .call(action.payload.resolveForm)
        .next()
        .isDone();
    });

    test('common failure way', () => {
      const action = {
        payload: {
          values: {
            showWeekNumber: false
          },
          resolveForm() {},
          rejectForm() {}
        }
      };
      const error = {name: 'error'};
      const settings = new SettingsRecord({weekStartDay: 1});
      const api = new SettingsApi();

      testSaga(updateSettingsNetwork, action)
        .next()
        .select(getSettings)
        .next(settings)
        .getContext('settingsApi')
        .next(api)
        .throw(error)
        .call([errorReporter, errorReporter.send], 'updateSettingsNetwork', error)
        .next()
        .call(action.payload.rejectForm)
        .next()
        .isDone();
    });

    test('failure way and notifying', () => {
      const action = {
        payload: {
          values: {
            showWeekNumber: false
          },
          options: {
            notify: true
          },
          resolveForm() {},
          rejectForm() {}
        }
      };
      const error = {name: 'error'};
      const settings = new SettingsRecord();
      const api = new SettingsApi();

      testSaga(updateSettingsNetwork, action)
        .next()
        .select(getSettings)
        .next(settings)
        .getContext('settingsApi')
        .next(api)
        .throw(error)
        .call([errorReporter, errorReporter.send], 'updateSettingsNetwork', error)
        .next()
        .put(notifyFailure({error}))
        .next()
        .call(action.payload.rejectForm)
        .next()
        .isDone();
    });

    test('failure way, optimistic update, rollback', () => {
      const action = {
        payload: {
          values: {
            showWeekNumber: false
          },
          resolveForm() {},
          rejectForm() {},
          options: {
            optimisticUpdate: true
          }
        }
      };
      const error = {name: 'error'};
      const settings = new SettingsRecord({weekStartDay: 1});
      const api = new SettingsApi();

      testSaga(updateSettingsNetwork, action)
        .next()
        .select(getSettings)
        .next(settings)
        .put(
          settingsActions.updateSettingsSuccess({
            newSettings: action.payload.values,
            oldSettings: settings
          })
        )
        .next()
        .getContext('settingsApi')
        .next(api)
        .throw(error)
        .call([errorReporter, errorReporter.send], 'updateSettingsNetwork', error)
        .next()
        .put(
          settingsActions.updateSettingsSuccess({
            newSettings: settings,
            oldSettings: action.payload.values
          })
        )
        .next()
        .call(action.payload.rejectForm)
        .next()
        .isDone();
    });
  });

  describe('updateSettingsOffline', () => {
    test('без оптимистичного обновления', () => {
      const action = {
        payload: {
          values: {
            showWeekNumber: false
          },
          resolveForm() {},
          rejectForm() {}
        }
      };
      const settings = new SettingsRecord();

      testSaga(updateSettingsOffline, action)
        .next()
        .select(getSettings)
        .next(settings)
        .call(action.payload.resolveForm)
        .next()
        .isDone();
    });

    test('c оптимистичным обновлением', () => {
      const action = {
        payload: {
          values: {
            showWeekNumber: false
          },
          resolveForm() {},
          rejectForm() {},
          options: {
            optimisticUpdate: true
          }
        }
      };
      const settings = new SettingsRecord();

      testSaga(updateSettingsOffline, action)
        .next()
        .select(getSettings)
        .next(settings)
        .put(
          settingsActions.updateSettingsSuccess({
            newSettings: action.payload.values,
            oldSettings: settings
          })
        )
        .next()
        .call(action.payload.resolveForm)
        .next()
        .isDone();
    });
  });

  describe('ensureSettingsLoaded', () => {
    describe('успешное выполнение', () => {
      test('должен показывать оповещение, если настройки не загрузились', () => {
        return expectSaga(ensureSettingsLoaded)
          .provide([[matchers.select(getSettings), {_failedToLoad: true}]])
          .put(notifyWarning({message: i18n.get('errors', 'failedToLoadSettings')}))
          .run();
      });

      test('не должен показывать оповещение, если настройки загрузились', () => {
        return expectSaga(ensureSettingsLoaded)
          .provide([[matchers.select(getSettings), {_failedToLoad: false}]])
          .not.put(notifyWarning().type)
          .run();
      });
    });

    describe('неуспешное выполнение', () => {
      test('должен логировать ошибку', () => {
        return expectSaga(ensureSettingsLoaded)
          .provide([
            [matchers.select(getSettings), throwError({name: 'error'})],
            [matchers.call.fn(errorReporter.send)]
          ])
          .call([errorReporter, errorReporter.send], 'ensureSettingsLoaded', {name: 'error'})
          .run();
      });
    });
  });
});
