import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { act } from 'react-dom/test-utils';

import { delay } from 'src/pages/promo/utils';
import { FILTER_NAME, NameFilter, Props as ComponentProps } from './NameFilter';
import { APPLY_FILTER_DEBOUNCE_TIME } from '../InputFilter/InputFilter';

describe('<NameFilter />', () => {
  const DEFAULT_PROPS: ComponentProps = {
    onChange: jest.fn(),
    appliedValue: '',
  };

  afterEach(() => {
    jest.clearAllMocks();
  });

  const renderWithProps = (props: ComponentProps = DEFAULT_PROPS) => {
    return render(<NameFilter {...props} />);
  };
  const getInput = () => screen.getByRole('textbox');

  it('render without errors and contains input', () => {
    renderWithProps();
    const input = getInput();
    expect(input).toBeInTheDocument();
    expect(input).toHaveProperty('value', '');
  });

  it('contains applied value', () => {
    const APPLIED_VALUE = 'Applied value';
    renderWithProps({
      ...DEFAULT_PROPS,
      appliedValue: APPLIED_VALUE,
    });
    expect(getInput()).toHaveProperty('value', APPLIED_VALUE);
  });

  it('changing of input calls the callback with delay', async () => {
    const CHANGE_VALUE = 'Change value';
    renderWithProps();
    fireEvent.change(getInput(), { target: { value: CHANGE_VALUE } });

    expect(DEFAULT_PROPS.onChange).toHaveBeenCalledTimes(0);
    await act(() => delay(APPLY_FILTER_DEBOUNCE_TIME));
    expect(DEFAULT_PROPS.onChange).toHaveBeenCalledWith({ [FILTER_NAME]: CHANGE_VALUE });
  });
});
