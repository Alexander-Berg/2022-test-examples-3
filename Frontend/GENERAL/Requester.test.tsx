import React from 'react';

import { shallow } from 'enzyme';
import { TaUsername } from '../../../../Dispenser/Dispenser.features/components/Username/TaUsername';

import { getUser } from '../testData/testData';
import { Requester } from './Requester';

describe('Requester', () => {
    it('Should have ta username with correct props', () => {
        const user = getUser(100);

        const wrapper = shallow(
            <Requester
                requester={user}
            />,
        );

        const taUsername = wrapper.find(TaUsername);
        expect(taUsername).toHaveLength(1);
        expect(taUsername.prop('dismissed')).toEqual(user.isDismissed);
        expect(taUsername.prop('href')).toEqual(`//staff.yandex-team.ru/${user.login}`);
        expect(taUsername.prop('username')).toEqual(user.login);
        expect(taUsername.prop('children')).toEqual(user.name.ru);
    });
});
