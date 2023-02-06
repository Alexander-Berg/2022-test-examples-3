import React from 'react';
import { mount, ReactWrapper } from 'enzyme';

import { Filters, OnlyMineFilter } from './Filters';

describe('Filters', () => {
    describe('With direct filter', () => {
        let onChange: jest.Mock;

        let wrapper: ReactWrapper;

        beforeEach(() => {
            onChange = jest.fn();

            wrapper = mount(
                <Filters
                    onlyMineFilter={OnlyMineFilter.DIRECT}
                    onChange={onChange}
                />,
            );
        });

        afterEach(() => {
            jest.clearAllMocks();
            wrapper.unmount();
        });

        it('Should check presence of all buttons', () => {
            const directFilterButton = wrapper.find('.Requests-FilterButton_type_direct').hostNodes();
            expect(directFilterButton).toHaveLength(1);

            const hierarchyFilterButton = wrapper.find('.Requests-FilterButton_type_hierarchy').hostNodes();
            expect(hierarchyFilterButton).toHaveLength(1);
        });

        it('Should have only one checked button - direct button', () => {
            const checkedDirectFilterButton = wrapper.find('.Requests-FilterButton_type_direct.Button2_checked').hostNodes();
            expect(checkedDirectFilterButton).toHaveLength(1);

            const checkedHierarchyFilterButton = wrapper.find('.Requests-FilterButton_type_hierarchy.Button2_checked').hostNodes();
            expect(checkedHierarchyFilterButton).toHaveLength(0);
        });

        it('Should call onChange with hierarchy filter after hierarchy button click', () => {
            const hierarchyFilterButton = wrapper.find('.Requests-FilterButton_type_hierarchy').hostNodes();

            hierarchyFilterButton.simulate('click');
            expect(onChange).toHaveBeenCalledWith(OnlyMineFilter.HIERARCHY);
        });
    });

    describe('With hierarchy filter', () => {
        let onChange: jest.Mock;

        let wrapper: ReactWrapper;

        beforeEach(() => {
            onChange = jest.fn();

            wrapper = mount(
                <Filters
                    onlyMineFilter={OnlyMineFilter.HIERARCHY}
                    onChange={onChange}
                />,
            );
        });

        afterEach(() => {
            jest.clearAllMocks();
            wrapper.unmount();
        });

        it('Should check presence of all buttons', () => {
            const directFilterButton = wrapper.find('.Requests-FilterButton_type_direct').hostNodes();
            expect(directFilterButton).toHaveLength(1);

            const hierarchyFilterButton = wrapper.find('.Requests-FilterButton_type_hierarchy').hostNodes();
            expect(hierarchyFilterButton).toHaveLength(1);
        });

        it('Should have only one checked button - hierarchy button', () => {
            const checkedDirectFilterButton = wrapper.find('.Requests-FilterButton_type_direct.Button2_checked').hostNodes();
            expect(checkedDirectFilterButton).toHaveLength(0);

            const checkedHierarchyFilterButton = wrapper.find('.Requests-FilterButton_type_hierarchy.Button2_checked').hostNodes();
            expect(checkedHierarchyFilterButton).toHaveLength(1);
        });

        it('Should call onChange with direct filter after direct button click', () => {
            const directFilterButton = wrapper.find('.Requests-FilterButton_type_direct').hostNodes();

            directFilterButton.simulate('click');
            expect(onChange).toHaveBeenCalledWith(OnlyMineFilter.DIRECT);
        });
    });
});
