import React from 'react';
import { render, cleanup } from '@testing-library/react';
import { setupTestProvider, TestProviderType } from 'src/test/setupProvider';

import { UeeTaskPage } from './UeeTaskPage';

describe('<UeeTaskPage />', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  afterEach(() => {
    cleanup();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <UeeTaskPage match={{ params: { id: '5' } }} />
      </Provider>
    );
  });
});
