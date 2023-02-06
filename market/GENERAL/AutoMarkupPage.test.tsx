import React from 'react';
import { render } from '@testing-library/react';

import { AutoMarkupPage } from './AutoMarkupPage';
import { setupTestProvider } from 'test/setupApp';

describe('AutoMarkupPage', () => {
  it('renders', () => {
    const { Provider } = setupTestProvider();
    render(
      <Provider>
        <AutoMarkupPage />
      </Provider>
    );
  });
});
