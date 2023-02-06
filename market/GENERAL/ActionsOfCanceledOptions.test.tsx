import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import TableUtils from '../../Table.utils';
import { ActionsOfCanceledOptions } from './ActionsOfCanceledOptions';

const data = TableUtils.getNewRowData(0);
describe('<ActionsOfCanceledOptions />', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <ActionsOfCanceledOptions {...data} />
      </Provider>
    );
  });
});
