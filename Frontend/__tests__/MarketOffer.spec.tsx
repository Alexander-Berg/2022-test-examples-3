import * as React from 'react';
import { shallow } from 'enzyme';

import { MarketOffer, IMarketOfferProps } from '@yandex-turbo/components/MarketOffer/MarketOffer';
import { CurrencyAvailable } from '@yandex-turbo/components/Cost/Currency/Currency';

const stubProps: IMarketOfferProps = {
    name: 'MyTestShop',
    rating: 4,
    reviews: 100,
    shopLink: '//m.market.yandex.ru',
    price: {
        newValue: 18000,
        oldValue: 20000,
        discount: 10,
        currencyId: CurrencyAvailable.RUR,
    },
    delivery: {
        rows: [
            {
                items: [
                    {
                        text: '+350 ₽ доставка, ',
                    },
                    {
                        text: 'сегодня',
                        isBenefit: true,
                    },
                ],
            },
            {
                items: [
                    {
                        text: 'Доступен самовывоз',
                    },
                ],
            },
        ],
    },
};

describe('MarketOffer', () => {
    it('должен рендерится без ошибок', () => {
        const wrapper = shallow(<MarketOffer {...stubProps} />);
        expect(wrapper.length).toEqual(1);
    });
});
