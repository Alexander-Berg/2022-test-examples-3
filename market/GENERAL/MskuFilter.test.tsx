import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { act } from 'react-dom/test-utils';

import { MskuFilterProps as ComponentProps, MskuFilter, FILTER_NAME } from './MskuFilter';
import { APPLY_FILTER_DEBOUNCE_TIME } from 'src/pages/promo/components/FiltersForm/InputFilter/InputFilter';
import { delay } from 'src/pages/promo/utils';

describe('<MskuFilter />', () => {
  const DEFAULT_PROPS: ComponentProps = {
    onChange: jest.fn(),
    appliedValue: null,
  };

  afterEach(() => {
    jest.clearAllMocks();
  });

  const renderWithProps = (props: ComponentProps = DEFAULT_PROPS) => {
    return render(<MskuFilter {...props} />);
  };

  const getInput = () => screen.getByRole('textbox');

  it('render without errors and contains input with empty values', () => {
    renderWithProps();

    const input = getInput();
    expect(input).toBeInTheDocument();
    expect(input).toHaveProperty('value', '');
  });

  it('render without errors and contains input with appliedValue', () => {
    renderWithProps({
      ...DEFAULT_PROPS,
      appliedValue: [123, 312, 424242],
    });

    expect(getInput()).toHaveProperty('value', '123, 312, 424242');
  });

  it('changing of input calls the callback with delay', async () => {
    const CHANGE_VALUE_1 = 3123;
    const CHANGE_VALUE_2 = 4444;
    renderWithProps();
    fireEvent.change(getInput(), { target: { value: CHANGE_VALUE_1 } });

    expect(DEFAULT_PROPS.onChange).toHaveBeenCalledTimes(0);
    await act(() => delay(APPLY_FILTER_DEBOUNCE_TIME));
    expect(DEFAULT_PROPS.onChange).toHaveBeenLastCalledWith({ [FILTER_NAME]: [CHANGE_VALUE_1] });

    fireEvent.change(getInput(), { target: { value: `${CHANGE_VALUE_1},${CHANGE_VALUE_2}` } });
    expect(DEFAULT_PROPS.onChange).toHaveBeenCalledTimes(1);
    await act(() => delay(APPLY_FILTER_DEBOUNCE_TIME));
    expect(DEFAULT_PROPS.onChange).toHaveBeenLastCalledWith({ [FILTER_NAME]: [CHANGE_VALUE_1, CHANGE_VALUE_2] });
  });

  it('changing of input calls the callback with only correct values', async () => {
    const CHANGE_CORRECT_VALUE_1 = 3123;
    const CHANGE_INCORRECT_VALUE = '123sdf';

    renderWithProps();
    fireEvent.change(getInput(), { target: { value: `${CHANGE_CORRECT_VALUE_1}, ${CHANGE_INCORRECT_VALUE}` } });
    await act(() => delay(APPLY_FILTER_DEBOUNCE_TIME));
    expect(DEFAULT_PROPS.onChange).toHaveBeenLastCalledWith({ [FILTER_NAME]: [CHANGE_CORRECT_VALUE_1] });
  });
});
