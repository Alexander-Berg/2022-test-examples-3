import * as React from 'react';
import { shallow } from 'enzyme';
import { MarketProductCarousel } from '../MarketProductCarousel';
import { defaultData } from './datastub';

describe('компонент MarketProductCarousel', () => {
    it('должен отрендериться без падения', () => {
        const wrapper = shallow(<MarketProductCarousel {...defaultData} />);
        expect(wrapper.length).toEqual(1);
    });
});
