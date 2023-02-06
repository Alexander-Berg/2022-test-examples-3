import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { InputModel } from 'src/utils/models';
import { InputCheckbox } from './InputCheckbox';

describe('<InputCheckbox />', () => {
  it('renders correct checked value', () => {
    const model = InputModel(true);
    render(<InputCheckbox model={model} />);
    const input = screen.getByRole('checkbox') as HTMLInputElement;
    expect(input.checked).toBeTruthy();
  });

  it('renders correct unchecked value', () => {
    const model = InputModel(false);
    render(<InputCheckbox model={model} />);
    const input = screen.getByRole('checkbox') as HTMLInputElement;
    expect(input.checked).toBeFalsy();
  });

  it('changes value correctly', () => {
    const model = InputModel(false);
    render(<InputCheckbox model={model} />);
    const input = screen.getByRole('checkbox') as HTMLInputElement;
    userEvent.click(input);
    expect(model.value).toBeTruthy();
    userEvent.click(input);
    expect(model.value).toBeFalsy();
  });
});
