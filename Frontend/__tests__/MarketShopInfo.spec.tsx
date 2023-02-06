import * as React from 'react';
import { shallow } from 'enzyme';

import { MarketShopInfo } from '@yandex-turbo/components/MarketShopInfo/MarketShopInfo';

describe('MarketShopInfo', () => {
    it('должен рендерится без ошибок', () => {
        const wrapper = shallow(<MarketShopInfo name="MyTestShop" rating={5} />);
        expect(wrapper.length).toEqual(1);
    });
});
