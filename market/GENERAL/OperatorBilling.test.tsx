import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider } from 'src/test/setupTestProvider';
import { OperatorBillingPage } from './OperatorBilling';

describe('OperatorBillingPage::', () => {
  it('renders without errors', () => {
    const Provider = setupTestProvider();
    render(
      <Provider>
        <OperatorBillingPage />
      </Provider>
    );
  });
});
