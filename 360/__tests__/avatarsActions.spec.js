import {ActionTypes} from '../avatarsConstants';
import {getAvatarURL, addAvatarURL} from '../avatarsActions';

describe('avatarsActions', () => {
  describe('getAvatarURL', () => {
    test('должен вернуть экшен GET_AVATAR_URL', () => {
      const params = {
        email: 'test@ya.ru'
      };

      expect(getAvatarURL(params)).toEqual({
        type: ActionTypes.GET_AVATAR_URL,
        payload: params
      });
    });
  });

  describe('addAvatarURL', () => {
    test('должен вернуть экшен ADD_AVATAR_URL', () => {
      const params = {
        email: 'test@ya.ru',
        url: 'some_url'
      };

      expect(addAvatarURL(params)).toEqual({
        type: ActionTypes.ADD_AVATAR_URL,
        payload: params
      });
    });
  });
});
