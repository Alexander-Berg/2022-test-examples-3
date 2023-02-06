import React from 'react';
import { waitFor } from '@testing-library/react';

import { setupWithReatom } from 'src/test/withReatom';
import { ShopList } from './ShopList';
import { shopsListAtom } from 'src/store/shop';

const shop = { id: 1, name: 'myShop', businessId: 0, datacamp: false };
const atoms = { shopsListAtom };

describe('ShopList', () => {
  test('render', async () => {
    const { app, api } = setupWithReatom(<ShopList />, atoms);

    await waitFor(() => api.shopControllerV2.userShops.next().resolve([shop]));

    await waitFor(() => {
      app.getByText(shop.name);
      app.getByText(`${shop.id}`);
    });
  });
});
