import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import { Table } from './Table';

describe('<Table />', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <Table />
      </Provider>
    );
  });
});
