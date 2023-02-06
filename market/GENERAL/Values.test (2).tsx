import React from 'react';
import { render } from '@testing-library/react';

import { CategoryParameterDetailsDto } from 'src/java/definitions';
import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import { Values } from './Values';

describe('<Values />', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <Values parameter={{} as CategoryParameterDetailsDto} />
      </Provider>
    );
  });
});
