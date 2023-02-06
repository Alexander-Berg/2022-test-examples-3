import React, { ComponentType } from 'react';
import { mount } from 'enzyme';
import { User } from '../../types/User';
import { UserNameProps } from './UserName';
import { UserName as UserNameDesktop } from './UserName.bundle/desktop';
import { UserName as UserNameTouch } from './UserName.bundle/touch-phone';

type UserNameType = ComponentType<UserNameProps>;

const platforms: [string, UserNameType][] = [
    ['desktop', UserNameDesktop],
    ['touch-phone', UserNameTouch],
];

const user: User = {
    display: 'Отображаемое Имя',
    login: 'login',
    url: '//localhost',
};

describe.each(platforms)('UserName@%s', (_platform, UserName) => {
    test('вывод отображаемого имени', () => {
        const wrapper = mount(
            <UserName
                value={user}
            />,
        );

        expect(wrapper.find('.UserName').first().text()).toBe(user.display);

        wrapper.unmount();
    });

    test('форсированно вывод имени', () => {
        const wrapper = mount(
            <UserName
                value={user}
                preferLogin={false}
            />,
        );

        expect(wrapper.find('.UserName').first().text()).toBe(user.display);

        wrapper.unmount();
    });

    test('форсированно вывод логина', () => {
        const wrapper = mount(
            <UserName
                value={user}
                preferLogin
            />,
        );

        expect(wrapper.find('.UserName').first().text()).toBe(user.login);

        wrapper.unmount();
    });

    test('вывод ссылки', () => {
        const wrapper = mount(
            <UserName
                value={user}
            />,
        );

        const component = wrapper.find('Link.UserName');

        expect(component).toHaveLength(1);
        expect(component.prop('href')).toBe(user.url);

        wrapper.unmount();
    });

    test('вывод без ссылки', () => {
        const wrapper = mount(
            <UserName
                value={user}
                withoutLink
            />,
        );

        expect(wrapper.find('Link.UserName')).toHaveLength(0);
        expect(wrapper.find('.UserName').first()).toHaveLength(1);

        wrapper.unmount();
    });
});
