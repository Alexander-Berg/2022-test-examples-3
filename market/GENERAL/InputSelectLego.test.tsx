import React from 'react';
import { render } from '@testing-library/react';

import { InputModel } from 'src/utils/models';
import { InputSelectLego } from './InputSelectLego';

describe('<InputSelect />', () => {
  it('renders without errors', () => {
    const model = InputModel('1');
    const options = [
      { value: '1', content: 'option 1' },
      { value: '2', content: 'option 2' },
    ];
    render(<InputSelectLego model={model} options={options} />);
  });
});
