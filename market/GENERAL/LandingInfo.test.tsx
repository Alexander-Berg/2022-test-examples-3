import React from 'react';
import { render } from '@testing-library/react';

import { LandingDto } from 'src/java/definitions';
import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import { LandingInfo } from './LandingInfo';

describe('<LandingInfo />', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <LandingInfo landing={{} as LandingDto} />
      </Provider>
    );
  });
});
