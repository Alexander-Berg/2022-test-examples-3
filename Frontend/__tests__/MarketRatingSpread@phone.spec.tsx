import * as React from 'react';
import { shallow } from 'enzyme';

import { MarketRatingSpread } from '@yandex-turbo/components/MarketRatingSpread/MarketRatingSpread';
import data from '../mock';

describe('MarketRatingSpread', () => {
    it('должен рендерится без ошибок', () => {
        const wrapper = shallow(<MarketRatingSpread {...data} />);
        expect(wrapper.length).toEqual(1);
    });
});
