import * as React from 'react';
import { shallow } from 'enzyme';
import { CheckboxGroup } from '../CheckboxGroup';

describe('CheckboxGroup', () => {
    it('должен вернуть стандартное количество видимых элементов и кнопку разворачивания', () => {
        const wrapper = shallow(
            <CheckboxGroup
                name="name"
                items={[
                    { value: 'value1', text: 'text1' },
                    { value: 'value2', text: 'text1' },
                    { value: 'value3', text: 'text1' },
                    { value: 'value4', text: 'text1' },
                    { value: 'value5', text: 'text1' },
                ]}
            />
        );

        expect(wrapper.find('.turbo-checkbox-group__item')).toHaveLength(4);
        expect(wrapper.find('.turbo-checkbox-group__item_showall')).toHaveLength(1);
        expect(wrapper.find('.turbo-checkbox-group__item_showall').render().text()).toEqual('Показать все5');
    });

    it('должен вернуть заданное количество видимых элементов и кнопку разворачивания', () => {
        const wrapper = shallow(
            <CheckboxGroup
                name="name"
                cutLimit={2}
                items={[
                    { value: 'value1', text: 'text1' },
                    { value: 'value2', text: 'text1' },
                    { value: 'value3', text: 'text1' },
                    { value: 'value4', text: 'text1' },
                    { value: 'value5', text: 'text1' },
                ]}
            />
        );
        expect(wrapper.find('.turbo-checkbox-group__item')).toHaveLength(3);
        expect(wrapper.find('.turbo-checkbox-group__item_showall')).toHaveLength(1);
        expect(wrapper.find('.turbo-checkbox-group__item_showall').render().text()).toEqual('Показать все5');
    });

    it('должен разворачивать все элементы и скрывать кнопку разворачивания', () => {
        const wrapper = shallow(
            <CheckboxGroup
                name="name"
                expanded
                items={[
                    { value: 'value1', text: 'text1' },
                    { value: 'value2', text: 'text1' },
                    { value: 'value3', text: 'text1' },
                    { value: 'value4', text: 'text1' },
                    { value: 'value5', text: 'text1' },
                ]}
            />
        );
        expect(wrapper.find('.turbo-checkbox-group__item')).toHaveLength(5);
        expect(wrapper.find('.turbo-checkbox-group__item_showall').exists()).toEqual(false);
    });

    it('должен вызывать обработчик при нажатии на кнопку разворачивания', () => {
        const onExpand = jest.fn();

        const wrapper = shallow(
            <CheckboxGroup
                name="name"
                onExpand={onExpand}
                items={[
                    { value: 'value1', text: 'text1' },
                    { value: 'value2', text: 'text1' },
                    { value: 'value3', text: 'text1' },
                    { value: 'value4', text: 'text1' },
                    { value: 'value5', text: 'text1' },
                ]}
            />
        );
        wrapper.find('.turbo-checkbox-group__item_showall').simulate('click');
        expect(onExpand).toHaveBeenCalled();
    });
});
