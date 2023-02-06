import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import { NameCell } from './NameCell';
import { RowData } from '../../Table.types';

describe('<NameCell />', () => {
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
        <NameCell {...testRowData} />
      </Provider>
    );
  });
});
