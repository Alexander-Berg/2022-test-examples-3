import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import { SizeMeasurePage } from '.';

describe('<SizeMeasurePage />', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <SizeMeasurePage />
      </Provider>
    );
  });
});
