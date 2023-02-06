import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import { RenderingTemplateEditor } from './RenderingTemplateEditor';

describe('<OutputTemplate />', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <RenderingTemplateEditor />
      </Provider>
    );
  });
});
