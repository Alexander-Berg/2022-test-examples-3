import React from 'react';
import { render, cleanup } from '@testing-library/react';

import { UeeCreatePage } from './UeeCreatePage';

describe('<UeeCreatePage />', () => {
  afterEach(() => {
    cleanup();
  });

  it('renders without errors', () => {
    render(<UeeCreatePage />);
  });
});
