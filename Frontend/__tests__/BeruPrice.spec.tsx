import * as React from 'react';
import { shallow } from 'enzyme';
import { BeruPrice, IProps, NOWRAP_SPACE } from '../BeruPrice';
import * as stubData from '../datastub';

const defaultData: IProps = {
    price: 110000,
    oldPrice: 119999,
};

const textPrices = {
    price: '110 000 ₽'.replace(/\s/g, NOWRAP_SPACE),
    oldPrice: '119 999 ₽'.replace(/\s/g, NOWRAP_SPACE),
};

describe('BeruPrice', () => {
    it('должен отрисовываться без ошибок', () => {
        const data = stubData.defaultPrice as IProps;
        const wrapper = shallow(<BeruPrice {...data} />);
        expect(wrapper.length).toEqual(1);
    });

    it('должен правильно отображать price', () => {
        const wrapper = shallow(<BeruPrice {...defaultData} />);
        expect(wrapper.render().find('.beru-price__current-price').text()).toEqual(String(textPrices.price));
    });

    it('должен правильно отображать oldPrice', () => {
        const wrapper = shallow(<BeruPrice {...defaultData} />);
        expect(wrapper.render().find('.beru-price__old-price').text()).toEqual(String(textPrices.oldPrice));
    });

    it('должен правильно отображать цену без скидки', () => {
        const data = stubData.defaultPrice as IProps;
        const wrapper = shallow(<BeruPrice {...data} />);
        expect(wrapper.render().hasClass('beru-price_with-old-price')).toEqual(false);
    });

    it('должен правильно отображать цену со скидкой', () => {
        const data = stubData.salePrice as IProps;
        const wrapper = shallow(<BeruPrice {...data} />);
        expect(wrapper.render().hasClass('beru-price_with-old-price')).toEqual(true);
    });

    it('должен правильно отображаться с theme="responsive"', () => {
        const data = stubData.themePrice as IProps;
        const wrapper = shallow(<BeruPrice {...data} />);

        expect(wrapper.hasClass('beru-price_theme_responsive')).toEqual(true);
        expect(wrapper.hasClass('beru-price_with-old-price')).toEqual(false);
        expect(wrapper.hasClass('beru-price_size_l')).toEqual(false);
    });
});
