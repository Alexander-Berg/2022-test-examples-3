import React from 'react';
import { render } from '@testing-library/react';

import { LoadersStatus } from '.';

describe('<LoadersStatus />', () => {
  it('renders without errors', () => {
    render(<LoadersStatus loaders={{}} />);
  });
});
