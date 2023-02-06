import React from 'react';
import { render } from '@testing-library/react';

import { TreeWidgetPage } from './TreeWidgetPage';
import { setupTestProvider } from 'test/setupApp';

describe('TreeWidgetPage', () => {
  it('renders', () => {
    const { Provider } = setupTestProvider();
    render(
      <Provider>
        <TreeWidgetPage />
      </Provider>
    );
  });
});
