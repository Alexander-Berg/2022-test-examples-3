import React from 'react';
import { render } from 'enzyme';

import { Summary } from './Summary';

jest.mock('./Item/Summary-Item');

const filters = {
    search: 'name',
    member: ['user1', 'user2'],
    owner: ['user3', 'user4'],
    department: [1, 2],
    states: ['develop', 'supported', 'needinfo', 'closed'],
    isSuspicious: [true, false],
    hasExternalMembers: [true, false],
    tags: [3, 4],
};

const permissions = {
    view_details: true,
    view_tags: true,
    view_traffic_light: true,
};

describe('Summary', () => {
    it('all filters', () => {
        const wrapper = render(
            <Summary
                filters={filters}
                onItemClear={jest.fn()}
                onFiltersClear={jest.fn()}
                permissions={permissions}
            />,
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('no filters', () => {
        const wrapper = render(
            <Summary
                filters={{}}
                onItemClear={jest.fn()}
                onFiltersClear={jest.fn()}
                permissions={permissions}
            />,
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('with labels', () => {
        const wrapper = render(
            <Summary
                filters={filters}
                onItemClear={jest.fn()}
                onFiltersClear={jest.fn()}
                memberLabels={['User 1', 'User 2']}
                ownerLabels={['Owner 3', 'Owner 4']}
                departmentLabels={['Dep 1', 'Dep 2']}
                tagsLabels={['Tag3', 'Tag4']}
                permissions={permissions}
            />,
        );

        expect(wrapper).toMatchSnapshot();
    });
});
