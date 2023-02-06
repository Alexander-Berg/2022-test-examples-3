import React from 'react';
import { render, cleanup } from '@testing-library/react';

import { UeeQuotePage } from './UeeQuotePage';

describe('<UeeQuotePage />', () => {
  afterEach(() => {
    cleanup();
  });

  it('renders without errors', () => {
    render(<UeeQuotePage match={{ params: { id: '5' } }} />);
  });
});
