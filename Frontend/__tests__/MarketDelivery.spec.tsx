import * as React from 'react';
import { shallow } from 'enzyme';

import { MarketDelivery } from '@yandex-turbo/components/MarketDelivery/MarketDelivery';
import {
    withDayBenefit,
} from './mocks';

describe('MarketDelivery', () => {
    it('должен рендерится без ошибок', () => {
        const wrapper = shallow(<MarketDelivery {...withDayBenefit} />);
        expect(wrapper.length).toEqual(1);
    });
});
