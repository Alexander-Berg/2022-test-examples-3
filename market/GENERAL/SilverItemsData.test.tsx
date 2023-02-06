import React from 'react';
import { act, render } from '@testing-library/react';
import { Route, Switch } from 'react-router-dom';

import { setupTestProvider } from 'test/setupApp';
import { SilverItemsData } from './SilverItemsData';

describe('<SilverItemsData />', () => {
  it('load and render', () => {
    const { api, Provider } = setupTestProvider('/mdm/handle-data/silver-items/234/testik');
    const app = render(
      <Provider>
        <Switch>
          <Route path="/mdm/handle-data/silver-items/:supplierId(\d+)/:shopSku" component={SilverItemsData} />
        </Switch>
      </Provider>
    );

    expect(api.silverController.silverItems.activeRequests()).toHaveLength(1);
    act(() => {
      api.silverController.silverItems.next().resolve([
        {
          supplier_id: undefined,
          source_id: undefined,
          source_type: undefined,
          shop_sku: undefined,
          fields: undefined,
          received_ts: undefined,
        },
        {
          supplier_id: '123',
          source_id: undefined,
          source_type: undefined,
          shop_sku: undefined,
          fields: {
            height_cm: { title: 'Поле высота', value: 1234 },
          },
          received_ts: undefined,
        },
      ]);
    });

    expect(app.getByText('123')).toBeInTheDocument();
    expect(app.getByText('Поле высота')).toBeInTheDocument();
    expect(app.getByText('1234')).toBeInTheDocument();
  });
});
