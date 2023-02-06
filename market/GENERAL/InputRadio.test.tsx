import React from 'react';
import { render } from '@testing-library/react';

import { InputModel } from 'src/utils/models';
import { InputRadio } from './InputRadio';

describe('<InputRadio />', () => {
  it('renders without errors', () => {
    const model = InputModel('1');
    const options = [
      { value: '1', label: 'option 1' },
      { value: '2', label: 'option 2' },
    ];
    render(<InputRadio model={model} options={options} />);
  });
});
