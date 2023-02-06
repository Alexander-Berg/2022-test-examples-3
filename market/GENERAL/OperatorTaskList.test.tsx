import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import { OperatorTaskList } from './OperatorTaskList';

describe('OperatorTaskList::', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <OperatorTaskList />
      </Provider>
    );
  });
});
