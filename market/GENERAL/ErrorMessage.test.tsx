import React from 'react';
import { render } from '@testing-library/react';

import { ErrorMessage } from '.';

describe('<ErrorMessage />', () => {
  it('renders without errors', () => {
    render(<ErrorMessage title="some text" />);
  });
});
