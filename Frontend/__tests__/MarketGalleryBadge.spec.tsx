import * as React from 'react';
import { shallow } from 'enzyme';

import { MarketGalleryBadge } from '@yandex-turbo/components/MarketGalleryBadge/MarketGalleryBadge';

describe('MarketGalleryBadge', () => {
    it('type: new', () => {
        const wrapper = shallow(<MarketGalleryBadge type="new" />);
        expect(wrapper.length).toEqual(1);
    });

    it('type: customer-choice', () => {
        const wrapper = shallow(<MarketGalleryBadge type="customer-choice" />);
        expect(wrapper.length).toEqual(1);
    });
});
