import React from 'react';
import userEvent from '@testing-library/user-event';
import { act, waitFor } from '@testing-library/react';

import { setupWithReatom } from 'src/test/withReatom';
import { setShopListAction, shopsListAtom } from 'src/store/shop';
import { ShopForm, DUPLICATE_SHOP_ERROR } from './ShopForm';
import { ShopView } from 'src/java/definitions';

const shop = { id: 1, name: 'myShop', businessId: 0, datacamp: false, updatedByFeed: false } as ShopView;
const atoms = { shopsListAtom };
const dispatches = [setShopListAction([shop])];

describe('ShopForm', () => {
  test('add shop', async () => {
    const { app, api } = setupWithReatom(<ShopForm />, atoms, dispatches);

    const numberInputs = app.getAllByRole('spinbutton');
    // ид
    userEvent.type(numberInputs[0], '777');
    // business id
    userEvent.type(numberInputs[1], '888');
    // name
    const stringInputs = app.getAllByRole('textbox');
    userEvent.type(stringInputs[0], 'myShop2');
    // is eox
    userEvent.click(app.getByLabelText('EOX'));
    userEvent.click(app.getByLabelText('Обновляется фидом'));

    userEvent.click(app.getByText(/Добавить/i));

    expect(api.shopControllerV2.insert).toHaveBeenCalledWith(777, {
      businessId: 888,
      datacamp: true,
      name: 'myShop2',
      updatedByFeed: true,
    });

    act(() => {
      api.shopControllerV2.insert.next().resolve(shop);
    });

    await waitFor(() => expect(api.shopControllerV2.insert.activeRequests()).toHaveLength(0));

    // проверяем что после добавления магазина обновляется весь список магазов
    expect(api.shopControllerV2.userShops.activeRequests()).toHaveLength(1);
  });

  test('invalid shop', async () => {
    const { app, api } = setupWithReatom(<ShopForm />, atoms, dispatches);

    const userIdInput = app.getAllByRole('spinbutton');
    userEvent.type(userIdInput[0], '1');
    const stringInputs = app.getAllByRole('textbox');
    userEvent.type(stringInputs[0], 'myShop');

    app.getByText(DUPLICATE_SHOP_ERROR);

    userEvent.click(app.getByText(/Добавить/i));
    expect(api.userController.activeRequests().length).toBe(0);
  });
});
