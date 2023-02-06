import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import { InModalFilterTab } from './InModalFilterTab';

describe('<InModalFilterTab />', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <InModalFilterTab hid={0} />
      </Provider>
    );
  });
});
