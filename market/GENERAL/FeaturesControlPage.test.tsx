import { render } from '@testing-library/react';
import React from 'react';

import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import { FeaturesControlPage } from 'src/pages';

describe('FeaturesControlPage::', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <FeaturesControlPage />
      </Provider>
    );
  });
});
