import React from 'react';

import { shallow } from 'enzyme';
import ServiceCreation from './ServiceCreation';
import { IUser } from '../../common/prop-types/userType';

const testUser: IUser = {
    login: 'testLogin',
    name: { ru: 'testName', en: 'testName' },
    department: { id: 13, name: { ru: 'testDepartment', en: 'testDepartment' } },
    first_name: { ru: 'testFirstName', en: 'testFirstName' },
    last_name: { ru: 'testLastName', en: 'testLastName' },
    uid: 'testUid',
};

describe('ServiceCreation', () => {
    const wrapper = shallow(
        <ServiceCreation user={testUser} />,
    );

    it('Should render text title', () => {
        expect(wrapper.find('.ServiceCreation-Title').render().text()).toEqual('i18n:service-creation');
    });

    it('Should render wizard', () => {
        expect(wrapper.find('.ServiceCreation-Wizard')).toHaveLength(1);
    });
});
