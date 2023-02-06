import React from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import { Flag } from './Flag';

describe('userStatus/Flag', () => {
  describe('props.text', () => {
    describe('when defined', () => {
      it('renders text', () => {
        render(<Flag name="test name" value={false} text="test text" onFlagChange={jest.fn()} />);

        expect(screen.getByText('test text')).toBeInTheDocument();
      });
    });
  });

  describe('props.value', () => {
    describe('when is true', () => {
      it('renders checked flag', () => {
        render(<Flag name="test name" value text="test text" onFlagChange={jest.fn()} />);

        expect(screen.getByRole('checkbox')).toBeChecked();
      });
    });

    describe('when is false', () => {
      it("doesn't render checked flag", () => {
        render(<Flag name="test name" value={false} text="test text" onFlagChange={jest.fn()} />);

        expect(screen.getByRole('checkbox')).not.toBeChecked();
      });
    });
  });

  describe('props.onFlagChange', () => {
    describe('when click on checkbox', () => {
      it('calls and recieves name and opossite value', () => {
        const flagStub = {
          name: 'name',
          value: false,
        };
        const onFlagChange = jest.fn();
        render(
          <Flag
            name={flagStub.name}
            value={flagStub.value}
            text="test text"
            onFlagChange={onFlagChange}
          />,
        );

        fireEvent.click(screen.getByRole('checkbox'));
        expect(onFlagChange).toBeCalledWith({ name: flagStub.name, value: !flagStub.value });
      });
    });
  });
});
