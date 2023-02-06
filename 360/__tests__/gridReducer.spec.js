import moment from 'moment';
import {LOCATION_CHANGE} from 'connected-react-router';

import {ActionTypes as SettingsActionTypes} from 'features/settings/settingsConstants';

import {ActionTypes} from '../gridConstants';
import GridRecord from '../GridRecord';
import createGridReducer, {createInitialState} from '../gridReducer';

describe('gridReducer', () => {
  test('должен обрабатывать начальное состояние', () => {
    expect(createGridReducer()(undefined, {})).toEqual(createInitialState());
  });

  describe('SET_EVENTS_LOADING', () => {
    test('должен устанавливать флаг загрузки событий', () => {
      const action = {
        type: ActionTypes.SET_EVENTS_LOADING,
        payload: true
      };
      const state = new GridRecord({
        eventsLoading: false
      });

      const expectedState = new GridRecord({
        eventsLoading: true
      });

      expect(createGridReducer()(state, action)).toEqual(expectedState);
    });
  });

  describe('SET_TODOS_LOADING', () => {
    test('должен устанавливать флаг загрузки дел', () => {
      const action = {
        type: ActionTypes.SET_TODOS_LOADING,
        payload: true
      };
      const state = new GridRecord({
        todosLoading: false
      });

      const expectedState = new GridRecord({
        todosLoading: true
      });

      expect(createGridReducer()(state, action)).toEqual(expectedState);
    });
  });

  describe('LOCATION_CHANGE', () => {
    test('должен переключать сетку на дневной вид', () => {
      const action = {
        type: LOCATION_CHANGE,
        payload: {
          location: {
            search: 'show_date=2017-08-10',
            pathname: '/day'
          }
        }
      };
      const state = new GridRecord({
        currentView: 'week',
        showDate: Number(moment('2017-08-10'))
      });

      const expectedState = new GridRecord({
        currentView: 'day',
        showDate: Number(moment('2017-08-10'))
      });

      expect(createGridReducer()(state, action)).toEqual(expectedState);
    });

    test('должен переключать сетку на недельный вид', () => {
      const action = {
        type: LOCATION_CHANGE,
        payload: {
          location: {
            search: 'show_date=2017-08-10',
            pathname: '/week'
          }
        }
      };
      const state = new GridRecord({
        currentView: 'day',
        showDate: Number(moment('2017-08-10'))
      });

      const expectedState = new GridRecord({
        currentView: 'week',
        showDate: Number(moment('2017-08-10'))
      });

      expect(createGridReducer()(state, action)).toEqual(expectedState);
    });

    test('должен переключать сетку на месячный вид', () => {
      const action = {
        type: LOCATION_CHANGE,
        payload: {
          location: {
            search: 'show_date=2017-08-10',
            pathname: '/month'
          }
        }
      };
      const state = new GridRecord({
        currentView: 'week',
        showDate: Number(moment('2017-08-10'))
      });

      const expectedState = new GridRecord({
        currentView: 'month',
        showDate: Number(moment('2017-08-10'))
      });

      expect(createGridReducer()(state, action)).toEqual(expectedState);
    });

    test('не должен переключать сетку, если перешли не на сетку', () => {
      const action = {
        type: LOCATION_CHANGE,
        payload: {
          location: {
            search: 'show_date=2017-08-10',
            pathname: '/event'
          }
        }
      };
      const state = new GridRecord({
        currentView: 'week',
        showDate: Number(moment('2017-08-10'))
      });

      const expectedState = new GridRecord({
        currentView: 'week',
        showDate: Number(moment('2017-08-10'))
      });

      expect(createGridReducer()(state, action)).toEqual(expectedState);
    });

    test('должен изменять дату начала сетки', () => {
      const action = {
        type: LOCATION_CHANGE,
        payload: {
          location: {
            search: 'show_date=2017-08-10',
            pathname: '/week'
          }
        }
      };
      const state = new GridRecord({
        currentView: 'week',
        showDate: Number(moment('2017-08-11'))
      });

      const expectedState = new GridRecord({
        currentView: 'week',
        showDate: Number(moment('2017-08-10'))
      });

      expect(createGridReducer()(state, action)).toEqual(expectedState);
    });
  });

  describe('UPDATE_SETTINGS_SUCCESS', () => {
    test('не должен переключать вид сетки, если не меняли дефолтный вид', () => {
      const action = {
        type: SettingsActionTypes.UPDATE_SETTINGS_SUCCESS,
        newSettings: {}
      };
      const state = new GridRecord({
        currentView: 'week',
        showDate: Number(moment('2017-08-10'))
      });

      const expectedState = new GridRecord({
        currentView: 'week',
        showDate: Number(moment('2017-08-10'))
      });

      expect(createGridReducer()(state, action)).toEqual(expectedState);
    });

    test('не должен переключать вид сетки, если дефолтный вид равен текущему', () => {
      const action = {
        type: SettingsActionTypes.UPDATE_SETTINGS_SUCCESS,
        newSettings: {
          defaultView: 'week'
        }
      };
      const state = new GridRecord({
        currentView: 'week',
        showDate: Number(moment('2017-08-10'))
      });

      const expectedState = new GridRecord({
        currentView: 'week',
        showDate: Number(moment('2017-08-10'))
      });

      expect(createGridReducer()(state, action)).toEqual(expectedState);
    });

    test('должен переключать вид сетки, если поменяли дефолтный вид и он не равен текущему', () => {
      const action = {
        type: SettingsActionTypes.UPDATE_SETTINGS_SUCCESS,
        newSettings: {
          defaultView: 'day'
        }
      };
      const state = new GridRecord({
        currentView: 'week',
        showDate: Number(moment('2017-08-10'))
      });

      const expectedState = new GridRecord({
        currentView: 'day',
        showDate: Number(moment('2017-08-10'))
      });

      expect(createGridReducer()(state, action)).toEqual(expectedState);
    });
  });
});
