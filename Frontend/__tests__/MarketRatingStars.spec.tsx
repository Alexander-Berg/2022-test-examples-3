import * as React from 'react';
import { shallow } from 'enzyme';

import { MarketRatingStars } from '@yandex-turbo/components/MarketRatingStars/MarketRatingStars';

describe('MarketRatingStars', () => {
    it('должен рендерится без ошибок', () => {
        const wrapper = shallow(<MarketRatingStars rating={5} />);
        expect(wrapper.length).toEqual(1);
    });
});
