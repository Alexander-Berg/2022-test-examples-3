import React from 'react';
import { render } from '@testing-library/react';
import { problemStub } from 'modules/userStatus/Status.stubs';
import { ProblemIcon } from './ProblemIcon';

const problemsStub = [problemStub];

describe('userStatus/ProblemIcon', () => {
  describe('props.problems', () => {
    describe('when defined and length > 0', () => {
      it('renders icon', () => {
        const { container } = render(<ProblemIcon problems={problemsStub} />);

        expect(container.getElementsByClassName('Icon').length).toBe(problemsStub.length);
      });
    });

    describe('when defined and length == 0', () => {
      it("doesn't render icon", () => {
        const { container } = render(<ProblemIcon problems={[]} />);

        expect(container.getElementsByClassName('Icon').length).toBe(0);
      });
    });

    describe('when undefined', () => {
      it("doesn't render icon", () => {
        const { container } = render(<ProblemIcon />);

        expect(container.getElementsByClassName('Icon').length).toBe(0);
      });
    });
  });
});
