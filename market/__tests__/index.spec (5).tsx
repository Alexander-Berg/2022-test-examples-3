/**
 * @jest-environment jsdom
 */

import React from 'react';
import {shallow, mount} from 'enzyme';

import PhoneInput from '..';

describe('PhoneInput', () => {
    it('just render', () => {
        const render = () => shallow(<PhoneInput />);

        expect(render).not.toThrowError();
    });

    it('uncontrolled usage', () => {
        const wrapper = mount(<PhoneInput value="91" />);

        wrapper.setProps({value: '912'});

        wrapper.update();

        expect(wrapper.find('input').prop('value')).toEqual('+7 (912) ___-__-__');
    });
});
