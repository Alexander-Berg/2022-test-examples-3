import React from 'react';
import { render, screen } from '@testing-library/react';
import { problemStub } from 'modules/userStatus/Status.stubs';
import userEvent from '@testing-library/user-event';
import { Problem } from './Problem';
import { ProblemAction } from './Problem.types';

const problemActionStub: ProblemAction = {
  text: 'action text',
  onClick: jest.fn(),
  ...problemStub.actions[0],
};

describe('userStatus/Problem', () => {
  describe('props.problem', () => {
    describe('when defined', () => {
      it('renders problem text', () => {
        render(<Problem problem={problemStub} actions={[]} />);

        expect(screen.getByText(problemStub.text)).toBeInTheDocument();
      });
    });
  });

  describe('props.actions', () => {
    describe('when defined', () => {
      it('renders actions list', () => {
        render(<Problem problem={problemStub} actions={[problemActionStub]} />);

        expect(screen.getByText(problemActionStub.text)).toBeInTheDocument();
      });

      it('calls on click', () => {
        const onClick = jest.fn();
        render(<Problem problem={problemStub} actions={[{ ...problemActionStub, onClick }]} />);

        userEvent.click(screen.getByText(problemActionStub.text));

        expect(onClick).toBeCalledWith(problemStub);
      });
    });
  });
});
