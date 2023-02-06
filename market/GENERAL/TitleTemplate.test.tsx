import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import { TitleTemplate } from './TitleTemplate';
import { JavaScriptTemplate } from '../../TitleMaker.types';
import { TemplateType } from 'src/java/definitions';

describe('<TitleTemplate />', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <TitleTemplate hid={0} data={{} as JavaScriptTemplate} titleKey={TemplateType.TITLE_WITHOUT_VENDOR} title="" />
      </Provider>
    );
  });
});
