import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';

import {
  PromoStatusFilterProps as ComponentProps,
  PromoStatusFilter,
  OPTIONS,
  Option,
  FILTER_NAME,
} from './PromoStatusFilter';

describe('<PromoStatusFilter />', () => {
  const DEFAULT_PROPS: ComponentProps = {
    onChange: jest.fn(),
    selectedValues: null,
  };

  afterEach(() => {
    jest.clearAllMocks();
  });

  const renderWithProps = (props: ComponentProps = DEFAULT_PROPS) => {
    return render(<PromoStatusFilter {...props} />);
  };

  const getInput = () => screen.getByRole('textbox');
  const selectOption = (option: Option) => {
    const input = getInput();
    fireEvent.change(input, { target: { value: option.label } });
    fireEvent.keyDown(input, { key: 'Enter', code: 'Enter' });
  };

  it('render without errors and contains input with empty values', () => {
    renderWithProps();

    const input = getInput();
    expect(input).toBeInTheDocument();
    expect(input).toHaveProperty('value', '');
  });

  it('changing of input calls the callback', async () => {
    renderWithProps();
    const optionForSelect = OPTIONS[0];

    selectOption(optionForSelect);

    expect(DEFAULT_PROPS.onChange).toHaveBeenLastCalledWith({ [FILTER_NAME]: [optionForSelect.value] });
    expect(getInput()).toHaveProperty('value', '');
  });

  it('changing of non-empty input calls the callback', async () => {
    const selectedOption = OPTIONS[0];
    const optionForSelect = OPTIONS[1];

    renderWithProps({
      ...DEFAULT_PROPS,
      selectedValues: [selectedOption.value],
    });

    selectOption(optionForSelect);

    expect(DEFAULT_PROPS.onChange).toHaveBeenLastCalledWith({
      [FILTER_NAME]: [selectedOption.value, optionForSelect.value],
    });
    expect(getInput()).toHaveProperty('value', '');
  });
});
