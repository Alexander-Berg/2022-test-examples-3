import React from 'react';
import { screen, render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Checkbox } from '../Checkbox';

describe('design/Checkbox', () => {
  describe('props.checked', () => {
    describe('is true', () => {
      it('renders check', () => {
        render(<Checkbox checked />);

        expect(screen.getByRole('checkbox', { checked: true })).toBeInTheDocument();
      });
    });

    describe('is false', () => {
      it(`doesn't render check`, () => {
        render(<Checkbox checked={false} />);

        expect(screen.getByRole('checkbox', { checked: false })).toBeInTheDocument();
      });
    });
  });

  describe('on click', () => {
    describe(`when isn't checked`, () => {
      it('calls props.onChange with true', () => {
        const handleChange = jest.fn();
        render(<Checkbox onChange={handleChange} />);

        const checkbox = screen.getByRole('checkbox', { checked: false });
        userEvent.click(checkbox);

        expect(handleChange).toBeCalledWith(true);
      });
    });

    describe(`when checked`, () => {
      it('calls props.onChange with false', () => {
        const handleChange = jest.fn();
        render(<Checkbox checked onChange={handleChange} />);

        const checkbox = screen.getByRole('checkbox', { checked: true });
        userEvent.click(checkbox);

        expect(handleChange).toBeCalledWith(false);
      });
    });
  });
});
