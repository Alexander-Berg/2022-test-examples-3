import * as React from 'react';
import { shallow } from 'enzyme';

import { MarketRatingDistribution } from '@yandex-turbo/components/MarketRatingDistribution/MarketRatingDistribution';
import grades from '../mock';

describe('MarketRatingDistribution', () => {
    it('должен рендерится без ошибок', () => {
        const wrapper = shallow(<MarketRatingDistribution grades={grades} />);
        expect(wrapper.length).toEqual(1);
    });

    it('не должен рендерится если нет оценок', () => {
        const wrapper = shallow(<MarketRatingDistribution />);
        expect(wrapper.children().length).toEqual(0);
    });

    it('должен прокидывать className', () => {
        const wrapper = shallow(<MarketRatingDistribution className="test" grades={[]} />);

        //className: 'test',
        expect(wrapper.hasClass('test')).toBe(true);
    });
});
