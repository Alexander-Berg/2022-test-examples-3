import {takeEvery, throttle} from 'redux-saga/effects';
import {testSaga, expectSaga} from 'redux-saga-test-plan';
import * as matchers from 'redux-saga-test-plan/matchers';
import {throwError} from 'redux-saga-test-plan/providers';
import {get} from 'lodash';
import moment from 'moment';
import cookies from 'js-cookie';

import * as environment from 'configs/environment';
import i18n from 'utils/i18n';
import SagaErrorReporter from 'utils/SagaErrorReporter';
import Decision from 'constants/Decision';
import {getSettings} from 'features/settings/settingsSelectors';
import {getCurrentUser} from 'features/settings/settingsSelectors';
import {getLayerById, getToggledOnLayersIds} from 'features/layers/layersSelectors';
import {getLayers, toggleLayer} from 'features/layers/layersActions';
import {
  notifyFailure,
  notifySuccess,
  notificationHelpers
} from 'features/notifications/notificationsActions';
import {logDeleteEvent} from 'features/logging/loggingActions';
import EventFormId from 'features/eventForm/EventFormId';
import {getPrivateToken} from 'features/embed/embedSelectors';
import {onErrorCreateEvent, onSuccessCreateEvent} from 'features/embedCreateEventMiniForm/helpers';

import EventsApi from '../EventsApi';
import {getLastUpdateTs} from '../eventsSelectors';
import {ActionTypes} from '../eventsConstants';
import rootSaga, * as eventsSagas from '../eventsSagas';
import * as eventsActions from '../eventsActions';
import getLoadingRangeSelector from '../selectors/getLoadingRangeSelector';
import {updateOfflineData} from '../eventsSagas';
import {getEventsByLogin} from '../eventsSagas';

jest.mock('utils/i18n');

const errorReporter = new SagaErrorReporter('events');

describe('eventsSagas', () => {
  beforeEach(() => {
    jest.spyOn(notificationHelpers, 'generateId').mockReturnValue('id');
  });

  describe('rootSaga', () => {
    test('должен работать', () => {
      testSaga(rootSaga)
        .next()
        .getContext('api')
        .next()
        .setContext({eventsApi: new EventsApi()})
        .next()
        .all([
          takeEvery(ActionTypes.CREATE_EVENT, eventsSagas.createEvent),
          takeEvery(ActionTypes.DROP_EVENT, eventsSagas.dropEvent),
          takeEvery(ActionTypes.UPDATE_EVENT, eventsSagas.updateEvent),
          takeEvery(ActionTypes.UPDATE_DECISION, eventsSagas.updateDecision),
          takeEvery(ActionTypes.CONFIRM_REPETITION, eventsSagas.confirmRepetition),
          takeEvery(ActionTypes.DELETE_EVENT, eventsSagas.deleteEvent),
          takeEvery(ActionTypes.DETACH_EVENT, eventsSagas.detachEvent),
          takeEvery(eventsActions.getEventsNetwork.type, eventsSagas.getEventsNetwork),
          takeEvery(eventsActions.getEventsOffline.type, eventsSagas.getEventsOffline),
          takeEvery(ActionTypes.GET_EVENTS_FOR_LAYER, eventsSagas.getEventsForLayer),
          takeEvery(ActionTypes.GET_EVENTS_BY_LOGIN, eventsSagas.getEventsByLogin),
          throttle(5000, ActionTypes.GET_MODIFIED_EVENTS, eventsSagas.getModifiedEvents),
          takeEvery(eventsActions.getEventNetwork.type, eventsSagas.getEventNetwork),
          takeEvery(eventsActions.getEventOffline.type, eventsSagas.getEventOffline),
          takeEvery(ActionTypes.HANDLE_EVENT_INVITATION, eventsSagas.handleEventInvitation),
          takeEvery(
            eventsActions.getRepetitionDescription.type,
            eventsSagas.getRepetitionDescription
          ),
          takeEvery(eventsActions.updateOfflineData.type, updateOfflineData)
        ])
        .next()
        .isDone();
    });
  });

  describe('createEvent', () => {
    test('должен работать', () => {
      const eventsApi = new EventsApi();
      const showEventId = 999999;
      const action = {
        values: {
          name: 'name',
          start: 'start',
          layerId: 1,
          organizer: {login: 'tavria'},
          id: 12
        },
        resolveForm() {},
        rejectForm() {},
        officesToStartBooking: [1, 2],
        initialOfficesToStartBooking: [1, 3]
      };
      const responseForCreating = {
        externalIds: 'externalIds',
        showEventId
      };
      const responseForGetting = {
        events: []
      };
      const error = {
        name: 'error'
      };

      testSaga(eventsSagas.createEvent, action)
        .next()
        .getContext('eventsApi')
        .next(eventsApi)
        .call([eventsApi, eventsApi.create], action.values)
        .next(responseForCreating)
        .call(eventsSagas.getEventsByExternalId, responseForCreating.externalIds)
        .next(responseForGetting)
        .put(eventsActions.createEventSuccess(responseForGetting.events))
        .next()
        .call(onSuccessCreateEvent, showEventId, false)
        .next()
        .call(action.resolveForm, action.values, showEventId)
        .next()
        .call(eventsSagas.tryTurnOnLayer, action.values.layerId)
        .next()
        .call(
          eventsSagas.updateEventResourcesBooking,
          showEventId,
          action.initialOfficesToStartBooking,
          action.officesToStartBooking,
          action.values.organizer
        )
        .next()
        .isDone()

        .restart()
        .next()
        .throw(error)
        .call([errorReporter, errorReporter.send], 'createEvent', error)
        .next()
        .call(onErrorCreateEvent)
        .next()
        .call(action.rejectForm, error)
        .next()
        .isDone();
    });

    test('должен перезагружать слои, если целевой слой не указан', () => {
      const eventsApi = new EventsApi();
      const action = {
        values: {
          name: 'name',
          start: 'start',
          layerId: null,
          organizer: {login: 'tavria'}
        },
        resolveForm() {},
        rejectForm() {},
        officesToStartBooking: [1, 2],
        initialOfficesToStartBooking: [1, 3]
      };

      return expectSaga(eventsSagas.createEvent, action)
        .provide([
          [matchers.getContext('eventsApi'), eventsApi],
          [matchers.call.fn(eventsApi.create), {}],
          [matchers.call.fn(eventsSagas.getEventsByExternalId), {}],
          [matchers.call.fn(action.resolveForm)],
          [matchers.call.fn(action.rejectForm)],
          [matchers.call.fn(eventsSagas.tryTurnOnLayer)],
          [matchers.call.fn(eventsSagas.updateEventResourcesBooking)]
        ])
        .put(getLayers())
        .run();
    });

    test('не должен перезагружать слои, если целевой слой указан', () => {
      const eventsApi = new EventsApi();
      const action = {
        values: {
          name: 'name',
          start: 'start',
          layerId: 1,
          organizer: {login: 'tavria'}
        },
        resolveForm() {},
        rejectForm() {},
        officesToStartBooking: [1, 2],
        initialOfficesToStartBooking: [1, 3]
      };

      return expectSaga(eventsSagas.createEvent, action)
        .provide([
          [matchers.getContext('eventsApi'), eventsApi],
          [matchers.call.fn(eventsApi.create), {}],
          [matchers.call.fn(eventsSagas.getEventsByExternalId), {}],
          [matchers.call.fn(action.resolveForm)],
          [matchers.call.fn(action.rejectForm)],
          [matchers.call.fn(eventsSagas.tryTurnOnLayer)],
          [matchers.call.fn(eventsSagas.updateEventResourcesBooking)]
        ])
        .not.put(getLayers())
        .run();
    });
  });

  describe('updateEvent', () => {
    test('должен работать', () => {
      const eventsApi = new EventsApi();
      const action = {
        oldEvent: {
          name: 'old_event',
          start: 'start',
          layerId: 1
        },
        newEvent: {
          name: 'new_event',
          start: 'start',
          layerId: 2,
          organizer: {login: 'tavria'},
          id: 10
        },
        mailToAll: true,
        applyToFuture: false,
        resolveForm() {},
        rejectForm() {},
        officesToStartBooking: [1, 2],
        initialOfficesToStartBooking: [1, 3]
      };
      const responseForUpdating = {
        externalIds: 'externalIds'
      };
      const responseForGetting = {
        events: []
      };
      const error = {
        name: 'error'
      };

      testSaga(eventsSagas.updateEvent, action)
        .next()
        .getContext('eventsApi')
        .next(eventsApi)
        .call([eventsApi, eventsApi.update], {
          oldEvent: action.oldEvent,
          newEvent: action.newEvent,
          mailToAll: action.mailToAll,
          applyToFuture: action.applyToFuture
        })
        .next(responseForUpdating)
        .call(eventsSagas.getEventsByExternalId, responseForUpdating.externalIds)
        .next(responseForGetting)
        .put(
          eventsActions.updateEventSuccess({
            oldEvent: action.oldEvent,
            newEvents: responseForGetting.events,
            applyToFuture: action.applyToFuture
          })
        )
        .next()
        .call(action.resolveForm, action.newEvent)
        .next()
        .call(eventsSagas.tryTurnOnLayer, action.newEvent.layerId)
        .next()
        .call(
          eventsSagas.updateEventResourcesBooking,
          action.newEvent.id,
          action.initialOfficesToStartBooking,
          action.officesToStartBooking,
          action.newEvent.organizer
        )
        .next()
        .isDone()

        .restart()
        .next()
        .throw(error)
        .call([errorReporter, errorReporter.send], 'updateEvent', error)
        .next()
        .call(action.rejectForm, error)
        .next()
        .isDone();
    });
  });

  describe('dropEvent', () => {
    test('должен работать', () => {
      const eventsApi = new EventsApi();
      const action = {
        oldEvent: {
          name: 'old_event',
          layerId: 1,
          data: {
            hasTelemostLink: true
          }
        },
        newEvent: {
          name: 'new_event',
          layerId: 2
        }
      };
      const responseForUpdating = {
        externalIds: 'externalIds'
      };
      const responseForGetting = {
        events: []
      };
      const error = {
        name: 'error'
      };

      testSaga(eventsSagas.dropEvent, action)
        .next()
        .getContext('eventsApi')
        .next(eventsApi)
        .put(
          eventsActions.replaceEvent({
            oldEvent: action.oldEvent,
            newEvent: {
              ...action.oldEvent,
              ...action.newEvent,
              isInProgress: true
            }
          })
        )
        .next()
        .call([eventsApi, eventsApi.update], {
          oldEvent: action.oldEvent,
          newEvent: {
            ...action.newEvent,
            eventData: action.oldEvent.data
          },
          applyToFuture: false
        })
        .next(responseForUpdating)
        .call(eventsSagas.getEventsByExternalId, responseForUpdating.externalIds)
        .next(responseForGetting)
        .put(
          eventsActions.updateEventSuccess({
            oldEvent: action.oldEvent,
            newEvents: responseForGetting.events,
            applyToFuture: false
          })
        )
        .next()
        .isDone()

        .restart()
        .next()
        .throw(error)
        .call([errorReporter, errorReporter.send], 'dropEvent', error)
        .next()
        .put(notifyFailure({error}))
        .next()
        .put(
          eventsActions.replaceEvent({
            oldEvent: action.newEvent,
            newEvent: action.oldEvent
          })
        )
        .next()
        .isDone();
    });
  });

  describe('updateEventResourcesBooking', () => {
    test('не должен работать, если это не корпоративный календарь', () => {
      const eventId = 123;
      const initialOfficeIds = [];
      const officeIds = [10, 15];
      const organizer = 'tet4enko@yandex-team.ru';

      sinon.stub(environment, 'isCorp').value(false);

      const eventsApi = new EventsApi();

      expectSaga(
        eventsSagas.updateEventResourcesBooking,
        eventId,
        initialOfficeIds,
        officeIds,
        organizer
      )
        .provide([
          [matchers.getContext('eventsApi'), eventsApi],
          [matchers.select(getCurrentUser), {login: 'tet4enko'}]
        ])
        .not.call([eventsApi, eventsApi.updateEventResourcesBooking], {
          eventId,
          officeIds,
          organizer: 'tet4enko'
        });
    });

    test('не должен работать, если оффисы для автоподбора не поменялись', () => {
      const eventId = 123;
      const initialOfficeIds = [10, 15];
      const officeIds = [10, 15];
      const organizer = 'tet4enko@yandex-team.ru';

      sinon.stub(environment, 'isCorp').value(false);

      const eventsApi = new EventsApi();

      expectSaga(
        eventsSagas.updateEventResourcesBooking,
        eventId,
        initialOfficeIds,
        officeIds,
        organizer
      )
        .provide([
          [matchers.getContext('eventsApi'), eventsApi],
          [matchers.select(getCurrentUser), {login: 'tet4enko'}]
        ])
        .not.call([eventsApi, eventsApi.updateEventResourcesBooking], {
          eventId,
          officeIds,
          organizer: 'tet4enko'
        });
    });

    test('должен работать', () => {
      const eventId = 123;
      const initialOfficeIds = [];
      const officeIds = [10, 15];
      const organizer = 'tet4enko@yandex-team.ru';

      sinon.stub(environment, 'isCorp').value(true);

      const eventsApi = new EventsApi();

      expectSaga(
        eventsSagas.updateEventResourcesBooking,
        eventId,
        initialOfficeIds,
        officeIds,
        organizer
      )
        .provide([
          [matchers.getContext('eventsApi'), eventsApi],
          [matchers.select(getCurrentUser), {login: 'tet4enko'}]
        ])
        .call([eventsApi, eventsApi.updateEventResourcesBooking], {
          eventId,
          officeIds,
          organizer: 'tet4enko'
        });
    });
  });

  describe('updateDecision', () => {
    test('должен работать', () => {
      const eventsApi = new EventsApi();
      const meta = {
        form: EventFormId.fromParams(EventFormId.VIEWS.POPUP, EventFormId.MODES.EDIT).toString()
      };
      const action = {
        event: {id: '100'},
        decision: Decision.YES,
        reason: '',
        applyToAll: true,
        resolve() {},
        meta
      };
      const settings = {
        email: 'email@email.com'
      };

      expectSaga(eventsSagas.updateDecision, action)
        .provide([
          [matchers.getContext('eventsApi'), eventsApi],
          [matchers.select(getSettings), settings],
          [matchers.call.fn(eventsApi.updateDecision), {}]
        ])
        .call([eventsApi, eventsApi.updateDecision], {
          event: action.event,
          decision: action.decision,
          reason: action.reason,
          applyToAll: action.applyToAll
        })
        .put(
          eventsActions.makeUpdateDecisionSuccess(meta)({
            event: action.event,
            decision: action.decision,
            applyToAll: action.applyToAll,
            myEmail: settings.email
          })
        )
        .put(notifySuccess({message: i18n.get('notifications', `decision_${action.decision}`)}))
        .call(action.resolve)
        .put({
          type: ActionTypes.UPDATE_DECISION_DONE,
          event: action.event
        })
        .run();
    });
    test('должен вызывать errorReporter и бросать экшн notifyFailure и updateDecisionDone', () => {
      expectSaga(eventsSagas.updateDecision, {event: {}})
        .provide([[matchers.getContext('eventsApi'), throwError({error: 'error'})]])
        .call([errorReporter, errorReporter.send], 'updateDecision', {error: 'error'})
        .put(notifyFailure({error: {error: 'error'}}))
        .put({
          type: ActionTypes.UPDATE_DECISION_DONE,
          event: {}
        })
        .run();
    });
  });

  describe('deleteEvent', () => {
    test('должен работать', () => {
      const eventsApi = new EventsApi();
      const eventId = '111';
      const instanceStartTs = Date.now();
      const action = {
        event: {
          id: eventId,
          externalId: 'externalId',
          instanceStartTs
        },
        applyToFuture: true,
        resolveForm() {},
        rejectForm() {}
      };
      const responseForGetting = {
        events: []
      };
      const error = {
        name: 'error'
      };

      testSaga(eventsSagas.deleteEvent, action)
        .next()
        .getContext('eventsApi')
        .next(eventsApi)
        .call([eventsApi, eventsApi.delete], {
          event: action.event,
          applyToFuture: action.applyToFuture
        })
        .next()
        .call(get, action, 'applyToFuture')

        // без дозапроса событий
        .save('checkpoint')
        .next(true)
        .call(get, action, 'event')
        .next({
          repetition: {},
          isRecurrence: false
        })
        .put(
          eventsActions.deleteEventSuccess({
            event: action.event,
            newEvents: undefined
          })
        )
        .next()
        .put(
          logDeleteEvent({
            instanceStartTs: null,
            eventId
          })
        )

        // без дозапроса событий
        .restore('checkpoint')
        .save('checkpoint')
        .next(false)
        .call(get, action, 'event')
        .next({
          repetition: undefined,
          isRecurrence: false
        })
        .put(
          eventsActions.deleteEventSuccess({
            event: action.event,
            newEvents: undefined
          })
        )
        .next()
        .put(
          logDeleteEvent({
            instanceStartTs: null,
            eventId
          })
        )

        // c дозапросом событий
        .restore('checkpoint')
        .save('checkpoint')
        .next(false)
        .call(get, action, 'event')
        .next({
          repetition: undefined,
          isRecurrence: true
        })
        .call(eventsSagas.getEventsByExternalId, action.event.externalId)
        .next(responseForGetting)
        .put(
          eventsActions.deleteEventSuccess({
            event: action.event,
            newEvents: responseForGetting.events
          })
        )
        .next()
        .put(
          logDeleteEvent({
            instanceStartTs: null,
            eventId
          })
        )

        // c дозапросом событий
        .restore('checkpoint')
        .next(false)
        .call(get, action, 'event')
        .next({
          repetition: {},
          isRecurrence: false
        })
        .call(eventsSagas.getEventsByExternalId, action.event.externalId)
        .next(responseForGetting)
        .put(
          eventsActions.deleteEventSuccess({
            event: action.event,
            newEvents: responseForGetting.events
          })
        )
        .next()
        .put(
          logDeleteEvent({
            instanceStartTs: null,
            eventId
          })
        )

        .next()
        .call(action.resolveForm)
        .next()
        .isDone()

        .restart()
        .next()
        .throw(error)
        .call([errorReporter, errorReporter.send], 'deleteEvent', error)
        .next()
        .put(notifyFailure({error}))
        .next()
        .call(action.rejectForm)
        .next()
        .isDone();
    });
  });

  describe('detachEvent', () => {
    test('должен работать', () => {
      const eventsApi = new EventsApi();
      const action = {
        event: {
          id: '100'
        },
        resolveForm() {},
        rejectForm() {}
      };
      const error = {
        name: 'error'
      };

      testSaga(eventsSagas.detachEvent, action)
        .next()
        .getContext('eventsApi')
        .next(eventsApi)
        .call([eventsApi, eventsApi.detach], action.event)
        .next()
        .put(eventsActions.deleteEventSuccess({event: action.event}))
        .next()
        .call(action.resolveForm)
        .next()
        .isDone()

        .restart()
        .next()
        .throw(error)
        .call([errorReporter, errorReporter.send], 'detachEvent', error)
        .next()
        .put(notifyFailure({error}))
        .next()
        .call(action.rejectForm)
        .next()
        .isDone();
    });
  });

  describe('confirmRepetition', () => {
    test('должен работать', () => {
      const eventsApi = new EventsApi();
      const action = {
        event: {
          id: '100'
        },
        resolve() {},
        reject() {}
      };
      const error = {
        name: 'error'
      };

      testSaga(eventsSagas.confirmRepetition, action)
        .next()
        .getContext('eventsApi')
        .next(eventsApi)
        .call([eventsApi, eventsApi.confirmRepetition], action.event)
        .next()
        .put(eventsActions.confirmRepetitionSuccess({event: action.event}))
        .next()
        .put(notifySuccess({message: 'notifications.repetitionConfirmed'}))
        .next()
        .call(action.resolve)
        .next()
        .isDone()

        .restart()
        .next()
        .throw(error)
        .call([errorReporter, errorReporter.send], 'confirmRepetition', error)
        .next()
        .put(notifyFailure({error}))
        .next()
        .call(action.reject)
        .next()
        .isDone();
    });
  });

  describe('getEventsNetwork', () => {
    describe('успешное выполнение', () => {
      describe('есть включенные слои', () => {
        test('должен делать запрос за событиями', () => {
          const eventsApi = new EventsApi();
          const action = {
            payload: {
              from: '2018-04-26',
              to: '2018-04-27'
            }
          };
          const layersIds = [1, 2];
          const response = {
            events: [],
            lastUpdateTs: Number(moment('2018-01-01'))
          };
          const layerToken = 'pew';

          return expectSaga(eventsSagas.getEventsNetwork, action)
            .provide([
              [matchers.select(getToggledOnLayersIds), layersIds],
              [matchers.select(getPrivateToken), layerToken],
              [matchers.getContext('eventsApi'), eventsApi],
              [matchers.call.fn(eventsApi.getEvents), response]
            ])
            .call([eventsApi, eventsApi.getEvents], {
              from: action.payload.from,
              to: action.payload.to,
              layerId: layersIds,
              layerToken
            })
            .run();
        });

        test('должен записывать полученные события в стейт', () => {
          const eventsApi = new EventsApi();
          const action = {
            payload: {
              from: '2018-04-26',
              to: '2018-04-27'
            }
          };
          const layersIds = [1, 2];
          const response = {
            events: [],
            lastUpdateTs: Number(moment('2018-01-01'))
          };

          return expectSaga(eventsSagas.getEventsNetwork, action)
            .provide([
              [matchers.select(getToggledOnLayersIds), layersIds],
              [matchers.select(getPrivateToken), ''],
              [matchers.getContext('eventsApi'), eventsApi],
              [matchers.call.fn(eventsApi.getEvents), response]
            ])
            .put(
              eventsActions.getEventsSuccess({
                events: response.events,
                lastUpdateTs: response.lastUpdateTs,
                from: action.payload.from,
                to: action.payload.to
              })
            )
            .run();
        });
      });

      describe('нет включенных слоев', () => {
        test('не должен делать запрос за событиями', () => {
          const eventsApi = new EventsApi();
          const action = {
            payload: {
              from: '2018-04-26',
              to: '2018-04-27'
            }
          };
          const layersIds = [];

          return expectSaga(eventsSagas.getEventsNetwork, action)
            .provide([
              [matchers.select(getPrivateToken), ''],
              [matchers.select(getToggledOnLayersIds), layersIds]
            ])
            .not.call([eventsApi, eventsApi.getEvents], {
              from: action.payload.from,
              to: action.payload.to,
              layerId: layersIds
            })
            .run();
        });
      });

      test('должен сигнализировать о завершении', () => {
        const action = {
          payload: {
            from: '2018-04-26',
            to: '2018-04-27'
          }
        };
        const layersIds = [];

        return expectSaga(eventsSagas.getEventsNetwork, action)
          .provide([
            [matchers.select(getPrivateToken), ''],
            [matchers.select(getToggledOnLayersIds), layersIds]
          ])
          .put(eventsActions.getEventsDone({from: action.payload.from, to: action.payload.to}))
          .run();
      });
    });

    describe('неуспешное выполнение', () => {
      test('должен логировать ошибку', () => {
        const action = {
          payload: {
            from: '2018-04-26',
            to: '2018-04-27'
          }
        };

        return expectSaga(eventsSagas.getEventsNetwork, action)
          .provide([
            [matchers.select(getToggledOnLayersIds), throwError({name: 'error'})],
            [matchers.call.fn(errorReporter.send)]
          ])
          .call([errorReporter, errorReporter.send], 'getEventsNetwork', {name: 'error'})
          .run();
      });

      test('должен сигнализировать о завершении', () => {
        const action = {
          payload: {
            from: '2018-04-26',
            to: '2018-04-27'
          }
        };

        return expectSaga(eventsSagas.getEventsNetwork, action)
          .provide([
            [matchers.select(getToggledOnLayersIds), throwError({name: 'error'})],
            [matchers.call.fn(errorReporter.send)]
          ])
          .put(
            eventsActions.getEventsDone({
              error: {name: 'error'},
              from: action.payload.from,
              to: action.payload.to
            })
          )
          .run();
      });
    });
  });

  describe('getEventsByLogin', () => {
    test('должен ходить за событиями', () => {
      const eventsApi = new EventsApi();
      const action = {
        login: 'login',
        email: 'email',
        from: 'date1',
        to: 'date2',
        opaqueOnly: true
      };
      const response = {
        events: [{id: 1}],
        lastUpdateTs: 1
      };

      return expectSaga(getEventsByLogin, action)
        .provide([
          [matchers.getContext('eventsApi'), eventsApi],
          [matchers.call.fn(eventsApi.getEventsByLogin), response]
        ])
        .call([eventsApi, eventsApi.getEventsByLogin], {
          login: 'login',
          email: 'email',
          from: 'date1',
          to: 'date2',
          opaqueOnly: true
        })
        .put(
          eventsActions.getEventsSuccess({
            events: response.events,
            lastUpdateTs: 1,
            from: 'date1',
            to: 'date2',
            ownerId: 'login'
          })
        )
        .run();
    });
    test('должен логировать ошибку', () => {
      const eventsApi = new EventsApi();
      const action = {
        login: 'login',
        from: 'date1',
        to: 'date2'
      };
      const error = {
        message: 'some error'
      };
      return expectSaga(getEventsByLogin, action)
        .provide([
          [matchers.getContext('eventsApi'), eventsApi],
          [matchers.call.fn(eventsApi.getEventsByLogin), throwError(error)],
          [matchers.call.fn(errorReporter.send)]
        ])
        .call([errorReporter, errorReporter.send], 'getEventsByLogin', error)
        .put(notifyFailure({error}))
        .put(eventsActions.getEventsDone({error, from: 'date1', to: 'date2'}))
        .run();
    });
  });

  describe('getEventsForLayer', () => {
    test('должен работать', () => {
      const eventsApi = new EventsApi();
      const action = {
        layerId: 100
      };
      const range = {
        start: Number(moment('2018-05-29')),
        end: Number(moment('2018-05-31'))
      };
      const response = {
        events: []
      };
      const error = {
        name: 'error'
      };

      testSaga(eventsSagas.getEventsForLayer, action)
        .next()
        .getContext('eventsApi')
        .next(eventsApi)
        .select(getLoadingRangeSelector)
        .next(range)
        .call([eventsApi, eventsApi.getEvents], {
          from: range.start,
          to: range.end,
          layerId: action.layerId
        })
        .next(response)
        .put(eventsActions.getEventsForLayerSuccess(response.events))
        .next()
        .isDone()

        .restart()
        .next()
        .throw(error)
        .call([errorReporter, errorReporter.send], 'getEventsForLayer', error)
        .next()
        .isDone();
    });
  });

  describe('getModifiedEvents', () => {
    test('должен работать', () => {
      const eventsApi = new EventsApi();
      const lastUpdateTs = Number(moment('2018-01-01'));
      const range = {
        start: Number(moment('2018-05-29')),
        end: Number(moment('2018-05-31'))
      };
      const response = {
        eventsByExternalId: {},
        eventsByLayerId: {},
        lastUpdateTs: Number(moment('2018-01-02'))
      };
      const error = {
        name: 'error'
      };

      testSaga(eventsSagas.getModifiedEvents)
        .next()
        .getContext('eventsApi')
        .next(eventsApi)
        .select(getLastUpdateTs)
        .next(lastUpdateTs)
        .select(getLoadingRangeSelector)
        .next(range)
        .call([eventsApi, eventsApi.getModifiedEvents], {
          from: range.start,
          to: range.end,
          since: lastUpdateTs
        })
        .next(response)
        .put(eventsActions.getModifiedEventsSuccess(response))
        .next()
        .isDone()

        .restart()
        .next()
        .throw(error)
        .call([errorReporter, errorReporter.send], 'getModifiedEvents', error)
        .next()
        .isDone();
    });
  });

  describe('getEventNetwork', () => {
    test('должен работать', () => {
      const eventsApi = new EventsApi();
      const action = {
        payload: {
          eventId: 100,
          layerId: 2
        }
      };
      const response = {
        id: 100,
        layerId: 2
      };
      const error = {
        name: 'error'
      };

      testSaga(eventsSagas.getEventNetwork, action)
        .next()
        .getContext('eventsApi')
        .next(eventsApi)
        .call([eventsApi, eventsApi.getEvent], {
          eventId: action.payload.eventId,
          layerId: action.payload.layerId,
          instanceStartTs: action.payload.instanceStartTs,
          recurrenceAsOccurrence: action.payload.recurrenceAsOccurrence
        })
        .next(response)
        .put(eventsActions.getEventSuccess(response))
        .next()
        .isDone()

        .restart()
        .next()
        .throw(error)
        .call([errorReporter, errorReporter.send], 'getEventNetwork', error)
        .next()
        .put(eventsActions.getEventFailure(error))
        .next()
        .isDone();
    });
  });

  describe('handleEventInvitation', () => {
    describe('успешное выполнение', () => {
      test('должен получать куку с результатом обработки решения', () => {
        return expectSaga(eventsSagas.handleEventInvitation)
          .provide([[matchers.call.fn(cookies.getJSON)], [matchers.call.fn(cookies.remove)]])
          .call([cookies, cookies.getJSON], 'event_invitation')
          .run();
      });

      describe('есть кука', () => {
        describe('неуспешная обработка решения', () => {
          test('должен показывать оповещение об неуспешной обработки решения', () => {
            return expectSaga(eventsSagas.handleEventInvitation)
              .provide([
                [matchers.call.fn(cookies.getJSON), {error: {name: 'error'}}],
                [matchers.call.fn(cookies.remove)]
              ])
              .put(notifyFailure({error: {name: 'error'}}))
              .run();
          });
        });

        describe('успешная обработка решения', () => {
          test('должен показывать оповещение об успешной обработки решения', () => {
            return expectSaga(eventsSagas.handleEventInvitation)
              .provide([
                [matchers.call.fn(cookies.getJSON), {decision: Decision.YES}],
                [matchers.call.fn(cookies.remove)]
              ])
              .put(notifySuccess({message: i18n.get('notifications', 'decision_yes')}))
              .run();
          });
        });

        test('должен удалять куку', () => {
          return expectSaga(eventsSagas.handleEventInvitation)
            .provide([
              [matchers.call.fn(cookies.getJSON), {decision: Decision.YES}],
              [matchers.call.fn(cookies.remove)]
            ])
            .call([cookies, cookies.remove], 'event_invitation')
            .run();
        });
      });

      describe('нет куки', () => {
        test('не должен показывать оповещение об неуспешной обработки решения', () => {
          return expectSaga(eventsSagas.handleEventInvitation)
            .provide([[matchers.call.fn(cookies.getJSON)], [matchers.call.fn(cookies.remove)]])
            .not.put.actionType(notifyFailure().type)
            .run();
        });

        test('не должен показывать оповещение об успешной обработки решения', () => {
          return expectSaga(eventsSagas.handleEventInvitation)
            .provide([[matchers.call.fn(cookies.getJSON)], [matchers.call.fn(cookies.remove)]])
            .not.put.actionType(notifySuccess().type)
            .run();
        });

        test('не должен удалять куку', () => {
          return expectSaga(eventsSagas.handleEventInvitation)
            .provide([[matchers.call.fn(cookies.getJSON)], [matchers.call.fn(cookies.remove)]])
            .not.call([cookies, cookies.remove], 'event_invitation')
            .run();
        });
      });
    });

    describe('неуспешное выполнение', () => {
      test('должен логировать ошибку', () => {
        return expectSaga(eventsSagas.handleEventInvitation)
          .provide([
            [matchers.call.fn(cookies.getJSON), throwError({name: 'error'})],
            [matchers.call.fn(errorReporter.send)]
          ])
          .call([errorReporter, errorReporter.send], 'handleEventInvitation', {name: 'error'})
          .run();
      });
    });
  });

  describe('getEventsByExternalId', () => {
    test('должен работать', () => {
      const eventsApi = new EventsApi();
      const externalId = ['externalId1', 'externalId2'];
      const range = {
        start: Number(moment('2018-05-29')),
        end: Number(moment('2018-05-31'))
      };
      const response = {
        events: []
      };

      testSaga(eventsSagas.getEventsByExternalId, externalId)
        .next()
        .getContext('eventsApi')
        .next(eventsApi)
        .select(getLoadingRangeSelector)
        .next(range)
        .call([eventsApi, eventsApi.getEvents], {
          from: range.start,
          to: range.end,
          externalId
        })
        .next(response)
        .returns(response);
    });
  });

  describe('tryTurnOnLayer', () => {
    test('должен работать', () => {
      const layerId = 100;

      testSaga(eventsSagas.tryTurnOnLayer, layerId)
        .next()
        .select(getLayerById, layerId)
        .next()
        .isDone()

        .back()
        .next({id: 100, isToggledOn: true})
        .isDone()

        .back()
        .next({id: 100, isToggledOn: false})
        .put(toggleLayer(100, true))
        .next()
        .isDone();
    });
  });

  describe('getRepetitionDescription', () => {
    test('должен получать словесное описание повторений', () => {
      const eventsApi = new EventsApi();
      const action = {
        resolve() {},
        reject() {},
        payload: {
          start: Symbol(),
          repetition: Symbol()
        }
      };
      const response = {
        description: Symbol()
      };
      const error = {
        message: Symbol()
      };

      testSaga(eventsSagas.getRepetitionDescription, action)
        .next()
        .getContext('eventsApi')
        .next(eventsApi)
        .call([eventsApi, eventsApi.getRepetitionDescription], action.payload)
        .next(response)
        .call(action.resolve, response.description)
        .next()
        .isDone()

        .restart()
        .next()
        .throw(error)
        .call(action.reject, error.message)
        .next()
        .call([errorReporter, errorReporter.send], 'getRepetitionDescription', error)
        .next()
        .isDone();
    });
  });

  describe('updateOfflineData', () => {
    test('должен загружать события на ближайшие 3 дня', () => {
      const getEventsAction = Symbol();

      testSaga(eventsSagas.updateOfflineData)
        .next()
        .call(eventsActions.getEventsNetwork, {
          from: moment()
            .startOf('day')
            .valueOf(),
          to: moment()
            .startOf('day')
            .add(3, 'days')
            .valueOf()
        })
        .next(getEventsAction)
        .call(eventsSagas.getEventsNetwork, getEventsAction)
        .next()
        .put(eventsActions.updateOfflineDataSuccess())
        .next()
        .isDone();
    });

    test('должен логировать ошибку и бросать failure', () => {
      const error = Symbol();

      return expectSaga(eventsSagas.updateOfflineData)
        .provide([
          [matchers.call.fn(eventsActions.getEventsNetwork), throwError(error)],
          [matchers.call.fn(errorReporter.send)]
        ])
        .call([errorReporter, errorReporter.send], 'updateOfflineData', error)
        .run();
    });
  });
});
