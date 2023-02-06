import React from 'react';
import { render } from '@testing-library/react';

import { InputModel } from 'src/utils/models';
import { InputRadioButton } from './InputRadioButton';

describe('<InputRadioButton />', () => {
  it('renders without errors', () => {
    const model = InputModel('1');
    const options = [
      { value: '1', children: 'option 1' },
      { value: '2', children: 'option 2' },
    ];
    render(<InputRadioButton model={model} options={options} />);
  });
});
