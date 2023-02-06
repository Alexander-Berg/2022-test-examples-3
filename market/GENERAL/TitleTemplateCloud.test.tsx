import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import { Block } from '../../TitleMaker.types';
import { TemplateType } from 'src/java/definitions';
import { TitleTemplateCloud } from './TitleTemplateCloud';

describe('<TitleTemplateCloud />', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <TitleTemplateCloud cloud={{} as Block} index={0} indexCloud={[0]} titleKey={TemplateType.TITLE} />
      </Provider>
    );
  });
});
