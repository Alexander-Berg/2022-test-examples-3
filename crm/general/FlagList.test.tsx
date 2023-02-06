import React from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import { flagStub } from 'modules/userStatus/Status.stubs';
import { FlagList } from './FlagList';

const flagsStub = [flagStub];

describe('userStatus/FlagList', () => {
  describe('props.flags', () => {
    describe('when defined', () => {
      it('renders flags', () => {
        render(<FlagList flags={flagsStub} />);

        expect(screen.queryAllByRole('checkbox').length).toBe(flagsStub.length);
      });
    });

    describe('when undefined', () => {
      it("doesn't render flags", () => {
        render(<FlagList />);

        expect(screen.queryAllByRole('checkbox').length).toBe(0);
      });
    });
  });

  describe('props.onChange', () => {
    describe('when defined', () => {
      it('calls onClick with new flags', () => {
        const onChange = jest.fn();
        render(<FlagList flags={flagsStub} onChange={onChange} />);

        fireEvent.click(screen.queryAllByRole('checkbox')[0]);
        expect(onChange).toBeCalledWith([{ name: flagsStub[0].name, value: !flagsStub[0].value }]);
      });
    });

    describe('when undefined', () => {
      it("doesn't call onClick with new flags", () => {
        const onChange = jest.fn();
        render(<FlagList flags={flagsStub} />);

        fireEvent.click(screen.queryAllByRole('checkbox')[0]);
        expect(onChange).not.toBeCalled();
      });
    });
  });

  describe('props.isLoading', () => {
    describe('when defined', () => {
      it('renders loading spinner', () => {
        const { container } = render(<FlagList isLoading />);

        expect(container.getElementsByClassName('crm-spinner').length).toBe(1);
      });
    });

    describe('when undefined', () => {
      it("doesn't render loading spinner", () => {
        const { container } = render(<FlagList />);

        expect(container.getElementsByClassName('crm-spinner').length).toBe(0);
      });
    });
  });
});
