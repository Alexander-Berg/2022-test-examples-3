import {ActionTypes} from '../roomCardConstants';
import {getRoomInfo, getUserOrResourceInfo, getUserOrResourceInfoSuccess} from '../roomCardActions';

describe('roomCardActions', () => {
  describe('getRoomInfo', () => {
    test('должен вернуть экшен GET_ROOM_INFO', () => {
      const payload = {
        email: 'conf_rr_2_9@yandex-team.ru'
      };
      const resolve = () => {};

      expect(getRoomInfo(payload, resolve)).toEqual({
        type: ActionTypes.GET_ROOM_INFO,
        payload,
        resolve
      });
    });
  });
  describe('getUserOrResourceInfo', () => {
    test('должен вернуть экшен GET_USER_OR_RESOURCE_INFO', () => {
      const payload = {
        login: 'tavria',
        email: 'conf_rr_2_9@yandex-team.ru'
      };

      expect(getUserOrResourceInfo(payload)).toEqual({
        type: ActionTypes.GET_USER_OR_RESOURCE_INFO,
        payload
      });
    });
  });
  describe('getUserOrResourceInfoSuccess', () => {
    test('должен вернуть экшен GET_USER_OR_RESOURCE_INFO_SUCCESS', () => {
      const payload = 'info';

      expect(getUserOrResourceInfoSuccess(payload)).toEqual({
        type: ActionTypes.GET_USER_OR_RESOURCE_INFO_SUCCESS,
        payload: {info: payload}
      });
    });
  });
});
