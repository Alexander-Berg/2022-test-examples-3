import * as React from 'react';
import { mount } from 'enzyme';
import Select, { Option, ListItem } from './index';

const label = 'Select control';
const onChangeMock = jest.fn();

describe('Select', () => {
    afterEach(() => {
        onChangeMock.mockReset();
    });

    it('should open or close popup on the label click', () => {
        const wrapper = mount<Select>(
            <Select onChange={onChangeMock} label={label}>
                <Option text="1" value={1} selected>
                    1
                </Option>
                <Option text="2" value={2}>
                    2
                </Option>
            </Select>,
        );

        const isPopupVisible = () => {
            return wrapper.find('.select__popup').hasClass('select__popup_visible');
        };

        wrapper.find('.select__label').simulate('click');

        expect(isPopupVisible()).toEqual(true);

        wrapper.find('.select__label').simulate('click');

        expect(isPopupVisible()).toEqual(false);
    });

    it('should trim entered text', () => {
        const wrapper = mount<Select>(
            <Select onChange={onChangeMock} label={label}>
                <Option text="1" value={1}>
                    1
                </Option>
            </Select>,
        );

        const expectedText = 'lorem impsum';

        wrapper.find('.select__input').simulate('change', {
            target: {
                value: `    ${expectedText}    `,
            },
        });

        expect(wrapper.find('.select__input').prop('value')).toEqual(expectedText);
    });

    it('should close popup on the list item click', () => {
        const wrapper = mount<Select>(
            <Select onChange={onChangeMock} label={label}>
                <Option text="1" value={1}>
                    1
                </Option>
            </Select>,
        );

        wrapper.find('.select__label').simulate('click');

        wrapper.find(ListItem).first().simulate('click');

        expect(wrapper.find('.select__popup').hasClass('select__popup_visible')).toEqual(false);
        expect(onChangeMock).toBeCalledWith(1);
    });
});
