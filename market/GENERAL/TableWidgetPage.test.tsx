import React from 'react';
import { render } from '@testing-library/react';

import { TableWidgetPage } from './TableWidgetPage';
import { setupTestProvider } from 'test/setupApp';

describe('TableWidgetPage', () => {
  it('renders', () => {
    const { Provider } = setupTestProvider();
    render(
      <Provider>
        <TableWidgetPage />
      </Provider>
    );
  });
});
