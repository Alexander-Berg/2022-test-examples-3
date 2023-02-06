import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import { ModelEditorPage } from './ModelEditorPage';

describe('ModelEditorPage::', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <ModelEditorPage />
      </Provider>
    );
  });
});
