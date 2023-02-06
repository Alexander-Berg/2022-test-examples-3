import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import { OutputTemplate } from './OutputTemplate';

describe('<OutputTemplate />', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <OutputTemplate hid={0} />
      </Provider>
    );
  });
});
