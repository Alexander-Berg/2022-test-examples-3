import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import { OrderActionCellRenderer } from './OrderActionCellRenderer';

describe('<OrderActionCellRenderer/>', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <OrderActionCellRenderer />
      </Provider>
    );
  });

  it('renders complete value', () => {
    render(
      <Provider>
        <OrderActionCellRenderer value={{ isHidden: true, onHide: () => null, onShow: () => null, paramId: 0 }} />
      </Provider>
    );
  });
});
