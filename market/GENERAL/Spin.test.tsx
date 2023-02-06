import React from 'react';
import { render } from '@testing-library/react';

import { Spin } from '.';

describe('<Spin />', () => {
  it('renders without errors', () => {
    render(<Spin />);
  });
});
