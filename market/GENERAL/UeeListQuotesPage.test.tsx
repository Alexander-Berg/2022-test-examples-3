import React from 'react';
import { render, cleanup } from '@testing-library/react';

import { UeeListQuotesPage } from './UeeListQuotesPage';
import { TestingRoute } from 'src/test/setupApp';

describe('<UeeListQuotesPage />', () => {
  afterEach(() => {
    cleanup();
  });

  it('renders without errors', () => {
    render(
      <TestingRoute>
        <UeeListQuotesPage />
      </TestingRoute>
    );
  });
});
