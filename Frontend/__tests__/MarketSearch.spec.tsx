import * as React from 'react';
import { mount, shallow } from 'enzyme';
import { MarketSearch } from '../MarketSearch';

const componentProps = { clid: '927' };
describe('MarketSearch', () => {
    it('должен отрисовываться без ошибок', () => {
        const wrapper = shallow(<MarketSearch {...componentProps} />);

        expect(wrapper.length).toEqual(1);
    });

    it('должен отображать кнопку "clear" когда поле не пусто и очищать поле по ее нажатию', () => {
        const wrapper = mount(<MarketSearch {...componentProps} />);
        const input = wrapper.find('.market-search__input-control');

        input.simulate('input', { target: { value: 'phone' } });
        expect(wrapper.find('.market-search__input-clear').hasClass('market-search__input-clear_show')).toEqual(true);

        wrapper.find('.market-search__input-clear').simulate('click');
        expect(wrapper.find('.market-search__input-clear').hasClass('market-search__input-clear_show')).toEqual(false);
    });

    it('должен скрывать кнопку "clear" поле пусто', () => {
        const wrapper = mount(<MarketSearch {...componentProps} />);
        const input = wrapper.find('.market-search__input-control');

        input.simulate('input', { target: { value: 'phone' } });
        expect(wrapper.find('.market-search__input-clear').hasClass('market-search__input-clear_show')).toEqual(true);

        input.simulate('input', { target: { value: '' } });
        expect(wrapper.find('.market-search__input-clear').hasClass('market-search__input-clear_show')).toEqual(false);
    });

    it('должен отображаться правильный placeholder', () => {
        const wrapper = shallow(<MarketSearch {...componentProps} />);

        expect(wrapper.find('.market-search__input-control').props().placeholder).toEqual('Искать товары');
    });

    it('должны быть выставлены правильные submit опции', () => {
        const wrapper = shallow(<MarketSearch {...componentProps} />);
        const props = wrapper.props();

        expect(props.method).toEqual('GET');
        expect(props.action).toEqual('https://m.market.yandex.ru/search');
        expect(props.target).toEqual('_blank');
    });

    it('должен содержать скрытые поля с правильным именем и значением', () => {
        const wrapper = shallow(<MarketSearch {...componentProps} />);
        const input = wrapper.find('input[type="hidden"]');

        expect(input.length).toEqual(2);
        expect(input.at(0).props()).toMatchObject({
            name: 'cvredirect',
            value: '1',
        });
        expect(input.at(1).props()).toMatchObject({
            name: 'clid',
            value: '927',
        });
    });
});
