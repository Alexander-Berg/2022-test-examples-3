import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { RateButton } from './RateButton';

describe('design/Audio/RateButton', () => {
  it('shows rate', () => {
    render(<RateButton rate={1} />);

    expect(screen.getByText('1')).toBeInTheDocument();
  });

  it('disables button when isDisabled=true', () => {
    render(<RateButton rate={1} isDisabled />);

    expect(screen.getByRole('button')).toBeDisabled();
  });

  it('calls onRateChange with toggled value', () => {
    const handleRateChange = jest.fn(() => {});

    const { rerender } = render(<RateButton rate={1} onRateChange={handleRateChange} />);

    userEvent.click(screen.getByRole('button'));

    expect(handleRateChange).toBeCalledWith(1.5);

    rerender(<RateButton rate={1.5} onRateChange={handleRateChange} />);

    userEvent.click(screen.getByRole('button'));

    expect(handleRateChange).toBeCalledWith(2);

    rerender(<RateButton rate={2} onRateChange={handleRateChange} />);

    userEvent.click(screen.getByRole('button'));

    expect(handleRateChange).toBeCalledWith(1);
  });
});
