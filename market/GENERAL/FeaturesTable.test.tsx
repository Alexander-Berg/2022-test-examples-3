import { render } from '@testing-library/react';
import React from 'react';

import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import { FeaturesTable } from './FeaturesTable';

describe('FeaturesTable::', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <FeaturesTable isLoading={false} />
      </Provider>
    );
  });
});
