import {testSaga} from 'redux-saga-test-plan';

import SagaErrorReporter from 'utils/SagaErrorReporter';
import {featuresSelector} from 'features/appStatus/appStatusSelectors';

import ResourcesSuggestApi from '../ResourcesSuggestApi';
import {ActionTypes} from '../resourcesSuggestConstants';
import rootSaga, {loadSuggestions, loadSuggestionsDetails} from '../resourcesSuggestSagas';

const errorReporter = new SagaErrorReporter('resourcesSuggest');

describe('resourcesSuggestSagas', () => {
  describe('rootSaga', () => {
    test('должен работать', () => {
      testSaga(rootSaga)
        .next()
        .getContext('api')
        .next()
        .setContext({resourcesSuggestApi: new ResourcesSuggestApi()})
        .next()
        .takeLatestEffect(ActionTypes.LOAD_SUGGESTIONS, loadSuggestions)
        .next()
        .takeLatestEffect(ActionTypes.LOAD_SUGGESTIONS_DETAILS, loadSuggestionsDetails)
        .next()
        .isDone();
    });
  });

  describe('loadSuggestions', () => {
    test('должен работать', () => {
      const resourcesSuggestApi = new ResourcesSuggestApi();
      const action = {
        payload: {
          query: 'Аврора',
          start: '2018-01-01T10:30:00',
          end: '2018-01-01T11:00:00'
        },
        resolve() {}
      };
      const response = {
        resources: []
      };
      const error = {
        name: 'error'
      };

      testSaga(loadSuggestions, action)
        .next()
        .getContext('resourcesSuggestApi')
        .next(resourcesSuggestApi)
        .call([resourcesSuggestApi, resourcesSuggestApi.getSuggestions], action.payload)
        .next(response)
        .call(action.resolve, response.resources)
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

  describe('loadSuggestionsDetails', () => {
    test('должен работать', () => {
      const resourcesSuggestApi = new ResourcesSuggestApi();
      const action = {
        payload: {
          suggestions: [
            {
              email: 'cherdak_simf@yandex-team.ru'
            },
            {
              email: 'prachechnaya_simf@yandex-team.ru'
            }
          ]
        },
        resolve() {}
      };
      const response = {
        resources: []
      };
      const error = {
        name: 'error'
      };

      testSaga(loadSuggestionsDetails, action)
        .next()
        .select(featuresSelector)
        .next({reachResourceSuggest: 1})
        .getContext('resourcesSuggestApi')
        .next(resourcesSuggestApi)
        .call([resourcesSuggestApi, resourcesSuggestApi.getSuggestionsDetails], action.payload)
        .next(response)
        .call(action.resolve, response.resources)
        .next()
        .isDone()

        .restart()
        .next()
        .throw(error)
        .call([errorReporter, errorReporter.send], 'loadSuggestionsDetails', error)
        .next()
        .isDone();
    });
  });
});
