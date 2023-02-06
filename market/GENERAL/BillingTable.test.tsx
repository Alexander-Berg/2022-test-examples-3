import React from 'react';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { renderWithProvider } from 'src/test/setupTestProvider';
import { BillingTable } from '.';
import { OrderedColumn, SortingOrder } from 'src/java/definitions';

describe('<BillingTable />', () => {
  it('should be shown empty content', () => {
    renderWithProvider(<BillingTable items={[]} onSort={jest.fn()} />);

    expect(screen.getByText(/Нет данных/i)).toBeInTheDocument();
  });

  it('should be called onSort', () => {
    const handleSort = jest.fn();
    renderWithProvider(
      <BillingTable
        items={[]}
        sortBy={OrderedColumn.EXTERNAL_SOURCE}
        sortOrder={SortingOrder.ASC}
        onSort={handleSort}
      />
    );

    userEvent.click(screen.getByRole('columnheader', { name: 'Тип задания' }));

    expect(handleSort).toBeCalledTimes(1);
    expect(handleSort).toBeCalledWith(OrderedColumn.EXTERNAL_SOURCE, SortingOrder.DESC);
  });
});
