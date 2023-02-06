import React from 'react';
import { render } from '@testing-library/react';

import { InputModel } from 'src/utils/models';
import { InputSelect } from './InputSelect';

describe('<InputSelect />', () => {
  it('renders without errors', () => {
    const model = InputModel('1');
    const options = [
      { value: '1', label: 'option 1' },
      { value: '2', label: 'option 2' },
    ];
    render(<InputSelect model={model} options={options} />);
  });
});
