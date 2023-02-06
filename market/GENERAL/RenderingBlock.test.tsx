import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import { RenderingBlock } from './RenderingBlock';
import { RenderedBlock } from 'src/java/definitions';

describe('<OutputTemplate />', () => {
  let Provider: TestProviderType;

  const testData: RenderedBlock[] = [
    {
      items: [
        {
          key: 'test',
          value: 'test',
        },
      ],
      name: 'test',
    },
  ];
  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <RenderingBlock data={testData} />
      </Provider>
    );
  });
});
