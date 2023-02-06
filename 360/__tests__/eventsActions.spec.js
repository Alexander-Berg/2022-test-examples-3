import moment from 'moment';

import {login} from 'configs/session';
import Decision from 'constants/Decision';
import createActionMetaInfo from 'middlewares/offlineMiddleware/utils/createActionMetaInfo';
import EventFormId from 'features/eventForm/EventFormId';

import {ActionTypes} from '../eventsConstants';
import * as actions from '../eventsActions';

describe('eventsActions', () => {
  describe('createEvent', () => {
    test('должен вернуть экшен CREATE_EVENT', () => {
      const params = {
        values: {name: 'name'},
        resolveForm() {},
        rejectForm() {},
        officesToStartBooking: []
      };
      expect(actions.createEvent(params)).toEqual({
        type: ActionTypes.CREATE_EVENT,
        values: params.values,
        resolveForm: params.resolveForm,
        rejectForm: params.rejectForm,
        officesToStartBooking: []
      });
    });
  });

  describe('createEventSuccess', () => {
    test('должен вернуть экшен CREATE_EVENT_SUCCESS', () => {
      expect(actions.createEventSuccess([])).toEqual({
        type: ActionTypes.CREATE_EVENT_SUCCESS,
        events: []
      });
    });
  });

  describe('updateEvent', () => {
    test('должен вернуть экшен UPDATE_EVENT', () => {
      const params = {
        oldEvent: {},
        newEvent: {},
        mailToAll: false,
        applyToFuture: false,
        resolveForm() {},
        rejectForm() {},
        officesToStartBooking: []
      };
      expect(actions.updateEvent(params)).toEqual({
        type: ActionTypes.UPDATE_EVENT,
        oldEvent: params.oldEvent,
        newEvent: params.newEvent,
        mailToAll: params.mailToAll,
        applyToFuture: params.applyToFuture,
        resolveForm: params.resolveForm,
        rejectForm: params.rejectForm,
        officesToStartBooking: []
      });
    });
  });

  describe('dropEvent', () => {
    test('должен вернуть экшен DROP_EVENT', () => {
      const params = {
        oldEvent: {},
        newEvent: {}
      };
      expect(actions.dropEvent(params)).toEqual({
        type: ActionTypes.DROP_EVENT,
        oldEvent: params.oldEvent,
        newEvent: params.newEvent
      });
    });
  });

  describe('updateEventSuccess', () => {
    test('должен вернуть экшен UPDATE_EVENT_SUCCESS', () => {
      const params = {
        oldEvent: {},
        newEvents: [],
        applyToFuture: true
      };
      expect(actions.updateEventSuccess(params)).toEqual({
        type: ActionTypes.UPDATE_EVENT_SUCCESS,
        oldEvent: params.oldEvent,
        newEvents: params.newEvents,
        applyToFuture: true
      });
    });
  });

  describe('updateDecision', () => {
    test('должен вернуть экшен UPDATE_DECISION', () => {
      const params = {
        event: {},
        decision: Decision.MAYBE,
        reason: '',
        applyToAll: false,
        resolve() {}
      };
      const form = EventFormId.fromParams(
        EventFormId.VIEWS.POPUP,
        EventFormId.MODES.CREATE
      ).toString();
      expect(actions.makeUpdateDecision({form})(params)).toEqual({
        type: ActionTypes.UPDATE_DECISION,
        event: params.event,
        decision: params.decision,
        reason: params.reason,
        applyToAll: params.applyToAll,
        resolve: params.resolve,
        meta: {form}
      });
    });
  });

  describe('updateDecisionSuccess', () => {
    test('должен вернуть экшен UPDATE_DECISION_SUCCESS', () => {
      const params = {
        event: {},
        decision: Decision.MAYBE,
        myEmail: 'email@email.com',
        applyToAll: true
      };
      const form = EventFormId.fromParams(
        EventFormId.VIEWS.POPUP,
        EventFormId.MODES.CREATE
      ).toString();
      expect(actions.makeUpdateDecisionSuccess({form})(params)).toEqual({
        type: ActionTypes.UPDATE_DECISION_SUCCESS,
        event: params.event,
        decision: params.decision,
        myEmail: params.myEmail,
        applyToAll: params.applyToAll,
        meta: {form}
      });
    });
  });

  describe('updateDecisionDone', () => {
    test('должен вернуть экшен UPDATE_DECISION_DONE', () => {
      const params = {
        event: {}
      };
      expect(actions.updateDecisionDone(params)).toEqual({
        type: ActionTypes.UPDATE_DECISION_DONE,
        event: params.event
      });
    });
  });

  describe('updateEventHovered', () => {
    test('должен вернуть экшен UPDATE_EVENT_HOVERED', () => {
      const params = {
        event: {},
        hovered: false
      };
      expect(actions.updateEventHovered(params)).toEqual({
        type: ActionTypes.UPDATE_EVENT_HOVERED,
        event: params.event,
        hovered: params.hovered
      });
    });
  });

  describe('updateEventActivated', () => {
    test('должен вернуть экшен UPDATE_EVENT_ACTIVATED', () => {
      const params = {
        event: {},
        activated: false
      };
      expect(actions.updateEventActivated(params)).toEqual({
        type: ActionTypes.UPDATE_EVENT_ACTIVATED,
        event: params.event,
        activated: params.activated
      });
    });
  });

  describe('confirmRepetition', () => {
    test('должен вернуть экшен CONFIRM_REPETITION', () => {
      const params = {
        event: {},
        resolve() {},
        reject() {}
      };
      expect(actions.confirmRepetition(params)).toEqual({
        type: ActionTypes.CONFIRM_REPETITION,
        event: params.event,
        resolve: params.resolve,
        reject: params.reject
      });
    });
  });

  describe('confirmRepetitionSuccess', () => {
    test('должен вернуть экшен CONFIRM_REPETITION_SUCCESS', () => {
      const params = {
        event: {}
      };
      expect(actions.confirmRepetitionSuccess(params)).toEqual({
        type: ActionTypes.CONFIRM_REPETITION_SUCCESS,
        event: params.event
      });
    });
  });

  describe('deleteEvent', () => {
    test('должен вернуть экшен DELETE_EVENT', () => {
      const params = {
        event: {},
        applyToFuture: false,
        resolveForm() {},
        rejectForm() {}
      };
      expect(actions.deleteEvent(params)).toEqual({
        type: ActionTypes.DELETE_EVENT,
        event: params.event,
        applyToFuture: params.applyToFuture,
        resolveForm: params.resolveForm,
        rejectForm: params.rejectForm
      });
    });
  });

  describe('deleteEventSuccess', () => {
    test('должен вернуть экшен DELETE_EVENT_SUCCESS', () => {
      const params = {
        event: {},
        newEvents: []
      };
      expect(actions.deleteEventSuccess(params)).toEqual({
        type: ActionTypes.DELETE_EVENT_SUCCESS,
        event: params.event,
        newEvents: params.newEvents
      });
    });
  });

  describe('detachEvent', () => {
    test('должен вернуть экшен DETACH_EVENT', () => {
      const params = {
        event: {},
        resolveForm() {},
        rejectForm() {}
      };
      expect(actions.detachEvent(params)).toEqual({
        type: ActionTypes.DETACH_EVENT,
        event: params.event,
        resolveForm: params.resolveForm,
        rejectForm: params.rejectForm
      });
    });
  });

  describe('getEvents', () => {
    test('должен вернуть экшен GET_EVENTS', () => {
      const from = 'from';
      const to = 'to';

      expect(actions.getEvents({from, to})).toEqual({
        type: ActionTypes.GET_EVENTS,
        payload: {
          from,
          to
        },
        meta: createActionMetaInfo({
          network: actions.getEventsNetwork({from, to}),
          rollback: actions.getEventsOffline({from, to})
        })
      });
    });
  });

  describe('getEventsDone', () => {
    test('должен вернуть экшен GET_EVENTS_DONE', () => {
      const payload = {
        from: 'from',
        to: 'to'
      };

      expect(actions.getEventsDone(payload)).toEqual({
        type: ActionTypes.GET_EVENTS_DONE,
        payload
      });
    });
  });

  describe('getEventsSuccess', () => {
    test('должен вернуть экшен GET_EVENTS_SUCCESS', () => {
      const params = {
        events: [],
        lastUpdateTs: Number(moment('2018-01-01T10:00')),
        from: '2018-01-01',
        to: '2018-01-07'
      };
      expect(actions.getEventsSuccess(params)).toEqual({
        type: ActionTypes.GET_EVENTS_SUCCESS,
        events: params.events,
        lastUpdateTs: params.lastUpdateTs,
        from: params.from,
        to: params.to,
        ownerId: login
      });
    });

    test('должен вернуть экшен GET_EVENTS_SUCCESS с переданным ownerId', () => {
      const params = {
        events: [],
        lastUpdateTs: Number(moment('2018-01-01T10:00')),
        from: '2018-01-01',
        to: '2018-01-07',
        ownerId: 'pistch'
      };
      expect(actions.getEventsSuccess(params)).toEqual({
        type: ActionTypes.GET_EVENTS_SUCCESS,
        events: params.events,
        lastUpdateTs: params.lastUpdateTs,
        from: params.from,
        to: params.to,
        ownerId: 'pistch'
      });
    });
  });

  describe('getEventsForLayer', () => {
    test('должен вернуть экшен GET_EVENTS_FOR_LAYER', () => {
      expect(actions.getEventsForLayer(100)).toEqual({
        type: ActionTypes.GET_EVENTS_FOR_LAYER,
        layerId: 100
      });
    });
  });

  describe('getEventsForLayerSuccess', () => {
    test('должен вернуть экшен GET_EVENTS_FOR_LAYER_SUCCESS', () => {
      expect(actions.getEventsForLayerSuccess([])).toEqual({
        type: ActionTypes.GET_EVENTS_FOR_LAYER_SUCCESS,
        events: []
      });
    });
  });

  describe('getModifiedEvents', () => {
    test('должен вернуть экшен GET_MODIFIED_EVENTS', () => {
      expect(actions.getModifiedEvents()).toEqual({
        type: ActionTypes.GET_MODIFIED_EVENTS
      });
    });
  });

  describe('getModifiedEventsSuccess', () => {
    test('должен вернуть экшен GET_MODIFIED_EVENTS_SUCCESS', () => {
      const params = {
        eventsByExternalId: {},
        eventsByLayerId: {},
        lastUpdateTs: Number(moment('2018-01-01T10:00'))
      };
      expect(actions.getModifiedEventsSuccess(params)).toEqual({
        type: ActionTypes.GET_MODIFIED_EVENTS_SUCCESS,
        eventsByExternalId: params.eventsByExternalId,
        eventsByLayerId: params.eventsByLayerId,
        lastUpdateTs: params.lastUpdateTs
      });
    });
  });

  describe('getEvent', () => {
    test('должен вернуть экшен GET_EVENT', () => {
      const params = {
        eventId: 100,
        layerId: 2
      };

      expect(actions.getEvent(params)).toEqual({
        type: ActionTypes.GET_EVENT,
        payload: {
          eventId: params.eventId,
          layerId: params.layerId,
          instanceStartTs: params.instanceStartTs,
          recurrenceAsOccurrence: params.recurrenceAsOccurrence
        },
        meta: createActionMetaInfo({
          network: actions.getEventNetwork(params),
          rollback: actions.getEventOffline(params)
        })
      });
    });
  });

  describe('getEventSuccess', () => {
    test('должен вернуть экшен GET_EVENT_SUCCESS', () => {
      const event = {
        id: 100
      };

      expect(actions.getEventSuccess(event)).toEqual({
        type: ActionTypes.GET_EVENT_SUCCESS,
        event
      });
    });
  });

  describe('getEventFailure', () => {
    test('должен вернуть экшен GET_EVENT_FAILURE', () => {
      const error = {
        message: 'error'
      };

      expect(actions.getEventFailure(error)).toEqual({
        type: ActionTypes.GET_EVENT_FAILURE,
        error
      });
    });
  });

  describe('setActiveEvent', () => {
    test('должен вернуть экшен SET_ACTIVE_EVENT', () => {
      const event = {
        uuid: 'someuuid'
      };

      expect(actions.setActiveEvent(event)).toEqual({
        type: ActionTypes.SET_ACTIVE_EVENT,
        event
      });
    });
  });

  describe('unsetActiveEvent', () => {
    test('должен вернуть экшен UNSET_ACTIVE_EVENT', () => {
      expect(actions.unsetActiveEvent()).toEqual({
        type: ActionTypes.UNSET_ACTIVE_EVENT
      });
    });
  });
});
