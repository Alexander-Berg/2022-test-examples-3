import React from 'react';
import { render } from '@testing-library/react';

import { SizeDto } from 'src/java/definitions';
import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import { SizeEditor } from './SizeEditor';

describe('<SizeEditor />', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <SizeEditor data={{} as SizeDto} onChange={jest.fn()} onCancel={jest.fn()} />
      </Provider>
    );
  });
});
