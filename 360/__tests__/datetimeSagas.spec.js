import {delay} from 'redux-saga';
import {expectSaga} from 'redux-saga-test-plan';
import {call} from 'redux-saga-test-plan/matchers';
import {throwError} from 'redux-saga-test-plan/providers';
import {takeEvery, fork} from 'redux-saga/effects';

import SagaErrorReporter from 'utils/SagaErrorReporter';

import rootSaga, {getDateNow, getMsToNextMinute, forceUpdate, updateTime} from '../datetimeSagas';
import * as actions from '../datetimeActions';
import {ActionTypes} from '../datetimeConstants';

const errorReporter = new SagaErrorReporter('datetime');

describe('datetimeSagas', () => {
  describe('rootSaga', () => {
    test('должен подписываться на экшены', async () => {
      const {effects} = await expectSaga(rootSaga).silentRun(0);

      expect(effects.fork).toEqual([
        fork(updateTime),
        takeEvery(ActionTypes.FORCE_UPDATE, forceUpdate)
      ]);
    });
  });

  describe('updateTime', () => {
    describe('успешное выполнение', () => {
      test('должен инициировать цикл поминутного апдейта даты/времени', async () => {
        const time = {
          dateTs: new Date(2020, 10, 20).getTime(),
          timeTs: new Date(2020, 10, 20, 12, 15).getTime()
        };

        return expectSaga(updateTime)
          .call(getDateNow)
          .put(actions.update(time))
          .call(getMsToNextMinute)
          .call(delay)
          .call(getDateNow);
      });

      test('должен использовать время, полученное из getDateNow', async () => {
        const time = {
          dateTs: new Date(2020, 10, 20).getTime(),
          timeTs: new Date(2020, 10, 20, 12, 15).getTime()
        };

        return expectSaga(updateTime)
          .provide([[call(getDateNow, time)]])
          .put(actions.update(time));
      });
    });

    describe('неуспешное выполнение', () => {
      test('должен логировать ошибку', () => {
        const error = {
          name: 'error'
        };

        return expectSaga(updateTime)
          .provide([[call.fn(getDateNow), throwError(error)], [call.fn(errorReporter.send)]])
          .call([errorReporter, errorReporter.send], 'updateTime', error)
          .run();
      });
    });
  });

  describe('forceUpdate', () => {
    test('должен обновлять дату/время', async () => {
      const time = {
        dateTs: new Date(2020, 10, 20).getTime(),
        timeTs: new Date(2020, 10, 20, 12, 15).getTime()
      };

      return expectSaga(forceUpdate)
        .provide([[call(getDateNow, time)]])
        .put(actions.update(time));
    });
  });
});
