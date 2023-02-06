import React from 'react';
import { render } from '@testing-library/react';

import { ActionsOfCanceledEarlierOptions } from './ActionsOfCanceledEarlierOptions';
import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import TableUtils from '../../Table.utils';

const data = TableUtils.getNewRowData(0);
describe('<ActionsOfCanceledEarlierOptions />', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <ActionsOfCanceledEarlierOptions {...data} />
      </Provider>
    );
  });
});
