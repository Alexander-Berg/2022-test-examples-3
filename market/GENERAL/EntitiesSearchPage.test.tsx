import React from 'react';
import { render } from '@testing-library/react';

import { EntitiesSearchPage } from './EntitiesSearchPage';
import { setupTestProvider } from 'test/setupApp';

describe('EntitiesSearchPage', () => {
  it('renders', () => {
    const { Provider } = setupTestProvider();
    render(
      <Provider>
        <EntitiesSearchPage />
      </Provider>
    );
  });
});
