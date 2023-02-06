import { cleanup, render } from '@testing-library/react';
import React from 'react';
import { setupTestProvider, TestProviderType } from 'src/test/setupProvider';

import { ManageButtons } from './ManageButtons';

describe('<ManageButtons />', () => {
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
        <ManageButtons id={5} />
      </Provider>
    );
  });
});
