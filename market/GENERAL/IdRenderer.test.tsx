import React from 'react';
import { render, screen } from '@testing-library/react';

import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import { IdRenderer } from './IdRenderer';

describe('<IdRenderer/>', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <IdRenderer />
      </Provider>
    );
  });

  it('renders complete value', () => {
    render(
      <Provider>
        <IdRenderer value={5} />
      </Provider>
    );
    expect(screen.getByText('5')).toBeDefined();
  });
});
