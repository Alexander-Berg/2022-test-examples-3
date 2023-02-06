import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider } from 'src/test/setupTestProvider';
import { GlobalParameterEditPage } from './GlobalParameterEdit';

describe('GlobalParameterEditPage::', () => {
  it('renders without errors', () => {
    const Provider = setupTestProvider();
    render(
      <Provider>
        <GlobalParameterEditPage />
      </Provider>
    );
  });
});
