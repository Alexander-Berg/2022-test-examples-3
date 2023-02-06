import React from 'react';
import { render } from '@testing-library/react';

import { ParameterContent } from './ParameterContent';
import { setupTestProvider } from 'src/test/setupTestProvider';

describe('<ParameterContent />', () => {
  it('renders without errors', () => {
    const Provider = setupTestProvider();
    render(
      <Provider>
        <ParameterContent />
      </Provider>
    );
  });
});
