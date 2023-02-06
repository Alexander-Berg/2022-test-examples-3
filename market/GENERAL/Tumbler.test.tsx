import React from 'react';
import { render } from '@testing-library/react';

import { Tumbler } from './Tumbler';

describe('<Tumbler />', () => {
  it('renders without errors', () => {
    render(<Tumbler checked={false} onChange={jest.fn()} />);
  });
});
