import { cleanup, render, screen } from '@testing-library/react/pure';
import React, { FC } from 'react';
import { Grid } from '@crm/components/dist/Attribute2/components/Grid';
import { defaultValue } from './mocks/testData';
import { StatefulOpportunities } from './StatefulOpportunities';

export const OpportunitiesWrapper: FC = () => {
  return (
    <Grid>
      <StatefulOpportunities
        label="Сделки"
        defaultValue={defaultValue.opportunities}
        account={defaultValue.account}
      />
    </Grid>
  );
};

describe('Attribute2/Opportunities', () => {
  afterEach(() => {
    cleanup();
    jest.clearAllMocks();
  });

  it('renders tags', () => {
    render(<OpportunitiesWrapper />);

    expect(screen.getByText('Сделка 1')).toBeVisible();
    expect(screen.getByText('Сделка 2')).toBeVisible();
  });
});
