import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import { BrandCell } from './BrandCell';
import { RowData } from '../../Table.types';

describe('<BrandCell />', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    const testRowData: RowData = {
      paramOptionName: {
        notAlias: false,
        morphological: false,
        allowFilter: false,
        inTop: false,
        name: '',
        typePrefix: '',
      },
      vendorInfo: {
        comment: 'test',
      },
      meta: {
        disable: false,
      },
    } as RowData;
    render(
      <Provider>
        <BrandCell {...testRowData} />
      </Provider>
    );
  });
});
