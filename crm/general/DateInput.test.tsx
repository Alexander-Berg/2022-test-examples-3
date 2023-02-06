import React from 'react';
import { render, screen, fireEvent, cleanup } from '@testing-library/react';
import { DateInput } from './DateInput';

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
    useIMask: () => ({
      ref: { current: null },
      maskRef: {
        current: mockMask,
      },
    }),
  };
});

describe('DateInput', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    cleanup();
  });

  it('renders with correct value', () => {
    const date = new Date('2021-04-21');
    const { container } = render(<DateInput value={date} />);
    expect(screen.getByTestId('DateInput')).toBeInTheDocument();
    expect((container.querySelector('input') as HTMLInputElement)?.value).toEqual('21.04.2021');
  });
  describe('if change input value', () => {
    it('calls onChange CB with correct value', () => {
      const onChange = jest.fn();
      const date = '17.12.2009';
      mockMask.typedValue = new Date('2009-12-17');
      const { container } = render(<DateInput onChange={onChange} />);
      fireEvent.change(container.querySelector('input')!, {
        target: { value: date },
      });
      expect(screen.getByTestId('DateInput')).toBeInTheDocument();
      expect(onChange).toBeCalledTimes(1);
      expect(onChange).toBeCalledWith(new Date('2009-12-17'));
    });
  });
  describe('if clicks on calendar item', () => {
    it('calls onChange CB with correct value', () => {
      const onChange = jest.fn();
      const date = new Date('2022-04-22');
      const { container } = render(<DateInput value={date} onChange={onChange} />);
      fireEvent.focus(container.querySelector('input')!);
      fireEvent.click(container.querySelector('.react-datepicker__day--006')!);
      expect(screen.getByTestId('DateInput')).toBeInTheDocument();
      expect(onChange).toBeCalledTimes(1);
      expect(onChange).toBeCalledWith(new Date('2022-04-06'));
    });
  });
});
