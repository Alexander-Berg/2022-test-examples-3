import * as React from 'react';
import { shallow } from 'enzyme';

import { MarketProductTitle } from '@yandex-turbo/components/MarketProductTitle/MarketProductTitle';

const title = 'Соковыжималка Phillips HR1922 Avance Collection';

describe('MarketProductTitle', () => {
    it('должен рендерится без ошибок', () => {
        const wrapper = shallow(<MarketProductTitle>{title}</MarketProductTitle>);
        expect(wrapper.length).toEqual(1);
    });

    it('должен отображать текст, переданный в children', () => {
        const wrapper = shallow(<MarketProductTitle>{title}</MarketProductTitle>);
        expect(wrapper.find('.market-product-title').render().text()).toEqual(title);
    });

    it('должен отображать html, переданный в children', () => {
        const className = 'test-class';
        const selector = `.${className}`;
        const wrapper = shallow(<MarketProductTitle><span className={className}>{title}</span></MarketProductTitle>);
        expect(wrapper.find('.market-product-title').find(selector).length).toEqual(1);
        expect(wrapper.find('.market-product-title').find(selector).render().text()).toEqual(title);
    });
});
