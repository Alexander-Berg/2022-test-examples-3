import {takeLatest} from 'redux-saga/effects';
import {testSaga, expectSaga} from 'redux-saga-test-plan';
import {call, getContext} from 'redux-saga-test-plan/matchers';
import {throwError} from 'redux-saga-test-plan/providers';

import SagaErrorReporter from 'utils/SagaErrorReporter';
import {makeCheckFavoriteContactsAvailability} from 'features/eventForm/eventFormActions';
import EventFormId from 'features/eventForm/EventFormId';

import AbookSuggestApi from '../AbookSuggestApi';
import {ActionTypes} from '../abookSuggestConstants';
import {getFavoriteContactsSuccess} from '../abookSuggestActions';
import rootSaga, {loadSuggestions, getFavoriteContacts} from '../abookSuggestSagas';

const errorReporter = new SagaErrorReporter('abookSuggest');
const form = EventFormId.fromParams(EventFormId.VIEWS.POPUP, EventFormId.MODES.CREATE).toString();

describe('abookSuggestSagas', () => {
  describe('rootSaga', () => {
    test('должен работать', () => {
      testSaga(rootSaga)
        .next()
        .getContext('api')
        .next()
        .setContext({abookSuggestApi: new AbookSuggestApi()})
        .next()
        .all([
          takeLatest(ActionTypes.LOAD_SUGGESTIONS, loadSuggestions),
          takeLatest(ActionTypes.GET_FAVORITE_CONTACTS, getFavoriteContacts)
        ])
        .next()
        .isDone();
    });
  });

  describe('loadSuggestions', () => {
    test('должен работать', () => {
      const abookSuggestApi = new AbookSuggestApi();
      const action = {
        payload: {
          q: 'fre'
        },
        resolve() {}
      };
      const response = {contacts: []};
      const error = {
        message: 'error'
      };

      testSaga(loadSuggestions, action)
        .next()
        .getContext('abookSuggestApi')
        .next(abookSuggestApi)
        .call([abookSuggestApi, abookSuggestApi.getSuggestions], action.payload)
        .next(response)
        .call(action.resolve, response.contacts)
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

  describe('getFavoriteContacts', () => {
    test('должен получать список избранных контактов', () => {
      const abookSuggestApi = new AbookSuggestApi();
      const response = {
        contacts: []
      };

      return expectSaga(getFavoriteContacts, {meta: {form}})
        .provide([
          [getContext('abookSuggestApi'), abookSuggestApi],
          [call.fn(abookSuggestApi.getFavoriteContacts), response]
        ])
        .call.fn(abookSuggestApi.getFavoriteContacts)
        .put(getFavoriteContactsSuccess(response.contacts))
        .run();
    });

    test('должен запрашивать занятость избранных контактов', () => {
      const abookSuggestApi = new AbookSuggestApi();
      const response = {
        contacts: []
      };

      return expectSaga(getFavoriteContacts, {
        shouldCheckAvailability: true,
        meta: {form}
      })
        .provide([
          [getContext('abookSuggestApi'), abookSuggestApi],
          [call.fn(abookSuggestApi.getFavoriteContacts), response]
        ])
        .put(makeCheckFavoriteContactsAvailability({form})())
        .run();
    });

    test('должен логировать ошибку', () => {
      const error = {name: 'error'};

      return expectSaga(getFavoriteContacts, {meta: {form}})
        .provide([
          [getContext('abookSuggestApi'), throwError(error)],
          [call.fn(errorReporter.send)]
        ])
        .call([errorReporter, errorReporter.send], 'getFavoriteContacts', error)
        .run();
    });
  });
});
