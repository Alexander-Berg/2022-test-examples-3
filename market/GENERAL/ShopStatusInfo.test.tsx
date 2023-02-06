import React from 'react';
import userEvent from '@testing-library/user-event';
import { act } from '@testing-library/react';

import { UpdateStatus } from 'src/java/definitions';
import { ShopStatusInfo, PROCESSING_TEXT, getProcessingText } from './ShopStatusInfo';
import { setupWithReatom } from 'src/test/withReatom';

const shop = {
  businessId: 1,
  datacamp: false,
  id: 1,
  locked: false,
  name: 'test shop',
  updateTemporallyDisabled: false,
  updateStatus: UpdateStatus.ENABLED,
  updatedByFeed: true,
};

const shopUpdateStatus = {
  created: '',
  inQueue: true,
  retry: 1,
  shopId: shop.id,
  taskId: 1,
  updateCount: 100,
};

describe('<ShopStatusInfo />', () => {
  test('shop.updateStatus === UpdateStatus.ENABLING', () => {
    jest.useFakeTimers();
    const { app, api } = setupWithReatom(<ShopStatusInfo shop={{ ...shop, updateStatus: UpdateStatus.ENABLING }} />);
    app.getByText(PROCESSING_TEXT);
    jest.runOnlyPendingTimers();

    expect(api.shopControllerV2.getShop.activeRequests()).toHaveLength(1);
    expect(api.shopControllerV2.getShop.activeRequests()).toHaveLength(1);
    expect(api.shopControllerV2.getShopUpdateStatus.activeRequests()).toHaveLength(1);
    act(() => {
      api.shopControllerV2.getShopUpdateStatus.next().resolve(shopUpdateStatus);
    });

    app.getAllByText(getProcessingText(shop, shopUpdateStatus));

    jest.runOnlyPendingTimers();

    act(() => {
      api.shopControllerV2.getShopUpdateStatus.next().resolve({ ...shopUpdateStatus, updateCount: 3440 });
    });
    app.getAllByText('Обработано 3440 товаров', { exact: false });
  });

  test('shop.updateStatus === UpdateStatus.DISABLED', () => {
    const { app, api } = setupWithReatom(<ShopStatusInfo shop={{ ...shop, updateStatus: UpdateStatus.DISABLED }} />);
    // активируем обновления магазина
    userEvent.click(app.getByText('Включить обновления'));

    // резолв запроса активации
    expect(api.updateOffersController.updateOffersAsync.activeRequests()).toHaveLength(1);
    api.updateOffersController.updateOffersAsync.next().resolve();
    expect(api.updateOffersController.updateOffersAsync.activeRequests()).toHaveLength(0);

    // после успешного запроса на активацию магаза нужно обновить магазин
    expect(api.shopControllerV2.getShop.activeRequests()).toHaveLength(1);
  });
});
