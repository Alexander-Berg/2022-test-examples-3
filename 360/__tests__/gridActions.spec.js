import {ActionTypes} from '../gridConstants';
import {
  loadEvents,
  loadEventsInBackground,
  loadTodos,
  setEventsLoading,
  setTodosLoading
} from '../gridActions';

describe('gridActions', () => {
  describe('loadEvents', () => {
    test('должен вернуть экшен LOAD_EVENTS', () => {
      expect(loadEvents()).toEqual({
        type: ActionTypes.LOAD_EVENTS
      });
    });
  });

  describe('loadEventsInBackground', () => {
    test('должен вернуть экшен LOAD_EVENTS_IN_BACKGROUND', () => {
      expect(loadEventsInBackground()).toEqual({
        type: ActionTypes.LOAD_EVENTS_IN_BACKGROUND
      });
    });
  });

  describe('loadTodos', () => {
    test('должен вернуть экшен LOAD_TODOS', () => {
      expect(loadTodos()).toEqual({
        type: ActionTypes.LOAD_TODOS
      });
    });
  });

  describe('setEventsLoading', () => {
    test('должен вернуть экшен SET_EVENTS_LOADING', () => {
      expect(setEventsLoading(true)).toEqual({
        type: ActionTypes.SET_EVENTS_LOADING,
        payload: true
      });
    });
  });

  describe('setTodosLoading', () => {
    test('должен вернуть экшен SET_TODOS_LOADING', () => {
      expect(setTodosLoading(true)).toEqual({
        type: ActionTypes.SET_TODOS_LOADING,
        payload: true
      });
    });
  });
});
