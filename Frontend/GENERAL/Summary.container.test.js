import React from 'react';
import { mount } from 'enzyme';

import { SummaryContainer } from './Summary.container';

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

const memberLabels = ['User 1', 'User 2'];
const ownerLabels = ['User 3', 'User 4'];
const departmentLabels = ['Dpt 1', 'Dpt 2'];
const tagsLabels = ['Tag 3', 'Tag 4'];

const permissions = {
    view_traffic_light: true,
    view_tags: true,
};

describe('SummaryContainer', () => {
    const setFilters = jest.fn();
    const clearFilters = jest.fn();

    let wrapper;

    beforeEach(() => {
        wrapper = mount(
            <SummaryContainer
                filters={filters}

                memberLabels={memberLabels}
                ownerLabels={ownerLabels}
                departmentLabels={departmentLabels}
                tagsLabels={tagsLabels}

                savingEnabled

                setFilters={setFilters}
                clearFilters={clearFilters}

                permissions={permissions}
            />
        );
    });

    afterEach(() => {
        setFilters.mockReset();
        wrapper.unmount();
    });

    it('Should clear a single param on a cross click', () => {
        const initialCalls = setFilters.mock.calls.length;

        wrapper.find('.Summary-Item_search > .Summary-ItemClearButton').simulate('click');

        expect(setFilters).toHaveBeenCalledTimes(initialCalls + 1);
        expect(setFilters).toHaveBeenCalledWith({ search: '' }, expect.anything());
    });

    it('Should clear all filters with the "clear all" button', () => {
        const initialCalls = clearFilters.mock.calls.length;

        wrapper.find('.Summary > .Summary-FiltersClear').simulate('click');

        expect(clearFilters).toHaveBeenCalledTimes(initialCalls + 1);
        expect(clearFilters).toHaveBeenCalledWith(filters, expect.anything());
    });

    it('Should determine saving in local storage based on prop', () => {
        wrapper.find('.Summary-Item_search > .Summary-ItemClearButton').simulate('click');

        expect(setFilters).toHaveBeenLastCalledWith(expect.anything(), true);

        wrapper.setProps({ savingEnabled: false });

        wrapper.find('.Summary-Item_member > .Summary-ItemClearButton').simulate('click');

        expect(setFilters).toHaveBeenLastCalledWith(expect.anything(), false);
    });
});
