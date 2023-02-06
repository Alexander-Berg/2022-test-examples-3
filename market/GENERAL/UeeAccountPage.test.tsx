import React from 'react';
import { render, cleanup } from '@testing-library/react';

import { UeeAccountPage } from './UeeAccountPage';

describe('<UeeAccountPage />', () => {
  afterEach(() => {
    cleanup();
  });

  it('renders without errors', () => {
    render(<UeeAccountPage />);
  });
});
