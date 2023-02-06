import React from 'react';
import { render } from '@testing-library/react';

import { Textarea } from '.';

describe('<Textarea />', () => {
  it('renders without errors', () => {
    render(<Textarea />);
  });
});
