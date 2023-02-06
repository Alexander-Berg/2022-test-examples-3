import {testSaga} from 'redux-saga-test-plan';

import SagaErrorReporter from 'utils/SagaErrorReporter';

import RoomCardApi from '../RoomCardApi';
import {ActionTypes} from '../roomCardConstants';
import rootSaga, {getRoomInfo, getUserOrResourceInfo} from '../roomCardSagas';

const errorReporter = new SagaErrorReporter('roomCard');

describe('roomCardSagas', () => {
  describe('rootSaga', () => {
    test('должен работать', () => {
      testSaga(rootSaga)
        .next()
        .getContext('api')
        .next()
        .setContext({roomCardApi: new RoomCardApi()})
        .next()
        .takeEveryEffect(ActionTypes.GET_ROOM_INFO, getRoomInfo)
        .next()
        .takeEveryEffect(ActionTypes.GET_USER_OR_RESOURCE_INFO, getUserOrResourceInfo)
        .next()
        .isDone();
    });
  });

  describe('getRoomInfo', () => {
    test('должен работать', () => {
      const roomCardApi = new RoomCardApi();
      const action = {
        payload: {
          email: 'conf_rr_2_9@yandex-team.ru'
        },
        resolve() {}
      };
      const response = {
        capacity: 10,
        cityName: 'Москва',
        email: 'conf_rr_2_9@yandex-team.ru'
      };
      const error = {
        name: 'error'
      };

      testSaga(getRoomInfo, action)
        .next()
        .getContext('roomCardApi')
        .next(roomCardApi)
        .call([roomCardApi, roomCardApi.getInfo], {email: action.payload.email})
        .next(response)
        .call(action.resolve, response)
        .next()
        .isDone()

        .restart()
        .next()
        .throw(error)
        .call([errorReporter, errorReporter.send], 'getRoomInfo', error)
        .next()
        .isDone();
    });
  });
});
