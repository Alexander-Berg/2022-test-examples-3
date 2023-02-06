import React from 'react';
import { render } from 'enzyme';

import { Filters } from './Filters';

jest.mock('./components/Department/Department');
jest.mock('./components/Externals/Externals');
jest.mock('./components/IsSuspicious/IsSuspicious');
jest.mock('./components/Member/Member');
jest.mock('./components/Owner/Owner');
jest.mock('./components/Search/Search');
jest.mock('./components/States/States');
jest.mock('./components/Tags/Tags');

const { BEM_LANG } = process.env;

describe('Should render Filters', () => {
    const commonProps = {
        currentUser: {
            login: 'user0',
            name: { [BEM_LANG]: 'Юзер0' },
        },
        currentDepartment: 1,

        onSuggestAdd: jest.fn(),
        onSearchChange: jest.fn(),
        onStatesChange: jest.fn(),
        onIsSuspiciousChange: jest.fn(),
        onExternalsChange: jest.fn(),
        onMemberChange: jest.fn(),
        onOwnerChange: jest.fn(),
        onDepartmentChange: jest.fn(),
        onTagsChange: jest.fn(),

        filters: {
            search: 'AB',
            member: ['user2', 'user3'],
            owner: ['user4'],
            department: [2, 3],
            states: ['develop', 'needinfo'],
            isSuspicious: [false],
            hasExternalMembers: [true],
            tags: [10, 122],
        },
    };

    it('default', () => {
        const wrapper = render(
            <Filters
                {...commonProps}
                permissions={{
                    view_details: true,
                    view_tags: true,
                    view_traffic_light: true,
                }}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('Should not render state, perfection and tags filters for limited role', () => {
        const wrapper = render(
            <Filters
                {...commonProps}
                permissions={{}}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });
});
