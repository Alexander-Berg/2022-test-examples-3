import React from 'react';
import { render } from '@testing-library/react';

import { ParametersTable } from './ParametersTable';

describe('<ParametersTable/>', () => {
  it('renders without errors', () => {
    const data = [
      {
        rowId: 0,
        rowSettings: {
          backgroundColor: '#qqqqqq',
        },
        data: { col1: 1, col2: 'test' },
      },
      {
        rowId: 1,
        rowSettings: {
          backgroundColor: '#fff',
        },
        data: { col1: 2, col2: 'test' },
      },
    ];

    const columns = [
      {
        id: 'col1' as const,
        sortable: false,
        name: 'Column 1',
        renderer: () => <div>test</div>,
      },
    ];
    render(<ParametersTable data={data as any} columns={columns} onChangeSortState={() => 1} />);
  });
});
