import React from 'react';
import { render } from '@testing-library/react';

import { SizeDto } from 'src/java/definitions';
import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import { SizeTableRow } from './SizeTableRow';

describe('<SizeTableRow />', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <SizeTableRow size={{ size_name: 'test', category_ids: [123] } as SizeDto} measures={[]} />
      </Provider>
    );
  });
});
