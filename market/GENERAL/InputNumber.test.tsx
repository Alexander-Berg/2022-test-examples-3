import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { InputModel } from 'src/utils/models';
import { InputNumber } from './InputNumber';

describe('<InputNumber />', () => {
  it('renders correct value', () => {
    const model = InputModel(25);
    render(<InputNumber model={model} />);
    expect((screen.getByRole('textbox') as HTMLInputElement).value).toBe('25');
  });

  it('changes value correctly', () => {
    const model = InputModel(25);
    render(<InputNumber model={model} />);
    const input = screen.getByRole('textbox') as HTMLInputElement;
    input.setSelectionRange(0, 2);
    userEvent.type(input, '50');
    expect(model.value).toBe(50);
  });
});
