import React from 'react';
import { render, screen } from '@testing-library/react';

import { FastFilter } from 'src/java/definitions';
import { FastFilterTable } from './FastFilterTable';

const setData = jest.fn();
const fastFilters = [
  {
    id: 0,
    name: 'foo',
    is_published: true,
  },
] as FastFilter[];

describe('<FastFilterTable />', () => {
  it('render empty table', () => {
    render(<FastFilterTable setData={setData} fastFilters={[]} />);

    const textOfEmptyTable = screen.getByText('Быстрые фильтры отсутствуют');

    expect(textOfEmptyTable).toBeInTheDocument();
  });

  it('render table with one row', () => {
    render(<FastFilterTable setData={setData} fastFilters={fastFilters} />);

    const table = screen.getByRole('table');

    expect(table).toBeInTheDocument();
  });
});
