import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider } from 'src/test/setupTestProvider';
import { InheritedCategories } from './InheritedCategories';

describe('<InheritedCategories />', () => {
  it('renders without errors', () => {
    const Provider = setupTestProvider();
    render(
      <Provider>
        <InheritedCategories />
      </Provider>
    );
  });
});
