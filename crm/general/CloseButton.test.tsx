import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import CloseButton from './CloseButton';

jest.mock('services/Config', () => ({
  config: {
    value: {
      features: {
        newDesignFilters: true,
      },
    },
  },
}));

describe('CloseButton', () => {
  describe('props.onClose', () => {
    describe('when defined', () => {
      it('calls on click', () => {
        const onClose = jest.fn();
        render(<CloseButton onClose={onClose} />);

        userEvent.click(screen.getByRole('button'));
        expect(onClose).toBeCalled();
      });
    });
  });

  describe('props.direction', () => {
    describe('when equals right', () => {
      it('calls on click', () => {
        const direction = 'right';
        const { container } = render(<CloseButton onClose={jest.fn()} direction={direction} />);

        expect(container.getElementsByClassName(`CloseButton_${direction}`).length).toBe(1);
      });
    });

    describe('when equals left', () => {
      it('calls on click', () => {
        const direction = 'left';
        const { container } = render(<CloseButton onClose={jest.fn()} direction={direction} />);

        expect(container.getElementsByClassName(`CloseButton_${direction}`).length).toBe(1);
      });
    });
  });
});
