import React from 'react';
import { shallow } from 'enzyme';

import { ShiftChange } from './ShiftChange';

describe('ShiftChange', () => {
    it('lowercase', () => {
        const wrapper = shallow(
            <ShiftChange
                time="12:00"
                lowercase
            />,
        );

        expect(wrapper.find('.ShiftChange').length).toBe(1);
        expect(wrapper.find('.ShiftChange').text()).toBe('i18n:shift-change-at-lowercase');
    });

    it('no lowercase', () => {
        const wrapper = shallow(
            <ShiftChange
                time="12:00"
            />,
        );

        expect(wrapper.find('.ShiftChange').length).toBe(1);
        expect(wrapper.find('.ShiftChange').text()).toBe('i18n:shift-change-at');
    });

    it('empty if time is 00:00', () => {
        const wrapper = shallow(
            <ShiftChange
                time="00:00"
                lowercase
            />,
        );

        expect(wrapper.find('.ShiftChange').length).toBe(0);
    });

    it('empty if no time', () => {
        const wrapper = shallow(
            <ShiftChange
                lowercase
            />,
        );

        expect(wrapper.find('.ShiftChange').length).toBe(0);
    });
});
