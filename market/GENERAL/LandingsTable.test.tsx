import React from 'react';
import { render } from '@testing-library/react';

import { LandingTable } from './LandingsTable';
import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';

describe('<LandingTable />', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <LandingTable parameterId={0} />
      </Provider>
    );
  });
});
