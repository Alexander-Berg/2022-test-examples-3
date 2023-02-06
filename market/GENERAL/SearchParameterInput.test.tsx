import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import { SearchParameterInput } from './SearchParameterInput';

describe('<SearchParameterInput />', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <SearchParameterInput searchText="" onChange={jest.fn()} />
      </Provider>
    );
  });
});
