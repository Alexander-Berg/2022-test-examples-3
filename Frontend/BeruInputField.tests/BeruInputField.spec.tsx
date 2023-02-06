import * as React from 'react';
import { shallow } from 'enzyme';
import { BeruText } from '@yandex-turbo/components/BeruText/BeruText';
import { BeruInputField } from '../BeruInputField';

describe('BeruInputField', () => {
    it('должен корректно отрисовываться по умолчанию', () => {
        const wrapper = shallow(<BeruInputField name="custom-name" label="Заголовок" />);

        expect(wrapper.state()).toMatchObject({
            focused: false,
            value: '',
        });
        expect(wrapper.props()).toMatchObject({
            htmlFor: 'field-custom-name',
            className: 'beru-input-field',
        });
        expect(wrapper.find(BeruText).props()).toMatchObject({
            id: 'label-custom-name',
            children: 'Заголовок',
            size: '300',
        });
        expect(wrapper.find('button.beru-input-field__clear-button')).toHaveLength(0);
        expect(wrapper.find('.beru-input-field__input').props()).toMatchObject({
            id: 'field-custom-name',
            type: 'text',
            name: 'custom-name',
            autoFocus: false,
            autoComplete: 'off',
            value: '',
            'aria-labelledby': 'label-custom-name',
        });
    });

    it('должен корректно отрисовываться если передан размер', () => {
        const wrapper = shallow(<BeruInputField name="custom-name" label="Заголовок" styling={{ size: 'l' }} />);

        expect(wrapper.hasClass('beru-input-field_size_l')).toBe(true);
    });

    it('должен корректно отрисовываться если передано значение', () => {
        const wrapper = shallow(<BeruInputField name="custom-name" label="Заголовок" value="тест" />);

        expect(wrapper.state()).toMatchObject({ value: 'тест' });
        expect(wrapper.props()).toMatchObject({
            className: 'beru-input-field beru-input-field_filled beru-input-field_with-clear',
        });
        expect(wrapper.find('button.beru-input-field__clear-button')).toHaveLength(1);
        expect(wrapper.find('.beru-input-field__input').props()).toMatchObject({
            value: 'тест',
        });
    });

    it('поле должно быть в фокусе если autoFocus параметр выставлен', () => {
        const wrapper = shallow(<BeruInputField name="custom-name" label="Заголовок" autoFocus />);

        expect(wrapper.state()).toMatchObject({ focused: true });
        expect(wrapper.props()).toMatchObject({
            className: 'beru-input-field beru-input-field_focused',
        });
        expect(wrapper.find('button.beru-input-field__clear-button')).toHaveLength(0);
        expect(wrapper.find('.beru-input-field__input').props()).toMatchObject({ autoFocus: true });
    });

    it('поле должно очищатся при клике по кнопке и оставаться в фокусе', () => {
        const wrapper = shallow(<BeruInputField name="custom-name" label="Заголовок" value="тест" />);

        wrapper.find('button.beru-input-field__clear-button').simulate('click');
        expect(wrapper.state()).toMatchObject({
            value: '',
            focused: true,
        });
        expect(wrapper.find('.beru-input-field__input').props()).toMatchObject({ value: '' });
    });

    it('введенное в поле значение сохраняется в state и вызывается хэндлер onChange если передан.', () => {
        const persist = jest.fn();
        const onChange = jest.fn();
        const wrapper = shallow(<BeruInputField name="custom-name" label="Заголовок" onChange={onChange} />);
        const input = wrapper.find('.beru-input-field__input');

        input.simulate('change', { target: { value: 'Test' }, persist });

        expect(wrapper.state('value')).toEqual('Test');
        expect(persist).toHaveBeenCalled();
        expect(onChange).toHaveBeenCalledWith('Test');
    });

    it('на каждый ввод в поле вызывается хэндлер onKeyDown если передан', () => {
        const onKeyDown = jest.fn();
        const wrapper = shallow(<BeruInputField name="custom-name" label="Заголовок" onKeyDown={onKeyDown} />);
        const input = wrapper.find('.beru-input-field__input');

        input.simulate('keydown', { target: { value: 'T' } });
        input.simulate('keydown', { target: { value: 'Tе' } });

        expect(onKeyDown.mock.calls[0]).toEqual([{ target: { value: 'T' } }]);
        expect(onKeyDown.mock.calls[1]).toEqual([{ target: { value: 'Tе' } }]);
    });

    it('state компонента должен сбрасываться если входящее значение не равно уже записанному в state', () => {
        const wrapper = shallow(<BeruInputField name="custom-name" label="Заголовок" />);
        const input = wrapper.find('.beru-input-field__input');

        input.simulate('change', { target: { value: 'Test' }, persist: () => {} });
        wrapper.setProps({ value: 'new value' });

        expect(wrapper.state('value')).toEqual('new value');
    });

    it('state компонента не должен меняться если входящее значение равно null или undefined', () => {
        const wrapper = shallow(<BeruInputField name="custom-name" label="Заголовок" />);
        const input = wrapper.find('.beru-input-field__input');

        input.simulate('change', { target: { value: 'Test' }, persist: () => {} });
        wrapper.setProps({ value: undefined });

        expect(wrapper.state('value')).toEqual('Test');

        wrapper.setProps({ value: undefined });

        expect(wrapper.state('value')).toEqual('Test');
    });
});
