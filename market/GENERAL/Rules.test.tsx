import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider } from 'src/test/setupTestProvider';
import { Rules } from './Rules';

describe('<Rules />', () => {
  it('renders without errors', () => {
    const Provider = setupTestProvider();

    render(
      <Provider>
        <Rules hid={1} />
      </Provider>
    );
  });
});
