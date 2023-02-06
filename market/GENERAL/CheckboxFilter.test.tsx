import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';

import { CheckboxFilter, PermittedFilters, Props as ComponentProps } from './CheckboxFilter';

describe('<CheckboxFilter />', () => {
  const DEFAULT_PROPS: ComponentProps = {
    onChange: jest.fn(),
    isChecked: false,
    filterFieldName: PermittedFilters.PARTICIPATES,
  };

  afterEach(() => {
    jest.clearAllMocks();
  });

  const renderWithProps = (props: ComponentProps = DEFAULT_PROPS) => {
    return render(<CheckboxFilter {...props} />);
  };
  const getCheckbox = () => screen.getByRole('checkbox');

  it('render without errors', () => {
    renderWithProps();
    expect(getCheckbox()).toBeInTheDocument();
  });

  it('checkbox is not checked with false property', () => {
    renderWithProps({ ...DEFAULT_PROPS, isChecked: false });
    expect(getCheckbox()).toHaveProperty('checked', false);
  });

  it('checkbox is checked with true property', () => {
    renderWithProps({ ...DEFAULT_PROPS, isChecked: true });
    expect(getCheckbox()).toHaveProperty('checked', true);
  });

  it('click calls the callback with true value', () => {
    renderWithProps({ ...DEFAULT_PROPS, isChecked: false });
    fireEvent.click(getCheckbox());
    expect(DEFAULT_PROPS.onChange).toBeCalledWith({ [DEFAULT_PROPS.filterFieldName]: true });
  });

  it('click calls the callback with false value', () => {
    renderWithProps({ ...DEFAULT_PROPS, isChecked: true });
    fireEvent.click(getCheckbox());
    expect(DEFAULT_PROPS.onChange).toBeCalledWith({ [DEFAULT_PROPS.filterFieldName]: false });
  });
});
