import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { ColorInput } from './ColorInput';
import { DEFAULT_COLOR_COLLECTION } from './ColorInput.constants';

describe('ColorInput', () => {
  it('renders', async () => {
    render(<ColorInput />);
    expect(screen.queryByTestId('ColorInput')).toBeInTheDocument();
  });
  it('renders all color blocks', async () => {
    const { container } = render(<ColorInput value={DEFAULT_COLOR_COLLECTION[0]} />);
    expect(container.querySelectorAll('.ColorInputItem')?.length).toEqual(
      DEFAULT_COLOR_COLLECTION.length,
    );
  });
  describe('when correct value passed', () => {
    it('shows check sign in target block', async () => {
      const { container } = render(<ColorInput value={DEFAULT_COLOR_COLLECTION[0]} />);
      expect(container.querySelector('.ColorInputItem')?.querySelector('.Icon')).toBeTruthy();
    });
  });
  describe('when clicks on item', () => {
    it('calls onChange props', async () => {
      const mockOnChange = jest.fn();
      const { container } = render(
        <ColorInput colorCollection={DEFAULT_COLOR_COLLECTION} onChange={mockOnChange} />,
      );
      fireEvent.click(container.querySelector('.ColorInputItem') as Element);
      expect(mockOnChange).toBeCalledTimes(1);
      expect(mockOnChange).toBeCalledWith(DEFAULT_COLOR_COLLECTION[0]);
    });
  });
  describe('when invalid color collection passed', () => {
    it('renders with default collection', async () => {
      // @ts-ignore
      const { container } = render(<ColorInput colorCollection="hello" />);
      expect(container.querySelectorAll('.ColorInputItem')?.length).toEqual(
        DEFAULT_COLOR_COLLECTION.length,
      );
    });
  });
});
