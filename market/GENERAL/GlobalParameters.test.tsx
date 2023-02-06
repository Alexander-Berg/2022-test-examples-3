import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import { GlobalParametersPage } from './GlobalParameters';

describe('GlobalParametersPage::', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <GlobalParametersPage />
      </Provider>
    );
  });
});
