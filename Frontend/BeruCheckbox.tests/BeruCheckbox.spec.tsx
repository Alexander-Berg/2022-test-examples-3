import * as React from 'react';
import { shallow } from 'enzyme';
import { BeruCheckbox } from '../BeruCheckbox';
import { BeruCheckboxIndicator } from '../Indicator/BeruCheckboxIndicator';

describe('BeruCheckbox', () => {
    it('по умолчанию отрисовывается без ошибок', () => {
        const wrapper = shallow(<BeruCheckbox name="foo" value="bar">Текст</BeruCheckbox>);

        expect(wrapper.hasClass('beru-checkbox_type_default')).toEqual(true);
        expect(wrapper.find('input').props()).toMatchObject({
            checked: false,
            disabled: false,
            name: 'foo',
            value: 'bar',
            type: 'checkbox',
            autoComplete: 'off',
        });
        expect(wrapper.find(BeruCheckboxIndicator).props()).toMatchObject({
            color: undefined,
        });
        expect(wrapper.find('.beru-checkbox__title').text()).toEqual('Текст');
    });

    it('проставляется модификатор disabled', () => {
        const wrapper = shallow(<BeruCheckbox name="foo" value="bar" disabled>Текст</BeruCheckbox>);

        expect(wrapper.hasClass('beru-checkbox_disabled'));
        expect(wrapper.find('input').props()).toMatchObject({ disabled: true });
    });

    it('проставляется модификатор checked', () => {
        const wrapper = shallow(<BeruCheckbox name="foo" value="bar" checked>Текст</BeruCheckbox>);

        expect(wrapper.hasClass('beru-checkbox_checked'));
        expect(wrapper.find('input').props()).toMatchObject({ checked: true });
    });

    it('рендерится как контрол выбора цвета', () => {
        const wrapper = shallow(<BeruCheckbox name="foo" value="bar" color="#ffffff">Текст</BeruCheckbox>);

        expect(wrapper.hasClass('beru-checkbox_type_color'));
        expect(wrapper.find(BeruCheckboxIndicator).props()).toMatchObject({
            color: '#ffffff',
        });
    });
});
