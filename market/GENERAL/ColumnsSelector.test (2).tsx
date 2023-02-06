import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import { ColumnsSelector } from './ColumnsSelector';
import { CATEGORY_LANDINGS_COLUMNS } from '../Table/Table.constants';

describe('<ColumnsSelector/>', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <ColumnsSelector columns={CATEGORY_LANDINGS_COLUMNS} visibleColumns={[]} setVisibleColumns={() => null} />
      </Provider>
    );
  });
});
