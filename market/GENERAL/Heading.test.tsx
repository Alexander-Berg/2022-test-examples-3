import React from 'react';
import { render } from '@testing-library/react';

import { Heading } from './Heading';

describe('<Heading />', () => {
  it('renders without errors', () => {
    render(<Heading title="Test" />);
  });
});
