import React, { FC } from 'react';
import { render } from '@testing-library/react';

import { Column, RowData } from '../Table.types';
import { TableBodyRow } from './TableBodyRow';
import TableUtils from '../Table.utils';

const TEST_DATA: RowData = TableUtils.getNewRowData(0);

const CellRenderer: FC<RowData> = ({ valueId }) => {
  return <div>{valueId}</div>;
};

const col1: Column = {
  width: 100,
  renderer: CellRenderer,
  title: 'test column',
  id: 'col1',
};

describe('<TableBodyRow />', () => {
  it('renders without errors', () => {
    render(
      <table>
        <tbody>
          <TableBodyRow row={TEST_DATA} onChange={() => 1} columns={[col1]} />
        </tbody>
      </table>
    );
  });
});
