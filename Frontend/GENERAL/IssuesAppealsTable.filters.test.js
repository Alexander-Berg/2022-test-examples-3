import React from 'react';
import { mount, render } from 'enzyme';

import IssuesAppealsTableFilters from './IssuesAppealsTable.filters';

describe('Should render IssuesAppealsTableFilters', () => {
    const onChangeFilter = jest.fn();

    it('Direct filter', () => {
        const wrapper = render(
            <IssuesAppealsTableFilters
                onlyMineFilter="direct"
                onChangeFilter={onChangeFilter}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('Hierarchy filter', () => {
        const wrapper = render(
            <IssuesAppealsTableFilters
                onlyMineFilter="hierarchy"
                onChangeFilter={onChangeFilter}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('Filter changing', () => {
        const wrapper = mount(
            <IssuesAppealsTableFilters
                onlyMineFilter="hierarchy"
                onChangeFilter={onChangeFilter}
            />
        );

        wrapper.find('.Button2 button').at(0).simulate('click');
        expect(onChangeFilter).toHaveBeenCalledTimes(1);
        expect(onChangeFilter).toHaveBeenCalledWith('direct');

        wrapper.find('.Button2 button').at(1).simulate('click');
        expect(onChangeFilter).toHaveBeenCalledTimes(2);
        expect(onChangeFilter).toHaveBeenCalledWith('hierarchy');
    });
});
