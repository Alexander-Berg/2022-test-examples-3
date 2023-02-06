import React, { ComponentType } from 'react';
import { mount } from 'enzyme';

import { User } from '../../types/User';
import { SuggestValueUserProps } from './SuggestValueUser';
import { SuggestValueUser as SuggestValueUserDesktop } from './SuggestValueUser.bundle/desktop';
import { SuggestValueUser as SuggestValueUserTouch } from './SuggestValueUser.bundle/touch-phone';

type SuggestValueUserType = ComponentType<SuggestValueUserProps>;

const platforms: [string, SuggestValueUserType][] = [
    ['desktop', SuggestValueUserDesktop],
    ['touch', SuggestValueUserTouch],
];
const user: User = {
    display: 'name',
    login: 'login',
};

describe.each(platforms)('SuggestValueUser@%s', (_platform, SuggestValueUser) => {
    test('выводит Avatar и Name', () => {
        const wrapper = mount(
            <SuggestValueUser
                value={user}
            />,
        );

        expect(wrapper.find('.SuggestValueUser').last()).toHaveLength(1);

        const avatar = wrapper.find('.SuggestValueUser-Avatar').first();
        const name = wrapper.find('.SuggestValueUser-Name').first();

        expect(avatar.prop('value')).toBe(user);
        expect(avatar.prop('size')).toBe(20);
        expect(avatar.prop('withoutGap')).toBe(true);
        expect(name.prop('value')).toBe(user);

        wrapper.unmount();
    });
});
