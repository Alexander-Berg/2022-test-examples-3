import React from 'react';
import userEvent from '@testing-library/user-event';
import { render, screen } from '@testing-library/react';
import { EditInfoPanel } from './EditInfoPanel';

describe('WFInput/EditInfoPanel', () => {
  describe('props.text', () => {
    describe('when defined', () => {
      it('renders text', () => {
        render(<EditInfoPanel text={'text'} />);

        expect(screen.getByText('text')).toBeInTheDocument();
      });
    });

    describe('when undefined', () => {
      it("doesn't render text", () => {
        render(<EditInfoPanel />);

        expect(screen.queryByText('text')).not.toBeInTheDocument();
      });
    });
  });

  describe('props.onCancelClick', () => {
    describe('when defined', () => {
      it('calls on click', () => {
        const handleCancelCLick = jest.fn();

        render(<EditInfoPanel onClearClick={handleCancelCLick} />);
        userEvent.click(screen.getByRole('button'));

        expect(handleCancelCLick).toBeCalled();
      });
    });

    describe('when undefined', () => {
      it("doesn't call on click", () => {
        const handleCancelCLick = jest.fn();

        render(<EditInfoPanel />);
        userEvent.click(screen.getByRole('button'));

        expect(handleCancelCLick).not.toBeCalled();
      });
    });
  });
});
