import React, { useEffect, useContext } from 'react';
import { timer, throwError } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { render, screen, act } from '@testing-library/react';
import { BatchTask } from 'services/BatchTask';
import { Retryable } from 'types/Retryable';
import { AsyncRender } from './AsyncRender';
import { AsyncRenderContext } from './AsyncRender.context';
import { testIds } from './AsyncRender.config';

jest.useFakeTimers('modern');

const subTaskDuration = 1000;
const timeAfterCompleteBatchTask = subTaskDuration + BatchTask.DEFAULT_DEBOUNCE_TIME + 100;

const childComponentDidMount = jest.fn();

const createChildComponent = ({ isError } = { isError: false }) => () => {
  const asyncRender = useContext(AsyncRenderContext);

  useEffect(() => {
    childComponentDidMount();

    let task = timer(subTaskDuration);

    if (isError) {
      task = task.pipe(switchMap(() => throwError(new Error('error'))));
    }

    asyncRender.registerTask(task);
  }, []);

  return null;
};

describe('AsyncRender', () => {
  beforeEach(() => {
    childComponentDidMount.mockClear();
  });

  describe('prop totalSubTasks', () => {
    describe('when totalSubTasks != 0', () => {
      it('hides children', () => {
        render(<AsyncRender>content</AsyncRender>);

        expect(screen.getByTestId(testIds.hiddenChildrenWrap).hidden).toBe(true);
      });
    });

    describe('when totalSubTasks=0', () => {
      it('displays children sync', () => {
        render(<AsyncRender totalSubTasks={0}>content</AsyncRender>);

        expect(screen.getByTestId(testIds.hiddenChildrenWrap).hidden).toBe(false);
      });
    });
  });

  describe('when batch task is running', () => {
    const Child = createChildComponent();

    it('displays spinner', () => {
      render(
        <AsyncRender>
          <Child />
        </AsyncRender>,
      );

      expect(screen.getByTestId(testIds.hiddenChildrenWrap).hidden).toBe(true);
      expect(screen.queryByTestId(testIds.errorBlock)).not.toBeInTheDocument();
      expect(screen.queryByTestId('spinner')).toBeInTheDocument();
    });
  });

  describe('when batch task completes', () => {
    const Child = createChildComponent();

    it('displays children', () => {
      render(
        <AsyncRender>
          <Child />
        </AsyncRender>,
      );

      act(() => {
        jest.advanceTimersByTime(timeAfterCompleteBatchTask);
      });

      expect(screen.getByTestId(testIds.hiddenChildrenWrap).hidden).toBe(false);
      expect(screen.queryByTestId(testIds.errorBlock)).not.toBeInTheDocument();
      expect(screen.queryByTestId('spinner')).not.toBeInTheDocument();
    });

    it('calls success callback', () => {
      const handleSuccess = jest.fn();

      render(
        <AsyncRender onRenderSuccess={handleSuccess}>
          <Child />
        </AsyncRender>,
      );

      act(() => {
        jest.advanceTimersByTime(timeAfterCompleteBatchTask);
      });

      expect(handleSuccess).toBeCalledTimes(1);
    });
  });

  describe('when batch task falls', () => {
    const Child = createChildComponent({ isError: true });

    it('displays error', () => {
      render(
        <AsyncRender>
          <Child />
        </AsyncRender>,
      );

      act(() => {
        jest.advanceTimersByTime(timeAfterCompleteBatchTask);
      });

      expect(screen.getByTestId(testIds.hiddenChildrenWrap).hidden).toBe(true);
      expect(screen.queryByTestId(testIds.errorBlock)).toBeInTheDocument();
      expect(screen.queryByTestId('spinner')).not.toBeInTheDocument();
    });

    it('calls error callback', () => {
      const handleError = jest.fn();

      render(
        <AsyncRender onRenderError={handleError}>
          <Child />
        </AsyncRender>,
      );

      act(() => {
        jest.advanceTimersByTime(timeAfterCompleteBatchTask);
      });

      expect(handleError).toBeCalledTimes(1);
    });

    it('rerenders children on retry', () => {
      const RetryComponent = jest.fn((_props: Retryable) => null);

      render(
        <AsyncRender retryComponent={RetryComponent}>
          <Child />
        </AsyncRender>,
      );

      act(() => {
        jest.advanceTimersByTime(timeAfterCompleteBatchTask);
      });

      act(() => {
        RetryComponent.mock.calls[0][0].onRetry();
      });

      expect(childComponentDidMount).toBeCalledTimes(2);
    });
  });
});
