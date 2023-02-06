import React from 'react';
import { render } from '@testing-library/react';

import { Select } from './Select';

describe('<Select />', () => {
  it('renders without errors', () => {
    render(<Select />);
  });
});
