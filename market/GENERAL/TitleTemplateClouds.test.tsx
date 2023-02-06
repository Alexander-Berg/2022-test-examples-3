import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import { TemplateType } from 'src/java/definitions';
import { TitleTemplateClouds } from './TitleTemplateClouds';

describe('<TitleTemplateClouds />', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <TitleTemplateClouds titleKey={TemplateType.TITLE} />
      </Provider>
    );
  });
});
