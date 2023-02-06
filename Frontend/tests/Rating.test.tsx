import * as React from 'react';
import { shallow } from 'enzyme';

import { Rating } from '../Rating';

describe('Rating', () => {
    it('рендерится хороший рейтинг', () => {
        const wrapper = shallow(<Rating value={3.98} />);
        expect(wrapper.text()).toBe('4,0');
    });

    it('рендерится нормальный рейтинг', () => {
        const wrapper = shallow(<Rating value={3.45} />);
        expect(wrapper.text()).toBe('3,5');
    });

    it('рендерится плохой рейтинг', () => {
        const wrapper = shallow(<Rating value={2.22} />);
        expect(wrapper.text()).toBe('2,2');
    });

    it('рендерится ужасный рейтинг', () => {
        const wrapper = shallow(<Rating value={0.95} />);
        expect(wrapper.text()).toBe('0,9');
    });

    it('не рендерится рейтинг выше 5', () => {
        const wrapper = shallow(<Rating value={5.1} />);
        expect(wrapper.isEmptyRender()).toBe(true);
    });

    it('не рендерится нулевой рейтинг', () => {
        const wrapper = shallow(<Rating value={0} />);
        expect(wrapper.isEmptyRender()).toBe(true);
    });
});
