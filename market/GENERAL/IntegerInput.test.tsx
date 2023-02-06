import React from 'react';
import { render } from '@testing-library/react';

import { IntegerInput } from '.';

describe('<IntegerInput />', () => {
  it('renders without errors', () => {
    render(<IntegerInput onChange={() => 0} value={1} />);
  });
});
