import React from 'react';
import { render } from '@testing-library/react';

import { Switch } from '.';

describe('<Switch />', () => {
  it('renders without errors', () => {
    render(<Switch onSelect={() => 0} options={[]} />);
  });
});
