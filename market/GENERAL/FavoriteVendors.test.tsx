import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import { FavoriteVendors } from './FavoriteVendors';

describe('<FavoriteVendors />', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <FavoriteVendors />
      </Provider>
    );
  });
});
