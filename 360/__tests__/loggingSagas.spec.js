import {expectSaga} from 'redux-saga-test-plan';
import {throwError} from 'redux-saga-test-plan/providers';
import {call, getContext} from 'redux-saga-test-plan/matchers';
import {takeEvery} from 'redux-saga/effects';

import SagaErrorReporter from 'utils/SagaErrorReporter';
import {ActionTypes as EventsActionTypes} from 'features/events/eventsConstants';

import rootSaga, {
  aceventuraSuggestReport,
  logCreateEvent,
  logDeleteEvent,
  logUpdateEvent
} from '../loggingSagas';
import {LoggingApi, LoggingModelsApi} from '../LoggingApi';
import * as actions from '../loggingActions';

const errorReporter = new SagaErrorReporter('logging');

describe('loggingSagas', () => {
  describe('rootSaga', () => {
    test('должен записывать LoggingApi в контекст', () => {
      const api = {};

      return expectSaga(rootSaga)
        .provide([[getContext('api'), api]])
        .setContext({
          loggingApi: new LoggingApi(window.location.origin),
          loggingModelsApi: new LoggingModelsApi(api)
        })
        .silentRun(0);
    });

    test('должен подписываться на экшны', async () => {
      const {effects} = await expectSaga(rootSaga)
        .provide([[getContext('api'), {}]])
        .silentRun(0);

      expect(effects.fork).toEqual([
        takeEvery(actions.suggestReport.type, aceventuraSuggestReport),
        takeEvery(EventsActionTypes.CREATE_EVENT_SUCCESS, logCreateEvent),
        takeEvery(actions.logDeleteEvent.type, logDeleteEvent),
        takeEvery(EventsActionTypes.UPDATE_EVENT_SUCCESS, logUpdateEvent)
      ]);
    });
  });

  describe('aceventuraSuggestReport', () => {
    test('должен отправлять статистику выбора из саджеста', () => {
      const data = Symbol();
      const action = {payload: data};
      const api = new LoggingModelsApi();

      return expectSaga(aceventuraSuggestReport, action)
        .provide([
          [getContext('loggingModelsApi'), api],
          [call([api, api.aceventuraSuggestReport], data), {}]
        ])
        .call([api, api.aceventuraSuggestReport], data)
        .run();
    });

    test('должен репортить ошибку', () => {
      const data = Symbol();
      const action = {payload: data};
      const api = new LoggingModelsApi();
      const error = Symbol();

      return expectSaga(aceventuraSuggestReport, action)
        .provide([
          [getContext('loggingModelsApi'), api],
          [call([api, api.aceventuraSuggestReport], data), throwError(error)],
          [call.fn(errorReporter.send)]
        ])
        .call([errorReporter, errorReporter.send], 'aceventuraSuggestReport', error)
        .run();
    });
  });

  describe('logCreateEvent', () => {
    test('должен логировать успешное создание события', () => {
      const eventId = 123456;
      const action = {events: [{id: eventId}]};
      const api = new LoggingApi();

      return expectSaga(logCreateEvent, action)
        .provide([
          [getContext('loggingApi'), api],
          [call([api, api.logCreateEvent], {eventId}), {}]
        ])
        .call([api, api.logCreateEvent], {eventId})
        .run();
    });

    test('должен репортить ошибку', () => {
      const eventId = 123456;
      const action = {events: [{id: eventId}]};
      const api = new LoggingApi();
      const error = Symbol();

      return expectSaga(logCreateEvent, action)
        .provide([
          [getContext('loggingApi'), api],
          [call([api, api.logCreateEvent], {eventId}), throwError(error)],
          [call.fn(errorReporter.send)]
        ])
        .call([errorReporter, errorReporter.send], 'logCreateEvent', error)
        .run();
    });
  });

  describe('logDeleteEvent', () => {
    test('должен логировать успешное создание события', () => {
      const eventId = 123456;
      const instanceStartTs = Date.now();
      const payload = {eventId, instanceStartTs};
      const action = {payload};
      const api = new LoggingApi();

      return expectSaga(logDeleteEvent, action)
        .provide([[getContext('loggingApi'), api], [call([api, api.logDeleteEvent], payload), {}]])
        .call([api, api.logDeleteEvent], payload)
        .run();
    });

    test('должен репортить ошибку', () => {
      const eventId = 123456;
      const instanceStartTs = Date.now();
      const payload = {eventId, instanceStartTs};
      const action = {payload};
      const api = new LoggingApi();
      const error = Symbol();

      return expectSaga(logDeleteEvent, action)
        .provide([
          [getContext('loggingApi'), api],
          [call([api, api.logDeleteEvent], payload), throwError(error)],
          [call.fn(errorReporter.send)]
        ])
        .call([errorReporter, errorReporter.send], 'logDeleteEvent', error)
        .run();
    });
  });

  describe('logUpdateEvent', () => {
    test('должен логировать успешное создание события', () => {
      const eventId = 123456;
      const instanceStartTs = Date.now();
      const action = {newEvents: [{id: eventId, instanceStartTs}], applyToFuture: true};
      const api = new LoggingApi();

      return expectSaga(logUpdateEvent, action)
        .provide([
          [getContext('loggingApi'), api],
          [call([api, api.logUpdateEvent], {eventId, instanceStartTs: null}), {}]
        ])
        .call([api, api.logUpdateEvent], {eventId, instanceStartTs: null})
        .run();
    });

    test('должен передавать instanceStartTs только при applyToFuture = false', () => {
      const eventId = 123456;
      const instanceStartTs = Date.now();
      const action = {newEvents: [{id: eventId, instanceStartTs}], applyToFuture: false};
      const api = new LoggingApi();

      return expectSaga(logUpdateEvent, action)
        .provide([
          [getContext('loggingApi'), api],
          [call([api, api.logUpdateEvent], {eventId, instanceStartTs}), {}]
        ])
        .call([api, api.logUpdateEvent], {eventId, instanceStartTs})
        .run();
    });

    test('должен репортить ошибку', () => {
      const eventId = 123456;
      const instanceStartTs = Date.now();
      const action = {newEvents: [{id: eventId, instanceStartTs}], applyToFuture: false};
      const api = new LoggingApi();
      const error = Symbol();

      return expectSaga(logUpdateEvent, action)
        .provide([
          [getContext('loggingApi'), api],
          [call([api, api.logUpdateEvent], {eventId, instanceStartTs}), throwError(error)],
          [call.fn(errorReporter.send)]
        ])
        .call([errorReporter, errorReporter.send], 'logUpdateEvent', error)
        .run();
    });
  });
});
