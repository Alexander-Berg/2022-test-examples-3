import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider } from 'src/test/setupTestProvider';
import { ModelListPage } from 'src/pages';

describe('ModelListPage::', () => {
  it('renders without errors', () => {
    const Provider = setupTestProvider();
    expect(() =>
      render(
        <Provider>
          <ModelListPage />
        </Provider>
      )
    ).not.toThrow();
  });
});
