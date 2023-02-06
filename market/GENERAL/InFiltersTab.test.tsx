import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import { InFiltersTab } from './InFiltersTab';

describe('<InFiltersTab />', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <InFiltersTab hid={0} />
      </Provider>
    );
  });
});
