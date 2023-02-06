import * as React from 'react';
import { shallow, render } from 'enzyme';

import { MarketButton } from '@yandex-turbo/components/MarketButton/MarketButton';

const caption = 'caption';

describe('MarketButton', () => {
    it('должен рендерится без ошибок', () => {
        const wrapper = shallow(<MarketButton>{caption}</MarketButton>);
        expect(wrapper.length).toEqual(1);
    });

    it('должен отображать текст, переданный в children', () => {
        const wrapper = shallow(<MarketButton>{caption}</MarketButton>);
        expect(wrapper.find('.market-button').render().text()).toEqual(caption);
    });

    it('должен отображать html, переданный в children', () => {
        const className = 'test-class';
        const selector = `.${className}`;
        const wrapper = shallow(<MarketButton><span className={className}>{caption}</span></MarketButton>);
        expect(wrapper.find('.market-button').find(selector).length).toEqual(1);
        expect(wrapper.find('.market-button').find(selector).render().text()).toEqual(caption);
    });

    it('если передан url, должен отображать ссылку (тег <a>) с этим url', () => {
        const url = '//yandex.ru/';
        const marketButton = render(<MarketButton url={url}>{caption}</MarketButton>);
        expect(marketButton.is('a')).toEqual(true);
        expect(marketButton.prop('href')).toEqual(url);
    });

    it('если параметр url не передан, должен отображать кнопку (тег <button>)', () => {
        const wrapper = shallow(<MarketButton>{caption}</MarketButton>);
        expect(wrapper.find('button').length).toEqual(1);
    });
});
