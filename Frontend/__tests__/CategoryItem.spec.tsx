import * as React from 'react';
import { mount } from 'enzyme';
import { CategoryItemTypeLink } from '../_Type/CategoryItem_Type_Link';
import { CategoryItemTypeButton } from '../_Type/CategoryItem_Type_Button';

describe('CategoryItem', () => {
    describe('_type_link', () => {
        it('должен прокидывать target и className', () => {
            const wrapper = mount(
                <CategoryItemTypeLink
                    url="/turbo?text=1"
                    count={188}
                    target="_self"
                    className="my-class"
                >
                    OLED-телевизоры
                </CategoryItemTypeLink>
            );
            expect(wrapper.find('li').prop('className')).toContain('my-class');
            expect(wrapper.find('Link').last().prop('target')).toEqual('_self');
        });
    });

    describe('_type_button', () => {
        it('должен прокидывать onClick и className', () => {
            const onClick = jest.fn();
            const wrapper = mount(
                <CategoryItemTypeButton
                    count={188}
                    onClick={onClick}
                    className="my-class"
                >
                    OLED-телевизоры
                </CategoryItemTypeButton>
            );
            expect(wrapper.find('li').prop('className')).toContain('my-class');
            wrapper.find('Button').simulate('click');
            expect(onClick).toHaveBeenCalledTimes(1);
        });
    });
});
