import {expectSaga} from 'redux-saga-test-plan';

import conditionalSaga from '../conditionalSaga';

describe('conditionalSaga', () => {
  test('вызывает задекорированную сагу, если селектор вернул true', async () => {
    const saga = jest.fn();
    const selector = () => true;
    const decoratedSaga = conditionalSaga(selector, saga);

    await expectSaga(decoratedSaga).silentRun(0);

    expect(saga).toHaveBeenCalledTimes(1);
  });
  test('не вызывает задекорированную сагу, если селектор вернул false', async () => {
    const saga = jest.fn();
    const selector = () => false;
    const decoratedSaga = conditionalSaga(selector, saga);

    await expectSaga(decoratedSaga).silentRun(0);

    expect(saga).toHaveBeenCalledTimes(0);
  });
  test('вызывает задекорированную сагу, пробросив в неё аргументы', async () => {
    const saga = jest.fn();
    const selector = () => true;
    const decoratedSaga = conditionalSaga(selector, saga);

    await expectSaga(decoratedSaga, 1, 2, 3).silentRun(0);

    expect(saga).toHaveBeenCalledWith(1, 2, 3);
  });
});
