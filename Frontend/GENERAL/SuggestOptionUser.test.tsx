import React, { ComponentType } from 'react';
import { mount } from 'enzyme';

import { User } from '../../types/User';
import { SuggestOptionUserProps } from './SuggestOptionUser';
import { SuggestOptionUser as SuggestOptionUserDesktop } from './SuggestOptionUser.bundle/desktop';
import { SuggestOptionUser as SuggestOptionUserTouch } from './SuggestOptionUser.bundle/touch-phone';

type SuggestOptionUserType = ComponentType<SuggestOptionUserProps>;

const platforms: [string, SuggestOptionUserType][] = [
    ['desktop', SuggestOptionUserDesktop],
    ['touch', SuggestOptionUserTouch],
];

const user1: User = {
    display: 'Иван Иванов',
    login: 'user',
    department: 'Группа разработки',
};

describe.each(platforms)('SuggestOptionUser@%s', (_platform, SuggestOptionUser) => {
    test('отображение всех полей', () => {
        const wrapper = mount(
            <SuggestOptionUser
                option={user1}
            />,
        );

        const avatar = wrapper.find('.SuggestOptionUser-Avatar').first();

        expect(avatar.prop('value')).toBe(user1);
        expect(avatar.prop('size')).toBe(32);
        expect(avatar.prop('withoutLink')).toBe(true);

        expect(wrapper.find('.SuggestOptionUser-FullName').text()).toBe(user1.display);
        expect(wrapper.find('.SuggestOptionUser-Login').text()).toBe(user1.login);

        const department = wrapper.find('.SuggestOptionUser-Department');

        expect(department).toHaveLength(1);
        expect(department.text()).toBe(user1.department);

        wrapper.unmount();
    });

    test('не выводит department, если его нет', () => {
        const wrapper = mount(
            <SuggestOptionUser
                option={{
                    ...user1,
                    department: undefined,
                }}
            />,
        );

        expect(wrapper.find('.SuggestOptionUser').last()).toHaveLength(1);
        expect(wrapper.find('.SuggestOptionUser-Department')).toHaveLength(0);

        wrapper.unmount();
    });
});
