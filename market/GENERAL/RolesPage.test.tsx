import { createStore } from '@reatom/core';
import { context as ReatomContext } from '@reatom/react';
import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import { RolesPage } from './RolesPage';

describe('RolesPage::', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <ReatomContext.Provider value={createStore()}>
          <RolesPage />
        </ReatomContext.Provider>
      </Provider>
    );
  });
});
