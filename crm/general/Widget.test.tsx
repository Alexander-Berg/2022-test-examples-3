import React from 'react';
import { render, fireEvent, screen } from '@testing-library/react';
import { Provider, Store } from '../../State';
import { Widget } from './Widget';
import css from './Widget.module.css';

describe('components/Widget', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  const mockTargetMeta = { type: 'Mail', id: 123 };

  describe('on click', () => {
    const mockSetup = jest.fn();
    const mockOpen = jest.fn();
    const mockEmit = jest.fn();

    it('calls state.open', () => {
      render(
        <Provider
          store={({ setup: mockSetup, open: mockOpen, emit: mockEmit } as unknown) as Store}
        >
          <Widget targetMeta={mockTargetMeta} />
        </Provider>,
      );

      fireEvent.click(screen.getByRole('button'));

      expect(mockOpen).toBeCalledTimes(1);
    });
  });

  describe('props.className', () => {
    it('adds className to node classList', () => {
      const className = 'Hello';
      render(<Widget targetMeta={mockTargetMeta} className={className} />);

      const node = document.body.querySelector(`.${css.Widget}`) as HTMLElement;

      expect(node.classList.contains(className)).toBeTruthy();
    });
  });
});
