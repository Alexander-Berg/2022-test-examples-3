import {takeLatest} from 'redux-saga/effects';
import {testSaga} from 'redux-saga-test-plan';

import SagaErrorReporter from 'utils/SagaErrorReporter';

import OfficesApi from '../OfficesApi';
import rootSaga, {getOfficesNetwork, getOfficesTzOffsetsNetwork} from '../officesSagas';
import * as actions from '../officesActions';

const errorReporter = new SagaErrorReporter('offices');

describe('officesSagas', () => {
  describe('rootSaga', () => {
    test('должен работать', () => {
      testSaga(rootSaga)
        .next()
        .getContext('api')
        .next()
        .setContext({officesApi: new OfficesApi()})
        .next()
        .all([
          takeLatest(actions.getOfficesNetwork.type, getOfficesNetwork),
          takeLatest(actions.getOfficesTzOffsetsNetwork.type, getOfficesTzOffsetsNetwork)
        ])
        .next()
        .isDone();
    });
  });

  describe('getOfficesNetwork', () => {
    test('должен работать', () => {
      const officeApi = new OfficesApi();
      const response = {
        offices: []
      };
      const error = {
        name: 'error'
      };

      testSaga(getOfficesNetwork)
        .next()
        .getContext('officesApi')
        .next(officeApi)
        .call([officeApi, officeApi.getOffices])
        .next(response)
        .put(actions.getOfficesSuccess(response.offices))
        .next()
        .isDone()

        .restart()
        .next()
        .throw(error)
        .call([errorReporter, errorReporter.send], 'getOfficesNetwork', error)
        .next()
        .isDone();
    });
  });

  describe('getOfficesTzOffsetsNetwork', () => {
    test('должен работать', () => {
      const officeApi = new OfficesApi();
      const action = {
        payload: {
          ts: '2018-06-06T21:00:00'
        }
      };
      const response = {
        offices: {}
      };
      const error = {
        name: 'error'
      };

      testSaga(getOfficesTzOffsetsNetwork, action)
        .next()
        .getContext('officesApi')
        .next(officeApi)
        .call([officeApi, officeApi.getOfficesTzOffsets], action.payload.ts)
        .next(response)
        .put(actions.getOfficesTzOffsetsSuccess(response.offices))
        .next()
        .isDone()

        .restart()
        .next()
        .throw(error)
        .call([errorReporter, errorReporter.send], 'getOfficesTzOffsetsNetwork', error)
        .next()
        .isDone();
    });
  });
});
