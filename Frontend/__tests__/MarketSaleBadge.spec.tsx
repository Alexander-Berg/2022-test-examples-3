import * as React from 'react';
import { shallow } from 'enzyme';

import { MarketSaleBadge } from '@yandex-turbo/components/MarketSaleBadge/MarketSaleBadge';

const stubProps = {
    percent: 10,
};

describe('MarketSaleBadge', () => {
    it('должен рендерится без ошибок', () => {
        const wrapper = shallow(<MarketSaleBadge {...stubProps} />);
        expect(wrapper.length).toEqual(1);
    });

    it('должен содержать текст в формате "−N%"', () => {
        const wrapper = shallow(<MarketSaleBadge {...stubProps} />);
        expect(wrapper.text()).toEqual(`−${stubProps.percent}%`); // "&minus;" переводится в символ "−"
    });
});
