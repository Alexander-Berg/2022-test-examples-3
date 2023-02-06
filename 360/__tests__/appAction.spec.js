import {
  handleAppMount,
  registerBackButtonHandler,
  unregisterBackButtonHandler,
  offlineDataUpdate
} from '../appActions';
import {ActionTypes} from '../appConstants';

describe('appActions', () => {
  const handler = () => {};

  describe('handleAppMount', () => {
    test('должен кидать экшн HANDLE_APP_MOUNT', () => {
      expect(handleAppMount()).toEqual({type: ActionTypes.HANDLE_APP_MOUNT});
    });
  });

  describe('registerBackButtonHandler', () => {
    test('должен кидать экшн REGISTER_BACK_BUTTON_HANDLER', () => {
      expect(registerBackButtonHandler(handler)).toEqual({
        type: ActionTypes.REGISTER_BACK_BUTTON_HANDLER,
        handler
      });
    });
  });

  describe('unregisterBackButtonHandler', () => {
    test('должен кидать экшн UNREGISTER_BACK_BUTTON_HANDLER', () => {
      expect(unregisterBackButtonHandler(handler)).toEqual({
        type: ActionTypes.UNREGISTER_BACK_BUTTON_HANDLER,
        handler
      });
    });
  });

  describe('offlineDataUpdate', () => {
    test('должен кидать экшн OFFLINE_DATA_UPDATE', () => {
      expect(offlineDataUpdate()).toEqual({
        type: ActionTypes.OFFLINE_DATA_UPDATE
      });
    });
  });
});
