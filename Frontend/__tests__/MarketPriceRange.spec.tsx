import * as React from 'react';
import { shallow } from 'enzyme';

import { MarketPriceRange } from '@yandex-turbo/components/MarketPriceRange/MarketPriceRange';
import { CurrencyAvailable } from '@yandex-turbo/components/Cost/Currency/Currency';

const stubProps = {
    minPrice: 18000,
    maxPrice: 20000,
    offersCount: 100,
    currencyId: CurrencyAvailable.RUR,
};

describe('MarketPriceRange', () => {
    beforeEach(() => {
        // Имитируем первоначальное глобальное состояние,
        // которое обычно пушится из common.blocks/root/root.bemhtml.js
        window.Ya.getStore!().dispatch({
            type: '@SYS/UPDATE',
            payload: {
                global: {
                    deviceDetect: {
                        BrowserEngine: 'WebKit',
                        BrowserName: 'MobileSafari',
                        BrowserVersion: '0011',
                        BrowserVersionRaw: '11.0',
                        OSFamily: 'iOS',
                        OSName: '',
                        OSVersion: '11.0',
                    },
                },
            },
        });
    });

    it('должен рендерится без ошибок', () => {
        const wrapper = shallow(<MarketPriceRange {...stubProps} />);
        expect(wrapper.render().text()).toEqual('100 предложений от 18,000\u00A0–\u00A020,000\u00A0₽');
    });
});
