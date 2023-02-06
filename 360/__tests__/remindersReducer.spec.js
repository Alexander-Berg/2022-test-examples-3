import {Map} from 'immutable';

import {ActionTypes as TodoActionTypes} from 'features/todo/todoConstants';

import {ActionTypes} from '../remindersConstants';
import ReminderRecord from '../ReminderRecord';
import remindersReducer from '../remindersReducer';

describe('remindersReducer', () => {
  test('должен обрабатывать начальное состояние', () => {
    expect(remindersReducer(undefined, {})).toEqual(new Map());
  });

  describe('LOAD_SIDEBAR_TODOS_SUCCESS', () => {
    test('не должен обновлять список напоминаний, если их нет', () => {
      const state = new Map();
      const expectedState = new Map();
      const action = {
        type: TodoActionTypes.LOAD_SIDEBAR_TODOS_SUCCESS,
        data: {}
      };

      expect(remindersReducer(state, action)).toEqual(expectedState);
    });

    test('должен обновлять список напоминаний, если они есть', () => {
      const state = new Map();
      const expectedState = new Map({
        '1': new ReminderRecord({
          id: '1',
          name: 'reminder 1'
        }),
        '2': new ReminderRecord({
          id: '2',
          name: 'reminder 2'
        })
      });
      const action = {
        type: TodoActionTypes.LOAD_SIDEBAR_TODOS_SUCCESS,
        data: {
          reminders: [
            {
              id: '1',
              name: 'reminder 1'
            },
            {
              id: '2',
              name: 'reminder 2'
            }
          ]
        }
      };

      expect(remindersReducer(state, action)).toEqual(expectedState);
    });
  });

  describe('UPDATE_REMINDER_SUCCESS', () => {
    test('должен обновлять напоминание', () => {
      const state = new Map({
        '1': new ReminderRecord({
          id: '1',
          name: 'reminder',
          reminderDate: '2018-02-28T19:00:00+03:00'
        })
      });
      const expectedState = new Map({
        '1': new ReminderRecord({
          id: '1',
          name: 'updated reminder',
          reminderDate: '2018-02-28T20:00:00+03:00'
        })
      });
      const action = {
        type: ActionTypes.UPDATE_REMINDER_SUCCESS,
        payload: {
          newReminder: {
            id: '1',
            name: 'updated reminder',
            reminderDate: '2018-02-28T20:00:00+03:00'
          }
        }
      };

      expect(remindersReducer(state, action)).toEqual(expectedState);
    });
  });

  describe('UPDATE_REMINDER_FAILURE', () => {
    test('должен возвращать старые данные напоминания', () => {
      const state = new Map({
        '1': new ReminderRecord({
          id: '1',
          name: 'updated reminder',
          reminderDate: '2018-02-28T20:00:00+03:00'
        })
      });
      const expectedState = new Map({
        '1': new ReminderRecord({
          id: '1',
          name: 'reminder',
          reminderDate: '2018-02-28T19:00:00+03:00'
        })
      });
      const action = {
        type: ActionTypes.UPDATE_REMINDER_FAILURE,
        payload: {
          oldReminder: {
            id: '1',
            name: 'reminder',
            reminderDate: '2018-02-28T19:00:00+03:00'
          }
        }
      };

      expect(remindersReducer(state, action)).toEqual(expectedState);
    });
  });

  describe('DELETE_REMINDER_SUCCESS', () => {
    test('должен удалять напоминание', () => {
      const state = new Map({
        '1': new ReminderRecord({
          id: '1',
          name: 'reminder',
          reminderDate: '2018-02-28T19:00:00+03:00'
        })
      });
      const expectedState = new Map();
      const action = {
        type: ActionTypes.DELETE_REMINDER_SUCCESS,
        payload: {
          id: '1'
        }
      };

      expect(remindersReducer(state, action)).toEqual(expectedState);
    });
  });
});
