import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import TableUtils from '../../Table.utils';
import { VendorComment } from './VendorComment';

const data = TableUtils.getNewRowData(0);
describe('<VendorComment />', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <VendorComment {...data} />
      </Provider>
    );
  });
});
