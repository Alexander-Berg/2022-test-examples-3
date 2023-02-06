import React from 'react';
import { render } from '@testing-library/react';

import { DiagnosticFilter } from './DiagnosticFilter';
import { setupTestProvider } from 'src/test/utils';

describe('<DiagnosticFilter />', () => {
  test('render', () => {
    const { Provider } = setupTestProvider();
    render(
      <Provider>
        <DiagnosticFilter />
      </Provider>
    );
  });
});
