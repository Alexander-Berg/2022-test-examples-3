import React from 'react';
import { render } from '@testing-library/react';

import { MappingsTable } from 'src/pages/ModelEditorCluster/components/MappingsTable/MappingsTable';
import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';

describe('MappingsTable::', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders with spin', () => {
    render(
      <Provider>
        <MappingsTable mappings={[]} isLoading selection={{}} setSelection={jest.fn()} />
      </Provider>
    );
  });

  it('renders with empty data', () => {
    render(
      <Provider>
        <MappingsTable mappings={[]} isLoading={false} selection={{}} setSelection={jest.fn()} />
      </Provider>
    );
  });
});
