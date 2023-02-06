import { render } from '@testing-library/react';
import React from 'react';

import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import { FeaturesCreate } from './FeaturesCreate';

describe('FeaturesCreate::', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <FeaturesCreate />
      </Provider>
    );
  });
});
