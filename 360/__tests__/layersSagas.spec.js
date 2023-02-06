import {takeEvery} from 'redux-saga/effects';
import {testSaga, expectSaga} from 'redux-saga-test-plan';
import {call, fork, getContext} from 'redux-saga-test-plan/matchers';
import {throwError} from 'redux-saga-test-plan/providers';
import {get} from 'lodash';
import cookies from 'js-cookie';

import i18n from 'utils/i18n';
import SagaErrorReporter from 'utils/SagaErrorReporter';
import Decision from 'constants/Decision';
import * as settingsActions from 'features/settings/settingsActions';
import {getEventsForLayer} from 'features/events/eventsActions';
import {
  notificationHelpers,
  notifySuccess,
  notifyFailure
} from 'features/notifications/notificationsActions';

import LayersApi from '../LayersApi';
import {ActionTypes} from '../layersConstants';
import rootSaga, * as layersSagas from '../layersSagas';
import * as layersActions from '../layersActions';

jest.mock('utils/i18n');

const errorReporter = new SagaErrorReporter('layers');

describe('layersSagas', () => {
  beforeEach(() => {
    jest.spyOn(notificationHelpers, 'generateId').mockReturnValue('id');
  });

  describe('rootSaga', () => {
    test('должен работать', () => {
      testSaga(rootSaga)
        .next()
        .getContext('api')
        .next()
        .setContext({layersApi: new LayersApi()})
        .next()
        .all([
          takeEvery(layersActions.getLayersNetwork.type, layersSagas.getLayersNetwork),
          takeEvery(layersActions.getLayerNetwork.type, layersSagas.getLayerNetwork),
          takeEvery(ActionTypes.CREATE_LAYER, layersSagas.createLayer),
          takeEvery(ActionTypes.UPDATE_LAYER, layersSagas.updateLayer),
          takeEvery(ActionTypes.DELETE_LAYER, layersSagas.deleteLayer),
          takeEvery(ActionTypes.TOGGLE_LAYER, layersSagas.toggleLayer),
          takeEvery(ActionTypes.SHARE_LAYER, layersSagas.shareLayer),
          takeEvery(ActionTypes.CREATE_FEED, layersSagas.createFeed),
          takeEvery(ActionTypes.UPDATE_FEED, layersSagas.updateFeed),
          takeEvery(ActionTypes.IMPORT_LAYER, layersSagas.importLayer),
          takeEvery(ActionTypes.CREATE_TOKEN, layersSagas.createToken),
          takeEvery(ActionTypes.HANDLE_LAYER_INVITATION, layersSagas.handleLayerInvitation)
        ])
        .next()
        .isDone();
    });
  });

  describe('getLayersNetwork', () => {
    describe('успешное выполнение', () => {
      test('должен вызывать метод getLayers у layersApi', () => {
        const layersApi = new LayersApi();
        return expectSaga(layersSagas.getLayersNetwork)
          .provide([[getContext('layersApi'), layersApi], [call.fn(layersApi.getLayers), []]])
          .call([layersApi, layersApi.getLayers])
          .run();
      });

      test('должен записывать полученные слои в стейт', () => {
        const layersApi = new LayersApi();
        return expectSaga(layersSagas.getLayersNetwork)
          .provide([[getContext('layersApi'), layersApi], [call.fn(layersApi.getLayers), []]])
          .put(layersActions.getLayersSuccess([]))
          .run();
      });
    });

    describe('неуспешное выполнение', () => {
      test('должен логировать ошибку', () => {
        return expectSaga(layersSagas.getLayersNetwork)
          .provide([
            [getContext('layersApi'), throwError({name: 'error'})],
            [call.fn(errorReporter.send)]
          ])
          .call([errorReporter, errorReporter.send], 'getLayersNetwork', {name: 'error'})
          .run();
      });
    });
  });

  describe('getLayerNetwork', () => {
    describe('успешное выполнение', () => {
      test('должен вызывать метод getLayer у layersApi', () => {
        const layersApi = new LayersApi();
        return expectSaga(layersSagas.getLayerNetwork, {payload: {id: '1'}})
          .provide([
            [getContext('layersApi'), layersApi],
            [call([layersApi, layersApi.getLayer], '1'), {id: '1', token: 'token'}]
          ])
          .call([layersApi, layersApi.getLayer], '1')
          .run();
      });

      test('должен записывать полученный слой в стейт', () => {
        const layersApi = new LayersApi();
        return expectSaga(layersSagas.getLayerNetwork, {payload: {id: '1'}})
          .provide([
            [getContext('layersApi'), layersApi],
            [call.fn(layersApi.getLayer), {id: '1', token: 'token'}]
          ])
          .put(layersActions.getLayerSuccess({id: '1', token: 'token'}))
          .run();
      });

      test('должен создавать токен, если его нет и пользователь является владельцем', () => {
        const layersApi = new LayersApi();
        return expectSaga(layersSagas.getLayerNetwork, {payload: {id: '1'}})
          .provide([
            [getContext('layersApi'), layersApi],
            [call.fn(layersApi.getLayer), {id: '1', isOwner: true}],
            [fork.fn(layersSagas.createToken)]
          ])
          .fork(layersSagas.createToken, {id: '1'})
          .run();
      });

      test('не должен создавать токен, если он уже есть', () => {
        const layersApi = new LayersApi();
        return expectSaga(layersSagas.getLayerNetwork, {payload: {id: '1'}})
          .provide([
            [getContext('layersApi'), layersApi],
            [call.fn(layersApi.getLayer), {id: '1', token: 'token'}]
          ])
          .not.fork(layersSagas.createToken, {id: '1'})
          .run();
      });

      test('не должен создавать токен, если пользователь не является владельцем', () => {
        const layersApi = new LayersApi();
        return expectSaga(layersSagas.getLayerNetwork, {payload: {id: '1'}})
          .provide([
            [getContext('layersApi'), layersApi],
            [call.fn(layersApi.getLayer), {id: '1', isOwner: false}]
          ])
          .not.fork(layersSagas.createToken, {id: '1'})
          .run();
      });
    });

    describe('неуспешное выполнение', () => {
      test('должен логировать ошибку', () => {
        return expectSaga(layersSagas.getLayerNetwork, {payload: {id: '1'}})
          .provide([
            [getContext('layersApi'), throwError({name: 'error'})],
            [call.fn(errorReporter.send)]
          ])
          .call([errorReporter, errorReporter.send], 'getLayerNetwork', {name: 'error'})
          .run();
      });

      test('должен записывать ошибку в стейт', () => {
        return expectSaga(layersSagas.getLayerNetwork, {payload: {id: '1'}})
          .provide([
            [getContext('layersApi'), throwError({name: 'error'})],
            [call.fn(errorReporter.send)]
          ])
          .put(layersActions.getLayerFailure({name: 'error'}))
          .run();
      });
    });
  });

  describe('createToken', () => {
    const layersApi = new LayersApi();
    const defaultProviders = [
      [getContext('layersApi'), layersApi],
      [call.fn(layersApi.createToken), {}]
    ];

    describe('успешное выполнение', () => {
      test('должен вызывать createToken у layersApi', () => {
        return expectSaga(layersSagas.createToken, {id: '1', forceNew: true})
          .provide(defaultProviders)
          .call([layersApi, layersApi.createToken], '1', {forceNew: true})
          .run();
      });

      test('должен записывать полученный токен в состояние', () => {
        return expectSaga(layersSagas.createToken, {id: '1', forceNew: true})
          .provide([[call.fn(layersApi.createToken), {token: 'token'}], ...defaultProviders])
          .put(layersActions.updateLayerSuccess('1', {token: 'token'}))
          .run();
      });

      test('должен показывать оповещение, если создаётся новый токен', () => {
        return expectSaga(layersSagas.createToken, {id: '1', forceNew: true})
          .provide(defaultProviders)
          .put(notifySuccess({message: i18n.get('notifications', 'secretUrlReset')}))
          .run();
      });

      test('не должен показывать оповещение, если токен не создаётся', () => {
        return expectSaga(layersSagas.createToken, {id: '1', forceNew: false})
          .provide(defaultProviders)
          .not.put(notifySuccess({message: i18n.get('notifications', 'secretUrlReset')}))
          .run();
      });
    });

    describe('неуспешное выполнение', () => {
      test('должен логировать ошибку', () => {
        return expectSaga(layersSagas.createToken, {})
          .provide([
            [getContext('layersApi'), throwError({name: 'error'})],
            [call.fn(errorReporter.send)]
          ])
          .call([errorReporter, errorReporter.send], 'createToken', {name: 'error'})
          .run();
      });
    });
  });

  describe('createLayer', () => {
    test('должен работать', () => {
      const layersApi = new LayersApi();
      const action = {
        values: {
          name: 'name'
        },
        resolveForm() {},
        rejectForm() {}
      };
      const response = {
        layerId: '100'
      };
      const error = {
        name: 'error'
      };

      testSaga(layersSagas.createLayer, action)
        .next()
        .getContext('layersApi')
        .next(layersApi)
        .call([layersApi, layersApi.create], action.values)
        .next(response)
        .put(
          layersActions.createLayerSuccess({
            ...action.values,
            id: response.layerId
          })
        )
        .next()
        .put(notifySuccess({message: 'notifications.layerCreated'}))
        .next()
        .put(settingsActions.updateSettings({values: {isCalendarsListExpanded: true}}))
        .next()
        .call(action.resolveForm)
        .next()
        .isDone()

        .restart()
        .next()
        .throw(error)
        .call([errorReporter, errorReporter.send], 'createLayer', error)
        .next()
        .put(notifyFailure({error}))
        .next()
        .call(action.rejectForm)
        .next()
        .isDone();
    });
  });

  describe('updateLayer', () => {
    test('должен работать', () => {
      const layersApi = new LayersApi();
      const action = {
        id: '100',
        applyNotificationsToEvents: true,
        values: {
          name: 'new_name'
        },
        resolveForm() {},
        rejectForm() {}
      };
      const error = {
        name: 'error'
      };

      testSaga(layersSagas.updateLayer, action)
        .next()
        .getContext('layersApi')
        .next(layersApi)
        .call([layersApi, layersApi.update], action.id, {
          values: action.values,
          applyNotificationsToEvents: action.applyNotificationsToEvents
        })
        .next()
        .put(layersActions.updateLayerSuccess(action.id, action.values))
        .next()
        .put(notifySuccess({message: 'notifications.changesSaved'}))
        .next()
        .call(action.resolveForm)
        .next()
        .isDone()

        .restart()
        .next()
        .throw(error)
        .call([errorReporter, errorReporter.send], 'updateLayer', error)
        .next()
        .put(notifyFailure({error}))
        .next()
        .call(action.rejectForm)
        .next()
        .isDone();
    });
  });

  describe('deleteLayer', () => {
    test('должен работать', () => {
      const layersApi = new LayersApi();
      const action = {
        id: '100',
        recipientLayerId: '101',
        resolveForm() {},
        rejectForm() {}
      };
      const error = {
        name: 'error'
      };

      testSaga(layersSagas.deleteLayer, action)
        .next()
        .getContext('layersApi')
        .next(layersApi)
        .call([layersApi, layersApi.delete], action.id, action.recipientLayerId)
        .next()
        .put(layersActions.deleteLayerSuccess(action.id, action.recipientLayerId))
        .next()
        .put(notifySuccess({message: 'notifications.layerDeleted'}))
        .next()
        .call(action.resolveForm)
        .next()
        .isDone()

        .restart()
        .next()
        .throw(error)
        .call([errorReporter, errorReporter.send], 'deleteLayer', error)
        .next()
        .put(layersActions.deleteLayerFailure(action.id, action.recipientLayerId))
        .next()
        .put(notifyFailure({error}))
        .next()
        .call(action.rejectForm)
        .next()
        .isDone();
    });
  });

  describe('toggleLayer', () => {
    test('должен работать', () => {
      const layersApi = new LayersApi();
      const action = {
        id: '100',
        checked: false
      };
      const error = {
        name: 'error'
      };

      testSaga(layersSagas.toggleLayer, action)
        .next()
        .getContext('layersApi')
        .next(layersApi)
        .call([layersApi, layersApi.toggle], action.id, action.checked)
        .next()
        .put(layersActions.toggleLayerSuccess(action.id, action.checked))
        .next()
        .call(get, action, 'checked')
        .next(false)
        .isDone()

        .back()
        .next(true)
        .put(getEventsForLayer(action.id))
        .next()
        .isDone()

        .restart()
        .next()
        .throw(error)
        .call([errorReporter, errorReporter.send], 'toggleLayer', error)
        .next()
        .isDone();
    });
  });

  describe('shareLayer', () => {
    test('должен работать', () => {
      const layersApi = new LayersApi();
      const action = {
        id: 100,
        name: 'name',
        privateToken: undefined
      };
      const response = {
        id: 100
      };
      const error = {
        name: 'error'
      };

      testSaga(layersSagas.shareLayer, action)
        .next()
        .getContext('layersApi')
        .next(layersApi)
        .call([layersApi, layersApi.share], action.id, action.privateToken)
        .next(response)
        .put(layersActions.shareLayerSuccess(response))
        .next()
        .put(getEventsForLayer(response.id))
        .next()
        .put(notifySuccess({message: 'notifications.layerAdded'}))
        .next()
        .put(settingsActions.updateSettings({values: {isCalendarsListExpanded: true}}))
        .next()
        .isDone()

        .restart()
        .next()
        .throw(error)
        .call([errorReporter, errorReporter.send], 'shareLayer', error)
        .next()
        .put(notifyFailure({error}))
        .next()
        .isDone();
    });
  });

  describe('createFeed', () => {
    test('должен работать', () => {
      const layersApi = new LayersApi();
      const action = {
        url: 'url',
        name: 'name'
      };
      const response = {};
      const error = {
        name: 'error'
      };

      testSaga(layersSagas.createFeed, action)
        .next()
        .getContext('layersApi')
        .next(layersApi)
        .call([layersApi, layersApi.createFeed], action.url)
        .next(response)
        .put(layersActions.createFeedSuccess(response))
        .next()
        .put(notifySuccess({message: 'notifications.layerAdded'}))
        .next()
        .put(settingsActions.updateSettings({values: {isSubscriptionsListExpanded: true}}))
        .next()
        .isDone()

        .restart()
        .next()
        .throw(error)
        .call([errorReporter, errorReporter.send], 'createFeed', error)
        .next()
        .put(notifyFailure({error}))
        .next()
        .isDone();
    });
  });

  describe('updateFeed', () => {
    test('должен работать', () => {
      const layersApi = new LayersApi();
      const action = {
        id: '100'
      };
      const error = {
        name: 'error'
      };

      testSaga(layersSagas.updateFeed, action)
        .next()
        .getContext('layersApi')
        .next(layersApi)
        .call([layersApi, layersApi.updateFeed], action.id)
        .next()
        .put(notifySuccess({message: 'notifications.eventsWillBeLoaded'}))
        .next()
        .isDone()

        .restart()
        .next()
        .throw(error)
        .call([errorReporter, errorReporter.send], 'updateFeed', error)
        .next()
        .put(notifyFailure({error}))
        .next()
        .isDone();
    });
  });

  describe('importLayer', () => {
    test('должен работать', () => {
      const layersApi = new LayersApi();
      const action = {
        values: {},
        resolveForm() {},
        rejectForm() {}
      };
      const responseForFeed = {
        id: 100,
        type: 'feed'
      };
      const responseForLayer = {
        id: 100,
        type: 'user'
      };
      const error = {
        name: 'error'
      };

      testSaga(layersSagas.importLayer, action)
        .next()
        .getContext('layersApi')
        .next(layersApi)
        .call([layersApi, layersApi.import], action.values)
        .next(responseForFeed)
        .put(layersActions.importLayerSuccess(responseForFeed))
        .next()
        .put(notifySuccess({message: 'notifications.eventsWillBeLoaded'}))
        .next()
        .put(settingsActions.updateSettings({values: {isSubscriptionsListExpanded: true}}))
        .next()
        .call(action.resolveForm)
        .next()
        .isDone()

        .back(5)
        .next(responseForLayer)
        .put(layersActions.importLayerSuccess(responseForLayer))
        .next()
        .put(getEventsForLayer(responseForLayer.id))
        .next()
        .put(notifySuccess({message: 'notifications.eventsWillBeLoaded'}))
        .next()
        .put(settingsActions.updateSettings({values: {isCalendarsListExpanded: true}}))
        .next()
        .call(action.resolveForm)
        .next()
        .isDone()

        .restart()
        .next()
        .throw(error)
        .call([errorReporter, errorReporter.send], 'importLayer', error)
        .next()
        .put(notifyFailure({error}))
        .next()
        .call(action.rejectForm)
        .next()
        .isDone();
    });
  });

  describe('handleLayerInvitation', () => {
    describe('успешное выполнение', () => {
      test('должен получать куку с результатом обработки решения', () => {
        return expectSaga(layersSagas.handleLayerInvitation)
          .provide([[call.fn(cookies.getJSON)], [call.fn(cookies.remove)]])
          .call([cookies, cookies.getJSON], 'layer_invitation')
          .run();
      });

      describe('есть кука', () => {
        describe('неуспешная обработка решения', () => {
          test('должен показывать оповещение об неуспешной обработки решения', () => {
            return expectSaga(layersSagas.handleLayerInvitation)
              .provide([
                [call.fn(cookies.getJSON), {error: {name: 'error'}}],
                [call.fn(cookies.remove)]
              ])
              .put(notifyFailure({error: {name: 'error'}}))
              .run();
          });
        });

        describe('успешная обработка решения', () => {
          test('должен показывать оповещение об успешной обработки решения', () => {
            return expectSaga(layersSagas.handleLayerInvitation)
              .provide([
                [call.fn(cookies.getJSON), {decision: Decision.YES}],
                [call.fn(cookies.remove)]
              ])
              .put(notifySuccess({message: i18n.get('notifications', 'decision_yes')}))
              .run();
          });

          test('должен раскрывать список календарей', () => {
            return expectSaga(layersSagas.handleLayerInvitation)
              .provide([
                [call.fn(cookies.getJSON), {decision: Decision.YES}],
                [call.fn(cookies.remove)]
              ])
              .put(
                settingsActions.updateSettings({
                  values: {isCalendarsListExpanded: true}
                })
              )
              .run();
          });
        });

        test('должен удалять куку', () => {
          return expectSaga(layersSagas.handleLayerInvitation)
            .provide([
              [call.fn(cookies.getJSON), {decision: Decision.YES}],
              [call.fn(cookies.remove)]
            ])
            .call([cookies, cookies.remove], 'layer_invitation')
            .run();
        });
      });

      describe('нет куки', () => {
        test('не должен показывать оповещение об неуспешной обработки решения', () => {
          return expectSaga(layersSagas.handleLayerInvitation)
            .provide([[call.fn(cookies.getJSON)], [call.fn(cookies.remove)]])
            .not.put.actionType(notifyFailure().type)
            .run();
        });

        test('не должен показывать оповещение об успешной обработки решения', () => {
          return expectSaga(layersSagas.handleLayerInvitation)
            .provide([[call.fn(cookies.getJSON)], [call.fn(cookies.remove)]])
            .not.put.actionType(notifySuccess().type)
            .run();
        });

        test('не должен раскрывать список календарей', () => {
          return expectSaga(layersSagas.handleLayerInvitation)
            .provide([[call.fn(cookies.getJSON)], [call.fn(cookies.remove)]])
            .not.put(
              settingsActions.updateSettings({
                values: {isCalendarsListExpanded: true}
              })
            )
            .run();
        });

        test('не должен удалять куку', () => {
          return expectSaga(layersSagas.handleLayerInvitation)
            .provide([[call.fn(cookies.getJSON)], [call.fn(cookies.remove)]])
            .not.call([cookies, cookies.remove], 'layer_invitation')
            .run();
        });
      });
    });

    describe('неуспешное выполнение', () => {
      test('должен логировать ошибку', () => {
        return expectSaga(layersSagas.handleLayerInvitation)
          .provide([
            [call.fn(cookies.getJSON), throwError({name: 'error'})],
            [call.fn(errorReporter.send)]
          ])
          .call([errorReporter, errorReporter.send], 'handleLayerInvitation', {name: 'error'})
          .run();
      });
    });
  });
});
