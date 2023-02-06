import {ActionTypes} from '../appStatusConstants';
import {
  getStatus,
  startGettingStatus,
  getStatusSuccess,
  changeNetworkStatus
} from '../appStatusActions';

describe('appStatusActions', () => {
  describe('startGettingStatus', () => {
    test('Должен возвращать экшн START_GETTING_STATUS', () => {
      expect(startGettingStatus()).toEqual({
        type: ActionTypes.START_GETTING_STATUS
      });
    });
  });

  describe('getStatus', () => {
    test('Должен возвращать экшн GET_STATUS', () => {
      expect(getStatus()).toEqual({type: ActionTypes.GET_STATUS});
    });

    test('Должен возвращать экшн GET_STATUS со статусом', () => {
      expect(getStatus({ololo: 'ololo'})).toEqual({
        type: ActionTypes.GET_STATUS,
        status: {ololo: 'ololo'}
      });
    });
  });

  describe('getStatusSuccess', () => {
    test('Должен возвращать экшн GET_STATUS_SUCCESS', () => {
      expect(getStatusSuccess({readOnly: false, notifications: []})).toEqual({
        type: ActionTypes.GET_STATUS_SUCCESS,
        readOnly: false,
        notifications: []
      });
    });
  });

  describe('changeNetworkStatus', () => {
    test('Должен возвращать экшн CHANGE_NETWORK_STATUS', () => {
      const payload = {offline: true};
      expect(changeNetworkStatus(payload)).toEqual({
        type: ActionTypes.CHANGE_NETWORK_STATUS,
        ...payload
      });
    });
  });
});
