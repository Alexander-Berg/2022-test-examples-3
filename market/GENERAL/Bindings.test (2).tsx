import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider } from 'src/test/setupTestProvider';
import { Bindings } from './Bindings';

describe('<Bindings />', () => {
  it('renders without errors', () => {
    const Provider = setupTestProvider();
    render(
      <Provider>
        <Bindings parameterId={0} />
      </Provider>
    );
  });
});
