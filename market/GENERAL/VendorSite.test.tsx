import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import TableUtils from '../../Table.utils';
import { VendorSite } from './VendorSite';

const data = TableUtils.getNewRowData(0);
describe('<VendorSite />', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <VendorSite {...data} />
      </Provider>
    );
  });
});
