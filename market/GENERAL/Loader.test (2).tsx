import React from 'react';
import { render } from '@testing-library/react';

import { Loader } from '.';

describe('<Loader />', () => {
  it('renders without errors', () => {
    render(<Loader />);
  });
});
