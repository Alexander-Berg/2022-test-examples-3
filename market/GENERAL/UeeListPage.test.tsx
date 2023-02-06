import React from 'react';
import { render, cleanup } from '@testing-library/react';

import { UeeListPage } from './UeeListPage';
import { TestingRoute } from 'src/test/setupApp';

describe('<UeeListPage />', () => {
  afterEach(() => {
    cleanup();
  });

  it('renders without errors', () => {
    render(
      <TestingRoute>
        <UeeListPage />
      </TestingRoute>
    );
  });

  it('renders quota without errors', () => {
    const match = { params: { quote: 'some quote' } };
    render(
      <TestingRoute>
        <UeeListPage match={match} />
      </TestingRoute>
    );
  });
});
