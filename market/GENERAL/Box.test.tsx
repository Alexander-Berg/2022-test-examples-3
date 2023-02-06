import React from 'react';
import { render } from '@testing-library/react';

import { Box } from '.';

describe('<Box />', () => {
  it('renders without errors', () => {
    render(<Box />);
  });
});
