import React from 'react';
import { render, act } from '@testing-library/react';
import { mocked } from 'ts-jest/utils';
import { AsyncRender, AsyncRenderProps } from 'components/AsyncRender';
import { AsyncTransition } from './AsyncTransition';
import { AsyncTransitionState, AsyncTransitionProps } from './AsyncTransition.types';

jest.mock('components/AsyncRender', () => ({
  AsyncRender: jest.fn((props) => {
    return <div>{props.children}</div>;
  }),
}));

const AsyncRenderMocked = mocked(AsyncRender);

const children = jest.fn((state: AsyncTransitionState) => state.hash);

const getAsyncRenderPropsByHash = (hash: number | string): AsyncRenderProps => {
  return AsyncRenderMocked.mock.calls
    .slice()
    .reverse()
    .find((args) => args[0].children === hash)![0];
};

describe('AsyncTransition', () => {
  const firstChildHash = 1;
  const secondChildHash = 2;

  const state1: AsyncTransitionState = { hash: firstChildHash };
  const state2: AsyncTransitionState = { hash: secondChildHash };

  beforeEach(() => {
    children.mockClear();
    AsyncRenderMocked.mockClear();
  });

  it('passes totalSubTasks from state to AsyncRender', () => {
    const state: AsyncTransitionState = { hash: 1, totalSubTasks: 2 };

    render(<AsyncTransition state={state}>{children}</AsyncTransition>);

    expect(AsyncRenderMocked.mock.calls[0][0].totalSubTasks).toBe(2);
  });

  describe('when first child has not been loaded', () => {
    describe('and when change state', () => {
      it('rerenders only second child', () => {
        const { rerender, container } = render(
          <AsyncTransition state={state1}>{children}</AsyncTransition>,
        );

        act(() => {
          rerender(<AsyncTransition state={state2}>{children}</AsyncTransition>);
        });

        expect(container).toMatchSnapshot();
      });
    });
  });

  describe('when first child has been loaded', () => {
    const renderFirstChildSuccess = (props?: AsyncTransitionProps) => {
      const result = render(
        <AsyncTransition state={state1} {...props}>
          {children}
        </AsyncTransition>,
      );

      act(() => {
        getAsyncRenderPropsByHash(firstChildHash)!.onRenderSuccess!();
      });

      return result;
    };

    describe('and when change state', () => {
      it('rerenders two children', () => {
        const { rerender, container } = renderFirstChildSuccess();

        act(() => {
          rerender(<AsyncTransition state={state2}>{children}</AsyncTransition>);
        });

        expect(container).toMatchSnapshot();
      });
    });

    describe('and second children render success', () => {
      it('rerenders with second child', () => {
        const { rerender, container } = renderFirstChildSuccess();

        act(() => {
          rerender(<AsyncTransition state={state2}>{children}</AsyncTransition>);
        });

        act(() => {
          getAsyncRenderPropsByHash(secondChildHash).onRenderSuccess!();
        });

        expect(container).toMatchSnapshot();
      });

      it('calls onTransitionSuccess', () => {
        const onTransitionSuccess = jest.fn();

        const { rerender } = renderFirstChildSuccess();

        act(() => {
          rerender(
            <AsyncTransition state={state2} onTransitionSuccess={onTransitionSuccess}>
              {children}
            </AsyncTransition>,
          );
        });

        act(() => {
          getAsyncRenderPropsByHash(secondChildHash).onRenderSuccess!();
        });

        expect(onTransitionSuccess).toBeCalledWith({ currentState: state2, prevState: state1 });
      });
    });

    describe('and second child render with error', () => {
      it('rerenders with only first child', () => {
        const { rerender, container } = renderFirstChildSuccess();

        act(() => {
          rerender(<AsyncTransition state={state2}>{children}</AsyncTransition>);
        });

        act(() => {
          getAsyncRenderPropsByHash(secondChildHash).onRenderError!();
        });

        expect(container).toMatchSnapshot();
      });

      it('calls onTransitionError', () => {
        const onTransitionError = jest.fn();

        const { rerender } = renderFirstChildSuccess();

        act(() => {
          rerender(
            <AsyncTransition state={state2} onTransitionError={onTransitionError}>
              {children}
            </AsyncTransition>,
          );
        });

        act(() => {
          getAsyncRenderPropsByHash(secondChildHash).onRenderError!();
        });

        expect(onTransitionError).toBeCalledWith(state1);
      });
    });
  });
});
