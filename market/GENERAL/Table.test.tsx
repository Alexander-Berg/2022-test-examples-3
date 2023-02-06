import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import { Table } from './Table';
import { RowData } from './Table.types';
import TableUtils from './Table.utils';
import { ACTIONS, DISPLAY_NAME, ID, NAME_COLUMN } from '../../columns';

const TEST_DATA: RowData[] = [TableUtils.getNewRowData(0)];

describe('<Table />', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <Table
          data={TEST_DATA}
          onDelete={() => 1}
          onChange={() => 1}
          columns={[ID, NAME_COLUMN, DISPLAY_NAME, ACTIONS]}
        />
      </Provider>
    );
  });
});
