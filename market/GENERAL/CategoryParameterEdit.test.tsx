import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider } from 'src/test/setupTestProvider';
import { CategoryParameterEditPage } from './CategoryParameterEdit';

describe('CategoryParameterEditPage', () => {
  it('renders without errors', () => {
    const Provider = setupTestProvider();
    render(
      <Provider>
        <CategoryParameterEditPage />
      </Provider>
    );
  });
});
