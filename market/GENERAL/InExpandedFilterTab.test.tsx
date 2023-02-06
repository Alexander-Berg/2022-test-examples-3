import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import { InExpandedFilterTab } from './InExpandedFilterTab';

describe('<InExpandedFilterTab />', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <InExpandedFilterTab hid={0} />
      </Provider>
    );
  });
});
