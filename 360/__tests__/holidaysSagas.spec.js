import {delay} from 'redux-saga';
import {testSaga} from 'redux-saga-test-plan';

import SagaErrorReporter from 'utils/SagaErrorReporter';

import HolidaysApi from '../HolidaysApi';
import rootSaga, {getHolidaysNetwork} from '../holidaysSagas';
import * as actions from '../holidaysActions';

const errorReporter = new SagaErrorReporter('holidays');

describe('holidaysSagas', () => {
  describe('rootSaga', () => {
    test('должен работать', () => {
      testSaga(rootSaga)
        .next()
        .getContext('api')
        .next()
        .setContext({holidaysApi: new HolidaysApi()})
        .next()
        .takeLatestEffect(actions.getHolidaysNetwork.type, getHolidaysNetwork)
        .next()
        .isDone();
    });
  });

  describe('getHolidaysNetwork', () => {
    test('должен работать', () => {
      const holidaysApi = new HolidaysApi();
      const action = {
        payload: {}
      };
      const response = {
        holidays: []
      };
      const error = {
        name: 'error'
      };

      testSaga(getHolidaysNetwork, action)
        .next()
        .call(delay, 200)
        .next()
        .getContext('holidaysApi')
        .next(holidaysApi)
        .call([holidaysApi, holidaysApi.getHolidays], action.payload)
        .next(response)
        .put(actions.getHolidaysSuccess(response.holidays))
        .next()
        .isDone()

        .restart()
        .next()
        .throw(error)
        .call([errorReporter, errorReporter.send], 'getHolidaysNetwork', error)
        .next()
        .isDone();
    });
  });
});
