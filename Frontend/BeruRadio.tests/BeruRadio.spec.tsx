import * as React from 'react';
import { shallow } from 'enzyme';
import { BeruRadio } from '../BeruRadio';

describe('BeruRadio', () => {
    it('По умолчанию рендерится корректно', () => {
        const wrapper = shallow(<BeruRadio name="test" value="1">Text</BeruRadio>);
        const input = wrapper.find('input');

        expect(input.props()).toMatchObject({
            type: 'radio',
            autoComplete: 'off',
            checked: false,
            disabled: false,
            name: 'test',
            value: '1',
        });
        expect(wrapper.find('.beru-radio__title').text()).toEqual('Text');
    });

    it('состояние "заблокирован", должно инициализироваться корректно', () => {
        const wrapper = shallow(<BeruRadio name="test" value="1" disabled>Text</BeruRadio>);
        const input = wrapper.find('input');

        expect(wrapper.hasClass('beru-radio_disabled')).toEqual(true);
        expect(input.prop('disabled')).toEqual(true);
    });

    it('состояние "выбран", должно рендерится корректно', () => {
        const wrapper = shallow(<BeruRadio name="test" value="1" checked>Text</BeruRadio>);
        const input = wrapper.find('input');

        expect(wrapper.hasClass('beru-radio_checked')).toEqual(true);
        expect(input.prop('checked')).toEqual(true);
    });
});
