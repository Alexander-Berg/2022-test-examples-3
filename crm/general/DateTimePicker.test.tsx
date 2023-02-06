import React from 'react';
import { screen, render, act, fireEvent } from '@testing-library/react';
import DateTimePicker from './index';

describe('DateTimePicker', () => {
  it('renders clear button', () => {
    const { container } = render(<DateTimePicker hasClear />);

    expect(container.firstChild).toMatchSnapshot();
  });

  it('supports null value', () => {
    const mockChange = jest.fn();

    act(() => {
      render(<DateTimePicker onChange={mockChange} value="2021-03-16T00:00:00.0000000+03:00" />);
    });

    act(() => {
      fireEvent.input(screen.getByDisplayValue('15.03.2021'), { target: { value: '' } });
    });

    expect(mockChange).toBeCalledTimes(1);
    expect(mockChange).toBeCalledWith(null);
  });
});
