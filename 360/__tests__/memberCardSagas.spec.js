import {expectSaga} from 'redux-saga-test-plan';
import {call, select, getContext} from 'redux-saga-test-plan/matchers';
import {throwError} from 'redux-saga-test-plan/providers';
import {take} from 'redux-saga/effects';

import SagaErrorReporter from 'utils/SagaErrorReporter';

import MemberCardApi from '../MemberCardApi';
import * as actions from '../memberCardActions';
import {ActionTypes} from '../memberCardConstants';
import rootSaga, {getStaffCard, getMemberCardSelector} from '../memberCardSagas';

const errorReporter = new SagaErrorReporter('memberCard');

describe('memberCardSagas', () => {
  describe('rootSaga', () => {
    test('должен подписываться на экшны', async () => {
      const {effects} = await expectSaga(rootSaga)
        .provide([[getContext('api'), {}]])
        .silentRun(0);

      expect(effects.take).toEqual([take(ActionTypes.GET_STAFF_CARD)]);
    });
    test('должен записывать MemberCardApi в контекст', async () => {
      const api = {};
      return expectSaga(rootSaga)
        .provide([[getContext('api'), api]])
        .setContext({memberCardApi: new MemberCardApi(api)})
        .silentRun(0);
    });
  });

  describe('getStaffCard', () => {
    const memberCardApi = new MemberCardApi();

    test('должен запрашивать карточку сотрудника', () => {
      const login = 'login';
      const action = {login};
      const response = {login};

      return expectSaga(getStaffCard, action)
        .provide([
          [getContext('memberCardApi'), memberCardApi],
          [select(getMemberCardSelector, {login}), {}],
          [call.fn(memberCardApi.getStaffCard), response]
        ])
        .put(actions.getStaffCardStart(login))
        .call.fn(memberCardApi.getStaffCard)
        .put(actions.getStaffCardDone(login, response))
        .run();
    });

    test('не должен запрашивать карточку сотрудника, если она уже есть', () => {
      const login = 'login';
      const action = {login};
      const memberCard = {member: {login}, isLoading: false};

      return expectSaga(getStaffCard, action)
        .provide([
          [getContext('memberCardApi'), memberCardApi],
          [select(getMemberCardSelector, {login}), memberCard]
        ])
        .not.call.fn(memberCardApi.getStaffCard)
        .not.put(actions.getStaffCardStart(login))
        .not.put(actions.getStaffCardDone(login))
        .run();
    });

    test('не должен запрашивать карточку сотрудника, если идёт её загрузка', () => {
      const login = 'login';
      const action = {login};
      const memberCard = {member: null, isLoading: true};

      return expectSaga(getStaffCard, action)
        .provide([
          [getContext('memberCardApi'), memberCardApi],
          [select(getMemberCardSelector, {login}), memberCard]
        ])
        .not.call.fn(memberCardApi.getStaffCard)
        .not.put(actions.getStaffCardStart(login))
        .not.put(actions.getStaffCardDone(login))
        .run();
    });

    test('должен логировать ошибку', () => {
      const login = 'login';
      const action = {login};
      const error = {name: 'error'};

      return expectSaga(getStaffCard, action)
        .provide([[getContext('memberCardApi'), throwError({name: 'error'})]])
        .call([errorReporter, errorReporter.send], 'getStaffCard', error)
        .run();
    });
  });
});
