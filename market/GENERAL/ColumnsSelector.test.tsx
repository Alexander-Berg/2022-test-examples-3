import React from 'react';
import { render } from '@testing-library/react';

import { GLOBAL_PARAMETERS_COLUMNS } from '../../components/ParametersTable/constants';
import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import { ColumnsSelector } from './ColumnsSelector';

describe('<ColumnsSelector/>', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <ColumnsSelector columns={GLOBAL_PARAMETERS_COLUMNS} visibleColumns={[]} setVisibleColumns={() => null} />
      </Provider>
    );
  });
});
