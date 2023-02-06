import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { WeightInputCell } from '.';

describe('<WeightInputCell />', () => {
  it('should show the passed value', () => {
    const handleChange = jest.fn();
    render(<WeightInputCell value={120} onChange={handleChange} />);

    expect(screen.getByRole('spinbutton')).toHaveDisplayValue('120');
  });

  it('should change value', async () => {
    const handleChange = jest.fn();
    render(<WeightInputCell value={120} onChange={handleChange} />);

    const input = screen.getByRole('spinbutton');
    userEvent.clear(input);
    userEvent.type(input, '100');

    await waitFor(() => {
      expect(handleChange).toBeCalledWith(100);
    });
  });

  it('should be show error message for typing an invalid value', async () => {
    const handleChange = jest.fn();
    render(<WeightInputCell value={100} onChange={handleChange} />);

    const input = screen.getByRole('spinbutton');
    userEvent.clear(input);
    userEvent.type(input, '1.5');

    expect(await screen.findByText(/Введите корректное целое число/i)).toBeInTheDocument();
    expect(handleChange).toBeCalledTimes(0);
  });
});
