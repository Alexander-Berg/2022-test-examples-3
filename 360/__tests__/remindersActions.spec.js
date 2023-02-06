import {ActionTypes} from '../remindersConstants';
import {
  updateReminder,
  updateReminderSuccess,
  updateReminderFailure,
  deleteReminder,
  deleteReminderSuccess
} from '../remindersActions';

describe('remindersActions', () => {
  describe('updateReminder', () => {
    test('должен вернуть экшен UPDATE_REMINDER', () => {
      const payload = {};

      expect(updateReminder(payload)).toEqual({
        type: ActionTypes.UPDATE_REMINDER,
        payload
      });
    });
  });

  describe('updateReminderSuccess', () => {
    test('должен вернуть экшен UPDATE_REMINDER_SUCCESS', () => {
      const payload = {};

      expect(updateReminderSuccess(payload)).toEqual({
        type: ActionTypes.UPDATE_REMINDER_SUCCESS,
        payload
      });
    });
  });

  describe('updateReminderFailure', () => {
    test('должен вернуть экшен UPDATE_REMINDER_FAILURE', () => {
      const payload = {};

      expect(updateReminderFailure(payload)).toEqual({
        type: ActionTypes.UPDATE_REMINDER_FAILURE,
        payload
      });
    });
  });

  describe('deleteReminder', () => {
    test('должен вернуть экшен DELETE_REMINDER', () => {
      const payload = {};

      expect(deleteReminder(payload)).toEqual({
        type: ActionTypes.DELETE_REMINDER,
        payload
      });
    });
  });

  describe('deleteReminderSuccess', () => {
    test('должен вернуть экшен DELETE_REMINDER_SUCCESS', () => {
      const payload = {};

      expect(deleteReminderSuccess(payload)).toEqual({
        type: ActionTypes.DELETE_REMINDER_SUCCESS,
        payload
      });
    });
  });
});
