import React from 'react';

import { ShopHeader } from './ShopHeader';
import { getShopInfoMock } from 'src/test/mockData/shops';
import { render } from '@testing-library/react';
import { setupTestProvider } from 'src/test/utils';

const campaignId = 21995831;
const expectPartnerLinks = 'https://partner.market.fslb.yandex.ru/shop/21995831/assortment/offer-card?offerId=867';

describe('<ShopHeader />', () => {
  test('show info', () => {
    const shopInfo = getShopInfoMock({ partnerLink: expectPartnerLinks });
    const { Provider } = setupTestProvider();

    const app = render(
      <Provider
        initialLocation={{
          pathname: '/offer-diagnostic',
          search: '?businessId=723377&mainTab=SHOPS&offerId=867',
        }}
      >
        <ShopHeader data={shopInfo} />
      </Provider>
    );

    app.getByText(shopInfo.shopName, { exact: false });

    // чекаем что отображается ид компании и правильно генерится ссылка на ПИ
    app.getByText(campaignId);
    const link = app.getByTitle('Ссылка на партнерку');
    expect(link).toHaveAttribute('href', expectPartnerLinks);
  });
});
