import {call, take} from 'redux-saga/effects';
import {cloneableGenerator} from 'redux-saga/utils';
import {expectSaga, testSaga} from 'redux-saga-test-plan';

import SagaErrorReporter from 'utils/SagaErrorReporter';
import * as environment from 'configs/environment';

import {
  operations,
  visibilityUpdateWatch,
  xivaUpdateWatch,
  createXivaChannel,
  createVisibilityChannel,
  processXivaMessage
} from '../xivaSagas';

const errorReporter = new SagaErrorReporter('xiva');

describe('xivaSagas', () => {
  describe('xivaUpdateWatch', () => {
    const data = {
      channel: 'channel',
      error: 'error'
    };
    data.gen = cloneableGenerator(xivaUpdateWatch)();

    test('должен создать канал', () => {
      expect(data.gen.next().value).toEqual(call(createXivaChannel));
    });
    test('не должен ничего делать, если МЯП', async () => {
      sinon.stub(environment, 'isMobileApp').value(true);

      const {effects} = await expectSaga(xivaUpdateWatch).run(0);
      expect(Object.keys(effects)).toHaveLength(0);
    });
    describe('success way', () => {
      beforeAll(() => {
        data.genSuccess = data.gen.clone();
      });
      test('должен получить payload из канала', () => {
        expect(data.genSuccess.next(data.channel).value).toEqual(take(data.channel));
      });
      describe('есть операция и ее можно выполнить', () => {
        beforeAll(() => {
          data.genSuccess1 = data.genSuccess.clone();
          data.payload = {
            operation: 'user-events-changed'
          };
        });
        test('должен вызвать processXivaMessage', () => {
          expect(data.genSuccess1.next(data.payload).value).toEqual(
            call(processXivaMessage, data.payload)
          );
        });
      });
      describe('есть операция и ее нельзя выполнить', () => {
        beforeAll(() => {
          data.genSuccess2 = data.genSuccess.clone();
          data.payload = {
            operation: 'user-events-changed'
          };
        });
      });
      describe('нет операции', () => {
        beforeAll(() => {
          data.genSuccess3 = data.genSuccess.clone();
          data.payload = {
            operation: '__test__'
          };
        });
      });
    });
    describe('failure way', () => {
      beforeAll(() => {
        data.genFailure = data.gen.clone();
      });
      test('должен залогировать ошибку', () => {
        expect(data.genFailure.throw(data.error).value).toEqual(
          call([errorReporter, errorReporter.send], 'xivaUpdateWatch', data.error)
        );
      });
      test('должен завершить сагу', () => {
        expect(data.genFailure.next().done).toBe(true);
      });
    });
  });

  describe('processXivaMessage', () => {
    const realOperation = 'user-events-changed';
    const fakeOperation = 'fake';

    test('не должен проверить, можно ли обработать операцию, если ее нет в списке операций', () => {
      const payload = {
        operation: fakeOperation
      };
      expectSaga(processXivaMessage, payload)
        .not.call.fn(operations[realOperation].shouldProcess, payload)
        .run();
    });

    test('должен проверить, можно ли обработать операцию, если она есть списке операций', () => {
      const payload = {
        operation: realOperation
      };

      expectSaga(processXivaMessage, payload)
        .call.fn(operations[realOperation].shouldProcess, payload)
        .run();
    });

    test('должен кинуть экшен операции, если она есть в списке операций и ее можно обработать', () => {
      const payload = {
        operation: realOperation
      };

      expectSaga(processXivaMessage, payload)
        .put(operations[payload.operation].createAction(payload))
        .run();
    });
  });

  describe('visibilityUpdateWatch', () => {
    const data = {
      channel: 'channel',
      error: 'error'
    };
    data.gen = cloneableGenerator(visibilityUpdateWatch)();
    test('должен создать канал', () => {
      expect(data.gen.next().value).toEqual(call(createVisibilityChannel));
    });
    test('не должен ничего делать, если МЯП', () => {
      sinon.stub(environment, 'isMobileApp').value(true);

      testSaga(visibilityUpdateWatch)
        .next()
        .isDone();
    });
  });
});
