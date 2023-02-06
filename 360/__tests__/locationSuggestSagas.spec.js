import {testSaga} from 'redux-saga-test-plan';

import SagaErrorReporter from 'utils/SagaErrorReporter';

import LocationSuggestApi from '../LocationSuggestApi';
import {ActionTypes} from '../locationSuggestConstants';
import rootSaga, {loadSuggestions} from '../locationSuggestSagas';

const errorReporter = new SagaErrorReporter('locationSuggest');

describe('locationSuggestSagas', () => {
  describe('rootSaga', () => {
    test('должен работать', () => {
      testSaga(rootSaga)
        .next()
        .getContext('api')
        .next()
        .setContext({locationSuggestApi: new LocationSuggestApi()})
        .next()
        .takeLatestEffect(ActionTypes.LOAD_SUGGESTIONS, loadSuggestions)
        .next()
        .isDone();
    });
  });

  describe('loadSuggestions', () => {
    test('должен работать', () => {
      const locationSuggestApi = new LocationSuggestApi();
      const action = {
        payload: {
          part: 'St. P'
        },
        resolve() {}
      };
      const response = [];
      const error = {
        name: 'error'
      };

      testSaga(loadSuggestions, action)
        .next()
        .getContext('locationSuggestApi')
        .next(locationSuggestApi)
        .call([locationSuggestApi, locationSuggestApi.getSuggestions], action.payload.part)
        .next(response)
        .call(action.resolve, response)
        .next()
        .isDone()

        .restart()
        .next()
        .throw(error)
        .call([errorReporter, errorReporter.send], 'loadSuggestions', error)
        .next()
        .isDone();
    });
  });
});
