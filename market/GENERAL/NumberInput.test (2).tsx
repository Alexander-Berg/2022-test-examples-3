import React from 'react';
import { render } from '@testing-library/react';

import { NumberInput } from '.';

describe('<NumberInput />', () => {
  it('renders without errors', () => {
    render(<NumberInput onChange={() => 0} value={1} />);
  });
});
