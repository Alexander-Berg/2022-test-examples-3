import * as React from 'react';
import { shallow } from 'enzyme';

import { MarketPrice } from '@yandex-turbo/components/MarketPrice/MarketPrice';
import { CurrencyAvailable } from '@yandex-turbo/components/Cost/Currency/Currency';

const stubProps = {
    newValue: 18000,
    oldValue: 20000,
    discount: 10,
    currencyId: CurrencyAvailable.RUR,
    size: 'm' as 'm',
};

describe('MarketPrice', () => {
    it('должен рендерится без ошибок', () => {
        const wrapper = shallow(<MarketPrice {...stubProps} />);
        expect(wrapper.length).toEqual(1);
    });

    it('должен рендерится без ошибок, если передана только цена', () => {
        const wrapper = shallow(<MarketPrice newValue={stubProps.newValue} />);
        expect(wrapper.length).toEqual(1);
    });
});
