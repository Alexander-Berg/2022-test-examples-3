import {Map} from 'immutable';

import eventPage from '../eventReducer';
import {ActionTypes} from '../eventsConstants';
import {getAttendeesStart, getAttendeesDone} from '../eventsActions';
import EventRecord from '../EventRecord';

describe('eventReducer', () => {
  test('должен обрабатывать начальное состояние', () => {
    const expectedState = Map({
      request: false,
      error: false,
      isLoadingAttendees: false,
      isLoadingOptionalAttendees: false,
      active: null,
      isUpdatingDecision: new Map()
    });
    expect(eventPage(undefined, {})).toEqual(expectedState);
  });

  test('должен обработать экшен GET_EVENT', () => {
    const state = Map({
      request: false,
      error: false,
      isLoadingAttendees: false,
      active: null,
      isUpdatingDecision: new Map()
    });
    const expectedState = Map({
      request: true,
      error: false,
      isLoadingAttendees: false,
      active: null,
      isUpdatingDecision: new Map()
    });
    const action = {
      type: ActionTypes.GET_EVENT
    };
    expect(eventPage(state, action)).toEqual(expectedState);
  });

  test('должен обработать экшен GET_EVENT_SUCCESS', () => {
    const state = Map({
      request: true,
      error: false,
      isLoadingAttendees: false,
      active: null,
      isUpdatingDecision: new Map()
    });
    const expectedState = Map({
      request: false,
      error: false,
      isLoadingAttendees: false,
      active: null,
      isUpdatingDecision: new Map()
    });
    const action = {
      type: ActionTypes.GET_EVENT_SUCCESS
    };
    expect(eventPage(state, action)).toEqual(expectedState);
  });

  test('должен обработать экшен GET_EVENT_FAILURE', () => {
    const state = Map({
      request: true,
      error: false,
      isLoadingAttendees: false,
      active: null,
      isUpdatingDecision: new Map()
    });
    const error = {
      message: 'not found',
      code: 404
    };
    const expectedState = Map({
      request: false,
      error,
      isLoadingAttendees: false,
      active: null,
      isUpdatingDecision: new Map()
    });
    const action = {
      type: ActionTypes.GET_EVENT_FAILURE,
      error
    };
    expect(eventPage(state, action)).toEqual(expectedState);
  });

  test('должен обработать экшен getAttendeesStart', () => {
    const state = Map({
      request: false,
      error: false,
      isLoadingAttendees: false,
      active: null,
      isUpdatingDecision: new Map()
    });
    const expectedState = Map({
      request: false,
      error: false,
      isLoadingAttendees: true,
      active: null,
      isUpdatingDecision: new Map()
    });
    const action = {
      type: getAttendeesStart.type
    };
    expect(eventPage(state, action)).toEqual(expectedState);
  });

  test('должен обработать экшен getAttendeesDone', () => {
    const state = Map({
      request: false,
      error: false,
      isLoadingAttendees: true,
      active: null,
      isUpdatingDecision: new Map()
    });
    const expectedState = Map({
      request: false,
      error: false,
      isLoadingAttendees: false,
      active: null,
      isUpdatingDecision: new Map()
    });
    const action = {
      type: getAttendeesDone.type
    };
    expect(eventPage(state, action)).toEqual(expectedState);
  });

  test('должен обработать экшен setActiveEvent', () => {
    const state = Map({
      request: false,
      error: false,
      isLoadingAttendees: false,
      active: null,
      isUpdatingDecision: new Map()
    });
    const expectedState = Map({
      request: false,
      error: false,
      isLoadingAttendees: false,
      active: 'someuuid',
      isUpdatingDecision: new Map()
    });
    const action = {
      type: ActionTypes.SET_ACTIVE_EVENT,
      event: {
        uuid: 'someuuid'
      }
    };
    expect(eventPage(state, action)).toEqual(expectedState);
  });

  test('должен обработать экшен unsetActiveEvent', () => {
    const state = Map({
      request: false,
      error: false,
      isLoadingAttendees: false,
      active: 'someuuid',
      isUpdatingDecision: new Map()
    });
    const expectedState = Map({
      request: false,
      error: false,
      isLoadingAttendees: false,
      active: null,
      isUpdatingDecision: new Map()
    });
    const action = {
      type: ActionTypes.UNSET_ACTIVE_EVENT
    };
    expect(eventPage(state, action)).toEqual(expectedState);
  });

  test('должен обработать экшн UPDATE_DECISION', () => {
    const state = Map({
      request: false,
      error: false,
      isLoadingAttendees: false,
      active: null,
      isUpdatingDecision: new Map()
    });

    const event = new EventRecord({});

    const expectedState = Map({
      request: false,
      error: false,
      isLoadingAttendees: false,
      active: null,
      isUpdatingDecision: new Map({[event.uuid]: true})
    });

    const action = {
      type: ActionTypes.UPDATE_DECISION,
      event
    };

    expect(eventPage(state, action)).toEqual(expectedState);
  });

  test('должен обработать экшн UPDATE_DECISION_DONE', () => {
    const event = new EventRecord({});

    const state = Map({
      request: false,
      error: false,
      isLoadingAttendees: false,
      active: null,
      isUpdatingDecision: new Map({[event.uuid]: true})
    });

    const expectedState = Map({
      request: false,
      error: false,
      isLoadingAttendees: false,
      active: null,
      isUpdatingDecision: new Map({})
    });

    const action = {
      type: ActionTypes.UPDATE_DECISION_DONE,
      event
    };

    expect(eventPage(state, action)).toEqual(expectedState);
  });
});
