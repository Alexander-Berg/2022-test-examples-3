import React from 'react';
import { render } from '@testing-library/react';

import { LandingsTable } from './LandingsTable';

describe('<LandingsTable/>', () => {
  it('renders without errors', () => {
    const data = [
      {
        rowId: 0,
        rowSettings: {
          backgroundColor: '#qqqqqq',
        },
        data: { id: 0, col1: 1, col2: 'test' },
      },
      {
        rowId: 1,
        rowSettings: {
          backgroundColor: '#fff',
        },
        data: { id: 1, col1: 2, col2: 'test' },
      },
    ];

    const columns = [
      {
        id: 'col1' as const,
        sortable: false,
        name: 'Column 1',
        renderer: () => <div>test</div>,
        convertor: () => 'test',
      },
    ];
    render(<LandingsTable data={data as any} columns={columns} onChangeSortState={() => 1} />);
  });
});
