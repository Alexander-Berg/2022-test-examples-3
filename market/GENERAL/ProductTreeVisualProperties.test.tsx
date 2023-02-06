import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider } from 'src/test/setupTestProvider';
import { ProductTreeVisualProperties } from '.';

describe('<ProductTreeVisualProperties />', () => {
  it('renders without errors', () => {
    const Provider = setupTestProvider();
    render(
      <Provider>
        <ProductTreeVisualProperties />
      </Provider>
    );
  });
});
