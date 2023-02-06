import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import { OutputTemplateEditor } from './OutputTemplateEditor';
import { OutputTemplate, OutputTemplateType } from 'src/java/definitions';

describe('<OutputTemplate />', () => {
  let Provider: TestProviderType;

  const testData: OutputTemplate = {
    categoryId: 0,
    comment: 'string',
    content: 'string',
    dirty: false,
    draft: 'string',
    generatorMessage: 'string',
    id: 0,
    name: 'string',
    type: OutputTemplateType.SEO,
  };
  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <OutputTemplateEditor data={testData} />
      </Provider>
    );
  });
});
