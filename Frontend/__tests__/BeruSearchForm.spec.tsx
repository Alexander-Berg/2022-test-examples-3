import * as React from 'react';
import { mount, shallow } from 'enzyme';
import { BeruSearchForm } from '../BeruSearchForm';

describe('BeruSearchForm', () => {
    const searchUrl = 'https://path/to/search';

    it('должен отрисовываться без ошибок', () => {
        const wrapper = shallow(<BeruSearchForm searchUrl={searchUrl} />);

        expect(wrapper.length).toEqual(1);
    });

    it('должке отображать кнопку "clear" когда поле не пусто и очищать поле по ее нажатию', () => {
        const wrapper = mount(<BeruSearchForm searchUrl={searchUrl} />);
        const input = wrapper.find('.beru-search-form__input-control');

        input.simulate('input', { target: { value: 'phone' } });
        expect(wrapper.find('.beru-search-form__input-clear').hasClass('beru-search-form__input-clear_show')).toEqual(true);

        wrapper.find('.beru-search-form__input-clear').simulate('click');
        expect(wrapper.find('.beru-search-form__input-clear').hasClass('beru-search-form__input-clear_show')).toEqual(false);
    });

    it('должен скрывать кнопку "clear" поле пусто', () => {
        const wrapper = mount(<BeruSearchForm searchUrl={searchUrl} />);
        const input = wrapper.find('.beru-search-form__input-control');

        input.simulate('input', { target: { value: 'phone' } });
        expect(wrapper.find('.beru-search-form__input-clear').hasClass('beru-search-form__input-clear_show')).toEqual(true);

        input.simulate('input', { target: { value: '' } });
        expect(wrapper.find('.beru-search-form__input-clear').hasClass('beru-search-form__input-clear_show')).toEqual(false);
    });

    it('должен отображаться правильный placeholder', () => {
        const wrapper = shallow(<BeruSearchForm searchUrl={searchUrl} />);

        expect(wrapper.find('.beru-search-form__input-control').props().placeholder).toEqual('Искать товары');
    });

    it('должны быть выставлены правильные submit опции', () => {
        const wrapper = shallow(<BeruSearchForm searchUrl={searchUrl} />);
        const props = wrapper.props();

        expect(props.method).toEqual('GET');
        expect(props.action).toEqual(searchUrl);
        expect(props.target).toEqual('_blank');
    });

    it('должен содержать скрытое поле с правильным именем и значением', () => {
        const wrapper = shallow(<BeruSearchForm searchUrl={searchUrl} />);
        const input = wrapper.find('input[type="hidden"]');

        expect(input.length).toEqual(1);
        expect(input.at(0).props()).toMatchObject({
            name: 'cvredirect',
            value: '1',
        });
    });
});
