import { render } from '@testing-library/react';
import React from 'react';
import { TableCell, TableRow } from '@yandex-market/mbo-components';

import { PagedTable } from './PagedTable';

describe('PagedTable::', () => {
  it('renders without errors', () => {
    render(<PagedTable />);
  });
  it('renders with loader', () => {
    render(<PagedTable showSpin />);
  });
  it('renders with data', () => {
    render(
      <PagedTable
        data={['test1,test2']}
        HeadRenderer={() => {
          return <td>HEAD</td>;
        }}
        RowRenderer={() => {
          return (
            <TableRow>
              <TableCell>Row</TableCell>
            </TableRow>
          );
        }}
      />
    );
  });
});
