import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Option } from './Option';
import { checkIconTestid } from './Option.constants';

describe('Option', () => {
  describe('props.onClick', () => {
    it('calls on click', () => {
      const handleClick = jest.fn();
      render(<Option item={{ value: 1, name: 'test' }} onClick={handleClick} />);

      userEvent.click(screen.getByText('test'));

      expect(handleClick).toBeCalled();
    });
  });

  describe('props.isSelected', () => {
    it('renders check icon if true', () => {
      render(<Option item={{ value: 1, name: 'test' }} isSelected />);

      expect(screen.getByTestId(checkIconTestid)).toBeInTheDocument();
    });

    it(`doesn't render check icon if false`, () => {
      render(<Option item={{ value: 1, name: 'test' }} isSelected={false} />);

      expect(screen.queryByTestId(checkIconTestid)).not.toBeInTheDocument();
    });
  });
});
