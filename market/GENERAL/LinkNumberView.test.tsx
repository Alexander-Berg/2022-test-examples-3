import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import { LinkNumberView } from './LinkNumberView';
import TableUtils from '../../Table.utils';

const data = TableUtils.getNewRowData(0);
describe('<LinkNumberView />', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <LinkNumberView {...data} />
      </Provider>
    );
  });
});
