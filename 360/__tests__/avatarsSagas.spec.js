import {expectSaga} from 'redux-saga-test-plan';
import {throwError} from 'redux-saga-test-plan/providers';
import {select, call, getContext} from 'redux-saga-test-plan/matchers';

import config from 'configs/config';
import * as environment from 'configs/environment';
import SagaErrorReporter from 'utils/SagaErrorReporter';

import getCorpAvatarUrl from '../utils/getCorpAvatarUrl';
import AvatarsApi from '../AvatarsApi';
import {ActionTypes} from '../avatarsConstants';
import * as avatarsSagas from '../avatarsSagas';
import * as avatarsActions from '../avatarsActions';
import * as avatarsSelectors from '../avatarsSelectors';

const errorReporter = new SagaErrorReporter('avatars');

jest.mock('configs/environment');
jest.mock('../utils/getCorpAvatarUrl');

describe('avatarsSagas', () => {
  describe('getAvatarURL', () => {
    describe('успешное выполнение', () => {
      it('не должен записывать в стейт url аватарки, если нет email', () => {
        const action = {
          payload: {}
        };

        return expectSaga(avatarsSagas.getAvatarURL, action)
          .provide([[select(avatarsSelectors.getAvatarURLByEmail, action.payload)]])
          .not.put.actionType(ActionTypes.ADD_AVATAR_URL)
          .run();
      });

      describe('корпоративный календарь', () => {
        const avatarsApi = new AvatarsApi();
        const defaultProviders = [
          [getContext('avatarsApi'), avatarsApi],
          [call.fn(avatarsApi.getSocialAvatar), {}]
        ];

        beforeEach(() => {
          sinon.stub(environment, 'isCorp').value(true);
          sinon.stub(config.urls, 'corpAvatars').value('https://corp-avatars/{{login}}');
        });

        describe('для корпоративного email', () => {
          it('должен записывать в стейт url аватарки с подставленным login, если есть login', () => {
            const action = {
              payload: {
                email: 'robot-mailcorp1@yandex-team.ru',
                login: 'robot'
              }
            };
            const url = 'https://corp-avatars/robot';

            getCorpAvatarUrl.mockReturnValue(url);

            return expectSaga(avatarsSagas.getAvatarURL, action)
              .provide([
                [select(avatarsSelectors.getAvatarURLByEmail, action.payload)],
                ...defaultProviders
              ])
              .put(
                avatarsActions.addAvatarURL({
                  email: action.payload.email,
                  url: url
                })
              )
              .run();
          });

          it('не должен перезаписывать в стейте url аватарки, если в существующем урле уже есть login', () => {
            const action = {
              payload: {
                email: 'robot@yandex-team.ru',
                login: 'robot'
              }
            };

            return expectSaga(avatarsSagas.getAvatarURL, action)
              .provide([
                [
                  select(avatarsSelectors.getAvatarURLByEmail, action.payload),
                  'https://corp-avatars/robot'
                ],
                ...defaultProviders
              ])
              .not.put(
                avatarsActions.addAvatarURL({
                  email: action.payload.email,
                  url: 'https://corp-avatars/robot'
                })
              )
              .run();
          });
        });

        describe('для публичного email', () => {
          it('не должен записывать в стейт url на корпоративную аватарку', () => {
            const action = {
              payload: {
                email: 'user@yandex.ru'
              }
            };

            return expectSaga(avatarsSagas.getAvatarURL, action)
              .provide([
                [select(avatarsSelectors.getAvatarURLByEmail, action.payload)],
                ...defaultProviders
              ])
              .not.put(
                avatarsActions.addAvatarURL({
                  email: action.payload.email,
                  url: 'https://corp-avatars/user'
                })
              )
              .run();
          });
        });
      });

      describe('публичный календарь', () => {
        beforeEach(() => {
          sinon.stub(environment, 'isCorp').value(false);
          sinon.stub(config.urls, 'avatars').value('https://public-avatars/{{uid}}');
        });

        it('не должен записывать в стейт url аватарки, если url уже есть', () => {
          const avatarsApi = new AvatarsApi();
          const action = {
            payload: {
              email: 'user@yandex.ru'
            }
          };

          return expectSaga(avatarsSagas.getAvatarURL, action)
            .provide([
              [select(avatarsSelectors.getAvatarURLByEmail, action.payload), 'https://some-url']
            ])
            .not.call.fn(avatarsApi.getSocialAvatar)
            .run();
        });

        it('должен записывать в стейт url аватарки с подставленным uid, если есть uid', () => {
          const action = {
            payload: {
              email: 'user@yandex.ru',
              uid: 100001
            }
          };

          return expectSaga(avatarsSagas.getAvatarURL, action)
            .provide([[select(avatarsSelectors.getAvatarURLByEmail, action.payload)]])
            .put(
              avatarsActions.addAvatarURL({
                email: action.payload.email,
                url: 'https://public-avatars/100001'
              })
            )
            .run();
        });

        it('должен записывать в стейт url социальной аватарки, если её удалось получить', () => {
          const avatarsApi = new AvatarsApi();
          const email = 'user@yandex.ru';
          const expectedUrl = 'https://vk.com/avatars/user';
          const action = {
            payload: {email}
          };

          return expectSaga(avatarsSagas.getAvatarURL, action)
            .provide([
              [select(avatarsSelectors.getAvatarURLByEmail, action.payload)],
              [getContext('avatarsApi'), avatarsApi],
              [call.fn(avatarsApi.getSocialAvatar), {[email]: [{ava: {url: expectedUrl}}]}]
            ])
            .put(
              avatarsActions.addAvatarURL({
                email: action.payload.email,
                url: expectedUrl
              })
            )
            .run();
        });

        it('не должен записывать в стейт url социальной аватарки, если её не удалось получить', () => {
          const avatarsApi = new AvatarsApi();
          const action = {
            payload: {
              email: 'user@yandex.ru'
            }
          };

          return expectSaga(avatarsSagas.getAvatarURL, action)
            .provide([
              [select(avatarsSelectors.getAvatarURLByEmail, action.payload)],
              [getContext('avatarsApi'), avatarsApi],
              [call.fn(avatarsApi.getSocialAvatar), {}]
            ])
            .not.put.actionType(ActionTypes.ADD_AVATAR_URL)
            .run();
        });
      });
    });

    describe('неуспешное выполнение', () => {
      test('должен логировать ошибку', () => {
        const action = {
          payload: {
            email: 'user@yandex.ru'
          }
        };

        return expectSaga(avatarsSagas.getAvatarURL, action)
          .provide([
            [
              select(avatarsSelectors.getAvatarURLByEmail, action.payload),
              throwError({name: 'error'})
            ],
            [call.fn(errorReporter.send)]
          ])
          .call([errorReporter, errorReporter.send], 'getAvatarURL', {name: 'error'})
          .run();
      });
    });
  });
});
