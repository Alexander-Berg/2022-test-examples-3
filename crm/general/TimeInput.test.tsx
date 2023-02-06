import React from 'react';
import { render, screen, fireEvent, cleanup } from '@testing-library/react';
import { TimeInput } from './TimeInput';

const mockMask = {
  typedValue: undefined as Date | undefined,
  updateValue: jest.fn(),
  masked: {
    isComplete: true,
  },
  on: () => {},
  off: () => {},
};

jest.mock('@crm/react-imask', () => {
  return {
    IMask: {
      MaskedRange: jest.fn(),
    },
    useIMask: () => ({
      ref: { current: null },
      maskRef: {
        current: mockMask,
      },
    }),
  };
});

describe('TimeInput', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    cleanup();
  });

  it('renders with correct value', () => {
    const time = '13:37';
    const { container } = render(<TimeInput value={time} />);
    expect(screen.getByTestId('TimeInput')).toBeInTheDocument();
    expect((container.querySelector('input') as HTMLInputElement)?.value).toEqual(time);
  });
  describe('if change input value', () => {
    it('calls onChange CB with correct value', () => {
      const onChange = jest.fn();
      const time = '14:17';
      const { container } = render(<TimeInput onChange={onChange} />);
      fireEvent.change(container.querySelector('input')!, {
        target: { value: time },
      });
      expect(onChange).toBeCalledTimes(1);
      expect(onChange).toBeCalledWith(time);
    });
  });
  describe('if clicks on menu item', () => {
    it('calls onChange CB with correct value', () => {
      const onChange = jest.fn();
      const { container } = render(<TimeInput onChange={onChange} />);
      fireEvent.focus(container.querySelector('input') as HTMLInputElement);
      const targetItem = screen.getByText('01:30');
      fireEvent.click(targetItem);
      expect(onChange).toBeCalledTimes(1);
      expect(onChange).toBeCalledWith('01:30');
    });
  });
});
