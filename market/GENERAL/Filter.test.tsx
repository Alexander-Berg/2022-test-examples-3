import React from 'react';
import { render } from '@testing-library/react';

import { GlobalCategoryParameterFilter } from 'src/java/definitions';
import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import { Filter } from './Filter';

describe('<Filter />', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <Filter filters={{} as GlobalCategoryParameterFilter} onChange={jest.fn()} filterConfig={[]} />
      </Provider>
    );
  });
});
