import React from 'react';
import { mount } from 'enzyme';

import { Summary } from './Summary';

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
    view_traffic_light: true,
    view_tags: true,
};

describe('Summary unmocked', () => {
    it('Should check onClear', () => {
        const onClear = jest.fn();

        const wrapper = mount(
            <Summary
                filters={filters}
                onItemClear={onClear}
                onFiltersClear={jest.fn()}
                permissions={permissions}
            />,
        );

        const initialCalls = onClear.mock.calls.length;
        wrapper.find('.Summary-Item_search>.Summary-ItemClearButton').simulate('click');
        expect(onClear).toHaveBeenCalledTimes(initialCalls + 1);

        wrapper.unmount();
    });
});
