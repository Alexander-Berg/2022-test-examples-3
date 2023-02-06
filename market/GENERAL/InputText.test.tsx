import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { InputModel } from 'src/utils/models';
import { InputText } from './InputText';

describe('<InputText />', () => {
  it('renders correct value', () => {
    const model = InputModel('some text');
    render(<InputText model={model} />);
    expect((screen.getByRole('textbox') as HTMLInputElement).value).toBe('some text');
  });

  it('changes value correctly', () => {
    const model = InputModel('some text');
    render(<InputText model={model} />);
    const input = screen.getByRole('textbox') as HTMLInputElement;
    userEvent.type(input, ' and new text');
    expect(model.value).toBe('some text and new text');
  });
});
