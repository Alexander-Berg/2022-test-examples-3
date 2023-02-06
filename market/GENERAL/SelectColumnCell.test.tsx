import React from 'react';

import { SelectColumnCell } from './SelectColumnCell';
import { setupTestProvider } from 'src/test/setupTestProvider';
import { renderWithProvider } from 'src/test/utils/utils';

describe('<SelectColumnCell/>', () => {
  it('renders without errors', () => {
    renderWithProvider(<SelectColumnCell />, setupTestProvider());
  });
});
