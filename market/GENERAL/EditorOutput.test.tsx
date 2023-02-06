import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import { EditorOutput } from './EditorOutput';

describe('<EditorOutput />', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <EditorOutput onChange={() => null} />
      </Provider>
    );
  });
});
