import React from 'react';
import { mount } from 'enzyme';
import { User } from '~/src/common/context/types';
import { withContext } from '~/src/common/hoc';
import { PersonOnDuty as PersonOnDutyBase } from './PersonOnDuty';

const person = {
    id: 123,
    login: 'somelogin',
    firstName: {
        ru: 'Name',
        en: 'Name',
    },
    lastName: {
        ru: 'Last Name',
        en: 'Last Name',
    },
};

describe('PersonOnDuty', () => {
    const abcContextMock = {
        configs: {
            hosts: {
                centerClient: { protocol: 'https:', hostname: 'center.y-t.ru' },
                staff: { protocol: 'https:', hostname: 'staff.y-t.ru' },
            },
        },
        user: {} as User,
    };

    const PersonOnDuty = withContext(PersonOnDutyBase, abcContextMock);

    it('Should render certain person on duty', () => {
        const wrapper = mount(
            <PersonOnDuty person={person} start="2019-10-26" end="2020-11-08" />,
        );

        expect(wrapper.find('.Employee').length).toBe(1);
        expect(wrapper.find('.Employee_empty').length).toBe(0);
        expect(wrapper.find('.Employee-Caption').childAt(0).text()).toBe('i18n:interval.dash.spaces-around');
    });

    it('Should render unassigned person on duty', () => {
        const wrapper = mount(
            <PersonOnDuty person={null} start="2019-10-26" end="2020-11-08" />,
        );

        expect(wrapper.find('.Employee').length).toBe(1);
        expect(wrapper.find('.Employee_empty').length).toBe(1);
        expect(wrapper.find('.Employee-Caption').childAt(0).text()).toBe('i18n:interval.dash.spaces-around');
    });
});
