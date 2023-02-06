import React from 'react';
import { render } from '@testing-library/react';

import { ProductTreeTab } from 'src/pages/ProductTree/ProductTree.types';
import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import { TabBar } from 'src/pages/ProductTree/components/TabBar/TabBar';

const tabItems = [
  {
    title: 'test',
    content: <div />,
    slug: ProductTreeTab.PARAMETERS,
  },
];

describe('<TabBar />', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <TabBar tabItems={tabItems} />
      </Provider>
    );
  });
});
