import React, { useRef, FC } from 'react';
import userEvent from '@testing-library/user-event';
import { render, screen } from '@testing-library/react';
import { useFocusKeeping } from './useFocusKeeping';

describe('hooks/useFocusKeeping', () => {
  const TestComponent: FC = () => {
    const inputRef = useRef<HTMLInputElement>(null);
    useFocusKeeping(inputRef);
    return <input ref={inputRef} data-testid="test component" />;
  };

  describe('on mount', () => {
    it('focuses the element', () => {
      render(<TestComponent />);

      expect(screen.getByRole('textbox')).toBe(document.activeElement);
    });

    it('adds keydown event listener', () => {
      const actualAddEventListener = document.addEventListener;
      const mockAddEventListener = jest.fn();
      document.addEventListener = mockAddEventListener;
      render(<TestComponent />);

      expect(mockAddEventListener.mock.calls[0][0]).toBe('keydown');
      document.addEventListener = actualAddEventListener;
    });
  });

  describe('on unmount', () => {
    it('removes keydown event listener', () => {
      const actualRemoveEventListener = document.removeEventListener;
      const mockRemoveEventListener = jest.fn();
      document.removeEventListener = mockRemoveEventListener;
      const { unmount } = render(<TestComponent />);

      unmount();

      expect(mockRemoveEventListener.mock.calls[0][0]).toBe('keydown');
      document.removeEventListener = actualRemoveEventListener;
    });
  });

  describe('on key down', () => {
    it('focuses the element', () => {
      render(
        <>
          <TestComponent />
          <div tabIndex={0} data-testid="focused" />
        </>,
      );
      screen.getByTestId('focused').focus();

      userEvent.type(document.body, '1');

      expect(screen.getByTestId('test component')).toBe(document.activeElement);
    });

    it('changes the element value', () => {
      render(
        <>
          <TestComponent />
          <div tabIndex={0} data-testid="focused" />
        </>,
      );
      screen.getByTestId('focused').focus();

      userEvent.type(document.body, '1');

      expect(screen.getByTestId('test component')).toHaveValue('1');
    });
  });
});
