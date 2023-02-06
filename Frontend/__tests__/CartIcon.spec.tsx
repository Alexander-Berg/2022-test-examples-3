import * as React from 'react';
import { shallow, mount } from 'enzyme';
import { CartIconComponent } from '../CartIcon';

describe('CartIconComponent', () => {
    it('Должен использоваться Link', () => {
        expect(
            mount(<CartIconComponent cartUrl="http://test" />)
                .find('a.link.turbo-cart-icon')
                .length
        ).toBe(1);
    });

    it('Должен использоваться div', () => {
        const wrapper = mount(<CartIconComponent />);

        expect(wrapper.find('.link.turbo-cart-icon').length).toBe(0);
        expect(wrapper.find('div.turbo-cart-icon').length).toBe(1);
    });

    it('Не должен показывать чило товаров', () => {
        expect(
            shallow(<CartIconComponent />)
                .find('.turbo-cart-icon__count')
                .length
        ).toBe(0);
        expect(
            shallow(<CartIconComponent count={0} />)
                .find('.turbo-cart-icon__count')
                .length
        ).toBe(0);
    });

    it('Должен показывать чило товаров', () => {
        const wrapperCount = shallow(<CartIconComponent count={42} />)
            .find('.turbo-cart-icon__count');

        expect(wrapperCount.length).toBe(1);
        expect(wrapperCount.text()).toBe('42');
    });
});
