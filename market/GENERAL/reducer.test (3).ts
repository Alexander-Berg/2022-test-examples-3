import * as actions from './actions';
import reducer, { initialState } from './reducer';
import { BaseNotification, Notification } from './types';

describe('Notification reducer', () => {
  describe('pushNotification', () => {
    it('should add notification', () => {
      const notification: BaseNotification = {
        content: 'text',
        status: 'error',
      };

      const action = actions.pushNotification(notification);

      expect(reducer(initialState, action)).toEqual([action.payload, ...initialState]);
    });
  });

  describe('removeNotification', () => {
    it('should remove notification', () => {
      const notification: Notification = {
        id: 0,
        timestamp: +new Date(),
        hidden: true,
        content: 'text',
        status: 'error',
      };

      expect(reducer([...initialState, notification], actions.removeNotification(0))).toEqual([]);
    });
  });

  describe('clearNotifications', () => {
    it('should clear notifications', () => {
      const notification: Notification = {
        id: 0,
        timestamp: +new Date(),
        hidden: true,
        content: 'text',
        status: 'error',
      };

      expect(reducer([...initialState, notification], actions.clearNotifications())).toEqual([]);
    });
  });
});
