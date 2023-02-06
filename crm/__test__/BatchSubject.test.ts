import { BatchSubject } from '../BatchSubject';

jest.useFakeTimers('modern');

const createBatchSubjectForTest = () => {
  return new BatchSubject({ throttleTime: 5000, maxBufferSize: 2 });
};

describe('BatchSubject', () => {
  it('sends by buffer max', () => {
    const batchSubject = createBatchSubjectForTest();
    const next = jest.fn();
    const subscription = batchSubject.outerObservable.subscribe(next);
    for (let i = 0; i < 5; i++) {
      batchSubject.next(i);
    }

    subscription.unsubscribe();

    expect(next).toBeCalledTimes(2);
    expect(next.mock.calls[0][0]).toStrictEqual([0, 1]);
    expect(next.mock.calls[1][0]).toStrictEqual([2, 3]);
  });

  it('sends by timer', () => {
    const batchSubject = createBatchSubjectForTest();
    const next = jest.fn();
    const subscription = batchSubject.outerObservable.subscribe(next);

    batchSubject.next(1);
    jest.advanceTimersByTime(20000);

    batchSubject.next(2);
    batchSubject.next(3);

    jest.advanceTimersByTime(20000);

    batchSubject.next(4);
    jest.advanceTimersByTime(20000);

    subscription.unsubscribe();

    expect(next).toBeCalledTimes(3);
    expect(next.mock.calls[0][0]).toStrictEqual([1]);
    expect(next.mock.calls[1][0]).toStrictEqual([2, 3]);
    expect(next.mock.calls[2][0]).toStrictEqual([4]);
  });
});
