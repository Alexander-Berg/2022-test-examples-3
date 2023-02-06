import * as React from 'react';
import { shallow } from 'enzyme';
import { BeruPrice, NOWRAP_SPACE } from '@yandex-turbo/components/BeruPrice/BeruPrice';
import { BeruPriceInterval } from '../BeruPriceInterval';

const data = {
    minPrice: 2000,
    maxPrice: 3000,
};

const expectedData = {
    minPrice: '2 000 ₽'.replace(/\s/g, NOWRAP_SPACE),
    maxPrice: '3 000 ₽'.replace(/\s/g, NOWRAP_SPACE),
};

describe('BeruPrice', () => {
    it('должен отрисовываться без ошибок', () => {
        const wrapper = shallow(<BeruPriceInterval {...data} />);
        expect(wrapper.length).toEqual(1);
    });

    it('должен выводить "бесплатно"', () => {
        const wrapper = shallow(<BeruPriceInterval minPrice={0} maxPrice={0} />);

        expect(wrapper.render().find('.beru-price-interval__free-price').text()).toEqual('бесплатно');
    });

    it('должен выводить "{minPrice}"', () => {
        const wrapper = shallow(<BeruPriceInterval minPrice={data.minPrice} maxPrice={data.minPrice} />);
        const beruPrice = wrapper.find(BeruPrice);

        expect(wrapper.render().text()).toEqual(expectedData.minPrice);
        expect(beruPrice.props()).toMatchObject({ price: data.minPrice, theme: 'responsive' });
    });

    it('должен выводить "от {minPrice}"', () => {
        const wrapper = shallow(<BeruPriceInterval minPrice={data.minPrice} maxPrice={0} />);
        const beruPrice = wrapper.find(BeruPrice);

        expect(wrapper.render().text()).toEqual(`от${NOWRAP_SPACE}${expectedData.minPrice}`);
        expect(beruPrice.props()).toMatchObject({ price: data.minPrice, theme: 'responsive' });
    });

    it('должен выводить "до {maxPrice}"', () => {
        const wrapper = shallow(<BeruPriceInterval minPrice={0} maxPrice={data.maxPrice} />);
        const beruPrice = wrapper.find(BeruPrice);

        expect(wrapper.render().text()).toEqual(`${NOWRAP_SPACE}до${NOWRAP_SPACE}${expectedData.maxPrice}`);
        expect(beruPrice.props()).toMatchObject({ price: data.maxPrice, theme: 'responsive' });
    });

    it('должен выводить "от {minPrice} до {maxPrice}"', () => {
        const wrapper = shallow(<BeruPriceInterval minPrice={data.minPrice} maxPrice={data.maxPrice} />);
        const beruPrice = wrapper.find(BeruPrice);

        expect(wrapper.render().text())
            .toEqual(`от${NOWRAP_SPACE}${expectedData.minPrice}${NOWRAP_SPACE}до${NOWRAP_SPACE}${expectedData.maxPrice}`);
        expect(beruPrice).toHaveLength(2);
        expect(beruPrice.at(0).props()).toMatchObject({ price: data.minPrice, theme: 'responsive' });
        expect(beruPrice.at(1).props()).toMatchObject({ price: data.maxPrice, theme: 'responsive' });
    });
});
