import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import { TemplateType } from 'src/java/definitions';
import { TitleTemplateEditor } from './TitleTemplateEditor';

describe('<TitleTemplateEditor />', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <TitleTemplateEditor titleKey={TemplateType.TITLE} hid={0} />
      </Provider>
    );
  });
});
