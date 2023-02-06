import {takeEvery} from 'redux-saga/effects';
import {testSaga} from 'redux-saga-test-plan';

import SagaErrorReporter from 'utils/SagaErrorReporter';
import {notificationHelpers, notifyFailure} from 'features/notifications/notificationsActions';

import RemindersApi from '../RemindersApi';
import {ActionTypes} from '../remindersConstants';
import rootSaga, {updateReminder, deleteReminder} from '../remindersSagas';
import * as actions from '../remindersActions';

const errorReporter = new SagaErrorReporter('reminders');

describe('remindersSagas', () => {
  beforeEach(() => {
    jest.spyOn(notificationHelpers, 'generateId').mockReturnValue('id');
  });

  describe('rootSaga', () => {
    test('должен работать', () => {
      testSaga(rootSaga)
        .next()
        .getContext('api')
        .next()
        .setContext({remindersApi: new RemindersApi()})
        .next()
        .all([
          takeEvery(ActionTypes.UPDATE_REMINDER, updateReminder),
          takeEvery(ActionTypes.DELETE_REMINDER, deleteReminder)
        ])
        .next()
        .isDone();
    });
  });

  describe('updateReminder', () => {
    test('должен работать', () => {
      const remindersApi = new RemindersApi();
      const action = {
        payload: {
          newReminder: {},
          oldReminder: {}
        }
      };
      const error = {
        name: 'error'
      };

      testSaga(updateReminder, action)
        .next()
        .getContext('remindersApi')
        .next(remindersApi)
        .put(actions.updateReminderSuccess(action.payload))
        .next()
        .call([remindersApi, remindersApi.update], action.payload.newReminder)
        .next()
        .isDone()

        .restart()
        .next()
        .throw(error)
        .call([errorReporter, errorReporter.send], 'updateReminder', error)
        .next()
        .put(actions.updateReminderFailure(action.payload))
        .next()
        .put(notifyFailure({error}))
        .next()
        .isDone();
    });
  });

  describe('deleteReminder', () => {
    test('должен работать', () => {
      const remindersApi = new RemindersApi();
      const action = {
        payload: {
          id: 100
        }
      };
      const error = {
        name: 'error'
      };

      testSaga(deleteReminder, action)
        .next()
        .getContext('remindersApi')
        .next(remindersApi)
        .call([remindersApi, remindersApi.delete], action.payload.id)
        .next()
        .put(actions.deleteReminderSuccess(action.payload))
        .next()
        .isDone()

        .restart()
        .next()
        .throw(error)
        .call([errorReporter, errorReporter.send], 'deleteReminder', error)
        .next()
        .put(notifyFailure({error}))
        .next()
        .isDone();
    });
  });
});
