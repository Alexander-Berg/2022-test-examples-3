import * as React from 'react';
import { shallow } from 'enzyme';

import { MarketSection } from '@yandex-turbo/components/MarketSection/MarketSection';

describe('MarketSection', () => {
    it('должен рендерится без ошибок', () => {
        const wrapper = shallow(<MarketSection>1</MarketSection>);
        expect(wrapper.length).toEqual(1);
    });
});
