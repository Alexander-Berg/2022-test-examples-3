import React, { ComponentType } from 'react';
import { mount } from 'enzyme';

import { User } from '../../types/User';
import { UserAvatarProps } from './UserAvatar';
import { UserAvatar as UserAvatarDesktop } from './UserAvatar.bundle/desktop';
import { UserAvatar as UserAvatarTouch } from './UserAvatar.bundle/desktop';

type UserAvatarType = ComponentType<UserAvatarProps>;

const platforms: [string, UserAvatarType][] = [
    ['desktop', UserAvatarDesktop],
    ['touch', UserAvatarTouch],
];

const user1: User = {
    display: 'Отображаемое Имя',
    login: 'login',
    url: '//localhost',
    icon: '//localhost/avatar.jpg',
    dismissed: false,
    hasLicense: true,
    gap: {
        type: 'vacation',
        working: true,
    },
};

const user2: User = {
    ...user1,
    gap: {
        type: 'vacation',
        working: false,
        message: 'Отпуск',
    },
};

describe.each(platforms)('UserAvatar@%s', (_platform, UserAvatar) => {
    describe.each([
        ['src', user1.icon],
        ['alt', user1.display],
        ['width', 32],
        ['height', 32],
        ['loading', 'lazy'],
    ] as [string, string | number][])('дефолтные настройки изображения', (property, value) => {
        test(`параметр ${property}`, () => {
            const wrapper = mount(
                <UserAvatar
                    value={user1}
                />,
            );

            expect(wrapper.find('.UserAvatar-Image').prop(property)).toBe(value);

            wrapper.unmount();
        });
    });

    test('изменение параметра "size"', () => {
        const wrapper = mount(
            <UserAvatar
                value={user1}
                size={60}
            />,
        );

        const image = wrapper.find('.UserAvatar-Image');

        expect(image.prop('width')).toBe(60);
        expect(image.prop('height')).toBe(60);
        expect(wrapper.find('.UserAvatar').last().prop('style')).toEqual({
            '--user-avatar-size': 60,
        });

        wrapper.unmount();
    });

    test('вывод ссылки', () => {
        const wrapper = mount(
            <UserAvatar
                value={user1}
            />,
        );

        const component = wrapper.find('Link.UserAvatar').last();

        expect(component).toHaveLength(1);
        expect(component.prop('href')).toBe(user1.url);

        wrapper.unmount();
    });

    test('вывод без ссылки', () => {
        const wrapper = mount(
            <UserAvatar
                value={user1}
                withoutLink
            />,
        );

        expect(wrapper.find('Link.UserAvatar')).toHaveLength(0);
        expect(wrapper.find('.UserAvatar')).toHaveLength(1);

        wrapper.unmount();
    });

    test('отсутствие Gap', () => {
        const wrapper = mount(
            <UserAvatar
                value={user1}
            />,
        );

        expect(wrapper.find('.UserAvatar-Gap')).toHaveLength(0);

        wrapper.unmount();
    });

    test('вывод Gap', () => {
        const wrapper = mount(
            <UserAvatar
                value={user2}
            />,
        );

        // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
        const gapData = user2.gap!;
        const gap = wrapper.find('.UserAvatar-Gap');

        expect(gap).toHaveLength(1);
        expect(gap.hasClass('UserAvatar-Gap_type_vacation')).toBe(true);
        expect(gap.text()).toBe(gapData.message);

        wrapper.unmount();
    });

    test('отключение Gap через withoutGap', () => {
        const wrapper = mount(
            <UserAvatar
                value={user2}
                withoutGap
            />,
        );

        expect(wrapper.find('.UserAvatar-Gap')).toHaveLength(0);

        wrapper.unmount();
    });

    test('отсутствие Gap, если dismissed = true', () => {
        const wrapper = mount(
            <UserAvatar
                value={{
                    ...user2,
                    dismissed: true,
                }}
            />,
        );

        expect(wrapper.find('.UserAvatar-Gap')).toHaveLength(0);

        wrapper.unmount();
    });

    test('отсутствие Gap, если hasLicense = false', () => {
        const wrapper = mount(
            <UserAvatar
                value={{
                    ...user2,
                    hasLicense: false,
                }}
            />,
        );

        expect(wrapper.find('.UserAvatar-Gap')).toHaveLength(0);

        wrapper.unmount();
    });
});
