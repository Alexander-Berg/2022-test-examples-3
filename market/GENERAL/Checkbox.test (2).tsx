import React from 'react';
import { render } from '@testing-library/react';

import { Checkbox } from '.';

describe('<Checkbox />', () => {
  it('renders without errors', () => {
    render(<Checkbox />);
  });
});
