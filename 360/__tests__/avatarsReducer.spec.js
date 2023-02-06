import {Map} from 'immutable';

import * as envConfig from 'configs/environment';
import {isCorpEmail} from 'utils/emails';
import EventRecord from 'features/events/EventRecord';
import {ActionTypes as EventsActionTypes} from 'features/events/eventsConstants';

import getCorpAvatarUrl from '../utils/getCorpAvatarUrl';
import {ActionTypes} from '../avatarsConstants';
import avatars, {initialState} from '../avatarsReducer';

jest.mock('utils/emails');
jest.mock('../utils/getCorpAvatarUrl');

describe('avatarsReducer', () => {
  test('должен обрабатывать начальное состояние', () => {
    expect(avatars(undefined, {})).toEqual(initialState);
  });

  describe('ADD_AVATAR_URL', () => {
    test('должен добавлять аватарку', () => {
      const action = {
        type: ActionTypes.ADD_AVATAR_URL,
        payload: {
          email: 'test_1@ya.ru',
          url: 'some_url_1'
        }
      };
      const state = new Map({
        'test@ya.ru': 'some_url'
      });
      const expectedState = new Map({
        'test@ya.ru': 'some_url',
        'test_1@ya.ru': 'some_url_1'
      });

      expect(avatars(state, action)).toEqual(expectedState);
    });
  });

  describe('GET_EVENT_SUCCESS', () => {
    beforeEach(() => {
      sinon.stub(envConfig, 'isCorp').value(true);
    });

    test('не должен отрабатывать вне корпа', () => {
      const email1 = 'test1@ya.ru';
      const url1 = Symbol();
      const email2 = 'test2@ya.ru';
      const url2 = Symbol();
      const emailUrlMap = {
        [email1]: url1,
        [email2]: url2
      };
      const action = {
        type: EventsActionTypes.GET_EVENT_SUCCESS,
        event: {
          attendees: {
            [email1]: {email: email1},
            [email2]: {email: email2}
          }
        }
      };
      const state = new Map();

      sinon.stub(envConfig, 'isCorp').value(false);
      isCorpEmail.mockReturnValue(true);
      getCorpAvatarUrl.mockImplementation(userData => emailUrlMap[userData.email]);

      expect(avatars(state, action)).toEqual(state);
    });

    test('должен добавлять корповые аватарки сразу', () => {
      const email1 = 'test1@ya.ru';
      const url1 = Symbol();
      const email2 = 'test2@ya.ru';
      const url2 = Symbol();
      const emailUrlMap = {
        [email1]: url1,
        [email2]: url2
      };
      const action = {
        type: EventsActionTypes.GET_EVENT_SUCCESS,
        event: {
          attendees: {
            [email1]: {email: email1},
            [email2]: {email: email2}
          }
        }
      };
      const state = new Map();
      const expectedState = new Map(emailUrlMap);

      isCorpEmail.mockReturnValue(true);
      getCorpAvatarUrl.mockImplementation(userData => emailUrlMap[userData.email]);

      expect(avatars(state, action)).toEqual(expectedState);
    });

    test('должен уметь работать с EventRecord', () => {
      const email1 = 'test1@ya.ru';
      const url1 = Symbol();
      const email2 = 'test2@ya.ru';
      const url2 = Symbol();
      const emailUrlMap = {
        [email1]: url1,
        [email2]: url2
      };
      const action = {
        type: EventsActionTypes.GET_EVENT_SUCCESS,
        event: new EventRecord({
          attendees: {
            [email1]: {email: email1}
          },
          subscribers: [{email: email2}]
        })
      };
      const state = new Map();
      const expectedState = new Map(emailUrlMap);

      isCorpEmail.mockReturnValue(true);
      getCorpAvatarUrl.mockImplementation(userData => emailUrlMap[userData.email]);

      expect(avatars(state, action)).toEqual(expectedState);
    });

    test('не должен добавлять некорповые аватарки', () => {
      const email1 = 'test1@ya.ru';
      const url1 = Symbol();
      const email2 = 'test2@ya.ru';
      const url2 = Symbol();
      const emailUrlMap = {
        [email1]: url1,
        [email2]: url2
      };
      const action = {
        type: EventsActionTypes.GET_EVENT_SUCCESS,
        event: {
          attendees: {
            [email1]: {email: email1},
            [email2]: {email: email2}
          }
        }
      };
      const state = new Map();

      isCorpEmail.mockReturnValue(false);
      getCorpAvatarUrl.mockImplementation(userData => emailUrlMap[userData.email]);

      expect(avatars(state, action)).toEqual(state);
    });
  });
});
