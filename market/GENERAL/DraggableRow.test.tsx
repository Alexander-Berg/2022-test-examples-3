import React from 'react';
import { render } from '@testing-library/react';
import { Table, TableBody } from '@yandex-market/mbo-components';

import { DraggableRow } from './DraggableRow';
import { ParameterTableRow } from '../types';
import { COLUMNS } from '../constants';

const ROW = {
  id: 0,
  name: 'Дата старта продаж',
  xslName: 'SaleDateGlob',
  publish: false,
  index: 0,
  actions: {
    isHidden: false,
    onHide: () => null,
    onShow: () => null,
    paramId: 2,
  },
} as ParameterTableRow;

describe('<DraggableRow/>', () => {
  it('renders without errors', () => {
    render(
      <Table size="m">
        <TableBody>
          <DraggableRow columns={COLUMNS.slice(1, -1)} row={ROW} />
        </TableBody>
      </Table>
    );
  });
});
