import {expectSaga} from 'redux-saga-test-plan';
import {call, getContext, select} from 'redux-saga-test-plan/matchers';
import {take} from 'redux-saga/effects';
import {throwError} from 'redux-saga-test-plan/providers';

import SagaErrorReporter from 'utils/SagaErrorReporter';
import * as notificationsActions from 'features/notifications/notificationsActions';
import {loadEventsInBackground} from 'features/grid/gridActions';
import * as environment from 'configs/environment';
import * as session from 'configs/session';

import AppStatusApi from '../AppStatusApi';
import * as actions from '../appStatusActions';
import {ActionTypes} from '../appStatusConstants';
import {currentNotificationsSelector} from '../appStatusSelectors';
import rootSaga, {getStatus, updateEventsIfOnline} from '../appStatusSagas';

const errorReporter = new SagaErrorReporter('appStatus');

jest.mock('features/grid/gridActions');

describe('appStatusSagas', () => {
  describe('rootSaga', () => {
    test('должен записывать AppStatusApi в контекст', () => {
      return expectSaga(rootSaga)
        .provide([[getContext('api'), {}]])
        .setContext({appStatusApi: new AppStatusApi({})})
        .silentRun(0);
    });

    test('должен подписываться на экшены', async () => {
      const {effects} = await expectSaga(rootSaga)
        .provide([[getContext('api'), {}]])
        .silentRun(0);

      expect(effects.take).toEqual([
        take(ActionTypes.START_GETTING_STATUS),
        take(ActionTypes.GET_STATUS),
        take(ActionTypes.CHANGE_NETWORK_STATUS)
      ]);
    });
  });

  describe('getStatus', () => {
    test('должен сохранять переданный статус', () => {
      const appStatusApi = new AppStatusApi();

      const statusFromConfig = {
        readOnly: true,
        notifications: [],
        resourceValidationConfig: {
          '8': {
            criticalDiff: 3
          }
        },
        features: {},
        corporateLayers: [],
        eventsToDisableAddButton: []
      };

      return expectSaga(getStatus, {status: statusFromConfig})
        .provide([
          [getContext('appStatusApi'), appStatusApi],
          [select(currentNotificationsSelector), []]
        ])
        .put(actions.getStatusSuccess(statusFromConfig))
        .run();
    });

    test('должен запрашивать статус с сервера и сохранять его', () => {
      const appStatusApi = new AppStatusApi();

      const receivedStatus = {
        readOnly: true,
        notifications: [],
        resourceValidationConfig: {
          '8': {
            criticalDiff: 3
          }
        },
        features: {},
        corporateLayers: [],
        eventsToDisableAddButton: []
      };

      return expectSaga(getStatus, {})
        .provide([
          [getContext('appStatusApi'), appStatusApi],
          [select(currentNotificationsSelector), []],
          [call.fn(appStatusApi.getStatus), receivedStatus]
        ])
        .put(actions.getStatusSuccess(receivedStatus))
        .run();
    });

    test('должен отобразить новые нотификации', () => {
      const appStatusApi = new AppStatusApi();

      const receivedStatus = {
        readOnly: true,
        notifications: [
          {
            uid: 'qwe',
            message: 'qwe'
          },
          {
            uid: 'gg',
            message: 'gg'
          }
        ]
      };

      return expectSaga(getStatus, {})
        .provide([
          [getContext('appStatusApi'), appStatusApi],
          [select(currentNotificationsSelector), []],
          [call.fn(appStatusApi.getStatus), receivedStatus]
        ])
        .put(notificationsActions.notifyPersistent(receivedStatus.notifications[0]))
        .put(notificationsActions.notifyPersistent(receivedStatus.notifications[1]))
        .run();
    });

    test('не должен отобразить приходившие ранее нотификации', () => {
      const appStatusApi = new AppStatusApi();

      const receivedStatus = {
        readOnly: true,
        notifications: [
          {
            uid: 'qwe',
            message: 'qwe'
          },
          {
            uid: 'gg',
            message: 'gg'
          }
        ]
      };

      return expectSaga(getStatus, {})
        .provide([
          [getContext('appStatusApi'), appStatusApi],
          [select(currentNotificationsSelector), ['qwe']],
          [call.fn(appStatusApi.getStatus), receivedStatus]
        ])
        .not.put(notificationsActions.notifyPersistent(receivedStatus.notifications[0]))
        .run();
    });

    test('должен убирать нотификации, приходившие ранее, но не пришедшие в текущем статусе', () => {
      const appStatusApi = new AppStatusApi();

      const receivedStatus = {
        readOnly: true,
        notifications: [
          {
            uid: 'gg',
            message: 'gg'
          }
        ]
      };

      const currentNotifications = ['qwe'];

      return expectSaga(getStatus, {})
        .provide([
          [getContext('appStatusApi'), appStatusApi],
          [select(currentNotificationsSelector), currentNotifications],
          [call.fn(appStatusApi.getStatus), receivedStatus]
        ])
        .put(notificationsActions.dissmissNotification(currentNotifications[0]))
        .run();
    });

    test('не должен убирать нотификации, приходившие ранее и пришедшие в текущем статусе', () => {
      const appStatusApi = new AppStatusApi();

      const receivedStatus = {
        readOnly: true,
        notifications: [
          {
            uid: 'gg',
            message: 'gg'
          }
        ]
      };

      const currentNotifications = ['gg'];

      return expectSaga(getStatus, {})
        .provide([
          [getContext('appStatusApi'), appStatusApi],
          [select(currentNotificationsSelector), currentNotifications],
          [call.fn(appStatusApi.getStatus), receivedStatus]
        ])
        .not.put(notificationsActions.dissmissNotification(currentNotifications[0]))
        .run();
    });

    test('должен логировать ошибку', () => {
      const error = {
        name: 'error'
      };

      return expectSaga(getStatus, {})
        .provide([[getContext('appStatusApi'), throwError(error)], [call.fn(errorReporter.send)]])
        .call([errorReporter, errorReporter.send], 'getStatus', error)
        .run();
    });
  });

  describe('updateEventsIfOnline', function() {
    beforeEach(() => {
      loadEventsInBackground.mockReturnValue({loadEventsInBackgroundAction: true});
    });

    test('должен вызвать обновление эвентов, если все условия верны', () => {
      sinon.stub(environment, 'isStaticMobileApp').value(true);
      sinon.stub(session, 'eventsDynamicUpdate').value(true);

      return expectSaga(updateEventsIfOnline, {offline: false})
        .put(loadEventsInBackground())
        .run();
    });

    test('не должен вызывать обновление эвентов, если это не оффлайн календарь', () => {
      sinon.stub(environment, 'isStaticMobileApp').value(false);
      sinon.stub(session, 'eventsDynamicUpdate').value(true);

      return expectSaga(updateEventsIfOnline, {offline: false})
        .not.put(loadEventsInBackground())
        .run();
    });

    test('не должен вызывать обновление эвентов, если нет флага динамического апдейта эвентов', () => {
      sinon.stub(environment, 'isStaticMobileApp').value(true);
      sinon.stub(session, 'eventsDynamicUpdate').value(false);

      return expectSaga(updateEventsIfOnline, {offline: false})
        .not.put(loadEventsInBackground())
        .run();
    });

    test('не должен вызывать обновление эвентов, если текущий статус сети - "оффлайн"', () => {
      sinon.stub(environment, 'isStaticMobileApp').value(true);
      sinon.stub(session, 'eventsDynamicUpdate').value(true);

      return expectSaga(updateEventsIfOnline, {offline: true})
        .not.put(loadEventsInBackground())
        .run();
    });
  });
});
