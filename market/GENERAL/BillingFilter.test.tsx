import React from 'react';
import { screen } from '@testing-library/react';

import { Grouping } from 'src/java/definitions';
import { renderWithProvider } from 'src/test/setupTestProvider';
import { BillingFilter } from './BillingFilter';

describe('<BillingFilter />', () => {
  it('renders without error', () => {
    renderWithProvider(
      <BillingFilter
        filter={{ dateFrom: new Date(), dateTo: new Date(), groupBy: Grouping.BY_BILLING_ACTION }}
        onChangeFilter={jest.fn()}
      />
    );

    expect(screen.getByText(/Применить/)).toBeInTheDocument();
  });
});
