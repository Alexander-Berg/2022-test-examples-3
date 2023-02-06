import React from 'react';
import {shallow} from 'enzyme';

import PhoneView from '../View';

declare const test: jest.It;

describe('PhoneView', () => {
    const wrapper = shallow(<PhoneView value="" />);

    test.each(['', {main: ''}])("empty input '%s' should not be rendered", input => {
        wrapper.setProps({value: input});
        expect(wrapper.render().text()).toEqual('');
    });

    const nbsp = '\u00A0';

    test.each([
        ' ',
        '123',
        'fooBar',
        'fsssa',
        '1233',
        '79122470269',
        '-79122470269',
        '*79122470269',
        '7 (912) 247-02-69',
        '++79122470269',
        '+7+79122470269',
        '+19122470269',
    ])("invalid input '%s' will be rendered with nbsp at the end", input => {
        wrapper.setProps({value: {main: input}});

        expect(wrapper.render().text()).toEqual(`${input}${nbsp}`);
    });

    test.each([
        ['+79122470269', `+7 (912) 247-02-69${nbsp}`],
        ['+70000000000', `+7 (000) 000-00-00${nbsp}`],
        ['+77777777777', `+7 (777) 777-77-77${nbsp}`],
    ])("valid input '%s' will be transformed to pretty format '%s' (with nbsp at the end)", (input, expected) => {
        wrapper.setProps({value: {main: input}});

        expect(wrapper.render().text()).toEqual(expected);
    });
});
