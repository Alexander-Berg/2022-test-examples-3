import { cleanup, render } from '@testing-library/react';
import React from 'react';
import { UserRun } from 'src/rest/definitions';
import { setupTestProvider, TestProviderType } from 'src/test/setupProvider';

import { DataSettings } from './DataSettings';

const testUserRun = {
  fieldMappings: {
    name: 'test',
    description: 'testDesc',
  } as { [index: string]: string },
} as UserRun;

describe('<DataSettings />', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  afterEach(() => {
    cleanup();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <DataSettings userRun={testUserRun} />
      </Provider>
    );
  });
});
