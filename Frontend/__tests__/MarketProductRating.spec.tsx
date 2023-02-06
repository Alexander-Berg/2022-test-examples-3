import * as React from 'react';
import { shallow } from 'enzyme';

import { MarketProductRating } from '@yandex-turbo/components/MarketProductRating/MarketProductRating';

describe('MarketProductRating', () => {
    it('должен рендерится без ошибок', () => {
        const wrapper = shallow(<MarketProductRating name="MyTestShop" rating={5} />);
        expect(wrapper.length).toEqual(1);
    });
});
