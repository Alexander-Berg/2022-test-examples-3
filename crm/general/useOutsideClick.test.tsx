import React, { FC, useRef } from 'react';
import userEvent from '@testing-library/user-event';
import { render, screen } from '@testing-library/react';
import { useOutsideClick } from './useOutsideClick';

const TestComponent: FC<{ onOutsideClick: () => void }> = ({ onOutsideClick }) => {
  const insideRef = useRef<HTMLSpanElement>(null);
  useOutsideClick(insideRef, onOutsideClick);

  return (
    <div>
      <span data-testid="outside">outside</span>
      <span data-testid="inside" ref={insideRef}>
        inside
      </span>
    </div>
  );
};

describe('hooks/useOutsideClick', () => {
  let addEventListenerSpy: jest.SpyInstance;
  let removeEventListenerSpy: jest.SpyInstance;
  beforeAll(() => {
    addEventListenerSpy = jest.spyOn(document, 'addEventListener');
    removeEventListenerSpy = jest.spyOn(document, 'removeEventListener');
  });

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('when was clicked inside ref node', () => {
    it(`doesn't call onOutsideClick`, () => {
      const handleOutsideClick = jest.fn();
      render(<TestComponent onOutsideClick={handleOutsideClick} />);

      userEvent.click(screen.getByTestId('inside'));

      expect(handleOutsideClick).toBeCalledTimes(0);
    });
  });

  describe('when was clicked outside ref node', () => {
    it('calls onOutsideClick', () => {
      const handleOutsideClick = jest.fn();
      render(<TestComponent onOutsideClick={handleOutsideClick} />);

      userEvent.click(screen.getByTestId('outside'));

      expect(handleOutsideClick).toBeCalledTimes(1);
    });
  });

  describe('when external handler was changed', () => {
    it('removes event listener', () => {
      const { rerender } = render(<TestComponent onOutsideClick={jest.fn()} />);
      const listener = addEventListenerSpy.mock.calls[0][1];

      rerender(<TestComponent onOutsideClick={jest.fn()} />);

      expect(removeEventListenerSpy).toBeCalledTimes(1);
      expect(removeEventListenerSpy).toBeCalledWith('mousedown', listener, expect.any(Boolean));
    });
  });

  describe('when component unmounts', () => {
    it('removes event listener', () => {
      const { unmount } = render(<TestComponent onOutsideClick={jest.fn()} />);
      const listener = addEventListenerSpy.mock.calls[0][1];

      unmount();

      expect(removeEventListenerSpy).toBeCalledTimes(1);
      expect(removeEventListenerSpy).toBeCalledWith('mousedown', listener, expect.any(Boolean));
    });
  });
});
