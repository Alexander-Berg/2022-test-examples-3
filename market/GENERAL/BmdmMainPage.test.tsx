import React from 'react';
import { render } from '@testing-library/react';

import { BmdmMainPage } from './BmdmMainPage';
import { setupTestProvider } from 'test/setupApp';

describe('BmdmMainPage', () => {
  it('renders', () => {
    const { Provider } = setupTestProvider();
    render(
      <Provider>
        <BmdmMainPage />
      </Provider>
    );
  });
});
