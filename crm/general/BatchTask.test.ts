import { Subject, Subscription, of, timer, throwError } from 'rxjs';
import { tap, finalize } from 'rxjs/operators';
import { BatchTask } from './BatchTask';

jest.useFakeTimers('modern');

describe('BatchTask', () => {
  const complete = jest.fn();
  const error = jest.fn();

  let subscription = new Subscription();

  beforeEach(() => {
    complete.mockClear();
    error.mockClear();
  });

  afterEach(() => {
    subscription.unsubscribe();
  });

  describe('when subscribe to completed task', () => {
    let batchTask: BatchTask;
    let subscription2 = new Subscription();

    beforeEach(() => {
      batchTask = new BatchTask({ debounceTime: 500 });

      subscription = batchTask.asObservable.subscribe({
        complete,
        error,
      });
    });

    afterEach(() => {
      subscription2.unsubscribe();
    });

    it('completes observer immediately', () => {
      const complete = jest.fn();
      const subTask = new Subject();
      batchTask.registerTask(subTask);
      subTask.complete();

      jest.advanceTimersByTime(600);

      subscription2 = batchTask.asObservable.subscribe({
        complete,
      });

      expect(complete).toBeCalledTimes(1);
    });

    it('completes with error observer immediately', () => {
      const error = jest.fn();
      const subTask = new Subject();
      batchTask.registerTask(subTask);
      subTask.error('test');

      subscription2 = batchTask.asObservable.subscribe({
        error,
      });

      expect(error).toBeCalledTimes(1);
    });
  });

  describe('totalSubTasks option', () => {
    describe('when totalSubTasks equal -1 (default value)', () => {
      describe('and no sub tasks', () => {
        it('resolves by debounceTime', () => {
          const batchTask = new BatchTask({ debounceTime: 500 });

          subscription = batchTask.asObservable.subscribe({
            complete,
            error,
          });

          expect(complete).toBeCalledTimes(0);
          jest.advanceTimersByTime(600);
          expect(complete).toBeCalledTimes(1);
        });
      });

      describe('and tasks are added one by one', () => {
        it('waits with debounce', () => {
          const batchTask = new BatchTask({ debounceTime: 500 });
          const task1 = new Subject();
          const task2 = new Subject();

          subscription = batchTask.asObservable.subscribe({
            complete,
            error,
          });

          batchTask.registerTask(task1);
          task1.complete();
          jest.advanceTimersByTime(400);
          expect(complete).toBeCalledTimes(0);

          batchTask.registerTask(task2);
          task2.complete();
          jest.advanceTimersByTime(400);
          expect(complete).toBeCalledTimes(0);

          jest.advanceTimersByTime(600);
          expect(complete).toBeCalledTimes(1);
        });
      });
    });

    describe('when totalSubTasks equal 0', () => {
      it('resolves immediately', () => {
        const batchTask = new BatchTask({ debounceTime: 500, totalSubTasks: 0 });

        subscription = batchTask.asObservable.subscribe({
          complete,
          error,
        });

        expect(complete).toBeCalledTimes(1);
      });

      it('does not subscribe to new tasks', () => {
        const tapSpy = jest.fn();
        const task = of(true).pipe(tap(tapSpy));
        const batchTask = new BatchTask({ totalSubTasks: 0 });

        subscription = batchTask.asObservable.subscribe({
          complete,
          error,
        });

        batchTask.registerTask(task);

        expect(tapSpy).not.toBeCalled();
      });
    });

    describe('when totalSubTasks more 0', () => {
      it('wait complete all tasks and does not use debounceTime', () => {
        const batchTask = new BatchTask({ debounceTime: 100, totalSubTasks: 2 });
        const task1 = new Subject();
        const task2 = new Subject();

        subscription = batchTask.asObservable.subscribe({
          complete,
          error,
        });

        jest.advanceTimersByTime(400);
        expect(complete).toBeCalledTimes(0);

        batchTask.registerTask(task1);
        task1.complete();
        jest.advanceTimersByTime(400);
        expect(complete).toBeCalledTimes(0);

        batchTask.registerTask(task2);
        task2.complete();
        expect(complete).toBeCalledTimes(1);
      });
    });
  });

  describe('.registerTask', () => {
    [-1, 1].forEach((totalSubTasks) => {
      describe(`when totalSubTasks = ${totalSubTasks}`, () => {
        it('completes asObservable before subscribe', () => {
          const batchTask = new BatchTask({ debounceTime: 500, totalSubTasks });

          const task = of(true);
          batchTask.registerTask(task);
          batchTask.unregisterTask(task);

          jest.advanceTimersByTime(600);

          subscription = batchTask.asObservable.subscribe({
            complete,
            error,
          });

          expect(complete).toBeCalledTimes(1);
        });

        it('rejects asObservable before subscribe', () => {
          const batchTask = new BatchTask({ debounceTime: 500, totalSubTasks });

          const task = throwError(new Error('error'));
          batchTask.registerTask(task);

          subscription = batchTask.asObservable.subscribe({
            complete,
            error,
          });

          expect(error).toBeCalledTimes(1);
        });
      });
    });
  });

  describe('.destroy', () => {
    [-1, 3].forEach((totalSubTasks) => {
      describe(`when totalSubTasks = ${totalSubTasks}`, () => {
        it('unsubscribes from each task', () => {
          const batchTask = new BatchTask({ debounceTime: 500, totalSubTasks });
          const finalizeSpy = jest.fn();
          const task1 = timer(1000).pipe(finalize(finalizeSpy));
          const task2 = timer(1000).pipe(finalize(finalizeSpy));

          subscription = batchTask.asObservable.subscribe({
            complete,
            error,
          });

          batchTask.registerTask(task1);
          batchTask.registerTask(task2);

          batchTask.destroy();

          expect(finalizeSpy).toBeCalledTimes(2);
        });

        describe('after destroy', () => {
          it('does not subscribe to new tasks', () => {
            const tapSpy = jest.fn();
            const task = of(true).pipe(tap(tapSpy));
            const batchTask = new BatchTask({ debounceTime: 500, totalSubTasks });
            batchTask.destroy();

            batchTask.registerTask(task);

            expect(tapSpy).not.toBeCalled();
          });
        });

        describe('destroy without error', () => {
          it('completes', () => {
            const batchTask = new BatchTask({ debounceTime: 500, totalSubTasks });

            subscription = batchTask.asObservable.subscribe({
              complete,
              error,
            });

            batchTask.destroy();

            expect(complete).toBeCalledTimes(1);
          });
        });

        describe('destroy with error', () => {
          it('completes with error', () => {
            const batchTask = new BatchTask({ debounceTime: 500, totalSubTasks });
            const errorObj = new Error('error');

            subscription = batchTask.asObservable.subscribe({
              complete,
              error,
            });

            batchTask.destroy(errorObj);

            expect(error).toBeCalledTimes(1);
            expect(error).toBeCalledWith(errorObj);
          });
        });
      });
    });
  });
});
