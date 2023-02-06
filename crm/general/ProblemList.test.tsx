import React from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import { problemStub } from 'modules/userStatus/Status.stubs';
import { ProblemList } from './ProblemList';

const problemsStub = [problemStub];

window.confirm = jest.fn(() => true);

Object.defineProperty(window, 'location', {
  writable: true,
  value: { reload: jest.fn() },
});

describe('userStatus/ProblemList', () => {
  describe('props.problems', () => {
    describe('when defined', () => {
      it('renders problems', () => {
        render(<ProblemList problems={problemsStub} />);

        expect(screen.queryAllByRole('button').length).toBe(problemsStub.length);
      });
    });

    describe('when undefined', () => {
      it("doesn't render problems", () => {
        render(<ProblemList />);

        expect(screen.queryAllByRole('button').length).toBe(0);
      });
    });
  });

  describe('props.problems.actions', () => {
    describe('when defined', () => {
      describe('when action === Fixed', () => {
        describe('props.onFixProblems', () => {
          describe('when defined', () => {
            it('calls onClick with new problems', () => {
              const onFixProblems = jest.fn();
              render(<ProblemList problems={problemsStub} onFixProblems={onFixProblems} />);

              fireEvent.click(screen.queryAllByRole('button')[0]);
              expect(onFixProblems).toBeCalledWith([{ name: problemStub.name }]);
            });
          });

          describe('when undefined', () => {
            it("doesn't call onClick with new problems", () => {
              const onChange = jest.fn();
              render(<ProblemList problems={problemsStub} />);

              fireEvent.click(screen.queryAllByRole('button')[0]);
              expect(onChange).not.toBeCalled();
            });
          });
        });
      });

      describe('when action === TabRefresh', () => {
        it('calls with tab refresh', () => {
          const onFixProblems = jest.fn();
          render(
            <ProblemList
              problems={[
                {
                  ...problemStub,
                  actions: [
                    {
                      ...problemStub.actions[0],
                      type: 'TabRefresh',
                    },
                  ],
                },
              ]}
              onFixProblems={onFixProblems}
            />,
          );

          fireEvent.click(screen.queryAllByRole('button')[0]);
          expect(window.location.reload).toBeCalled();
        });
      });
    });
  });
});
