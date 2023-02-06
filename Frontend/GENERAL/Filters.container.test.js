import React from 'react';
import { mount } from 'enzyme';

import { FiltersContainer } from './Filters.container';

const { BEM_LANG } = process.env;

const filters = {
    search: 'ABC',
    states: ['develop', 'supported'],
    isSuspicious: [true],
    hasExternalMembers: [false],
};

const permissions = {
    view_details: true,
    view_tags: true,
    view_traffic_light: true,
};

describe('Filters Container', () => {
    const setFilters = jest.fn();
    const setLabels = jest.fn();

    const commonProps = {
        user: { login: 'user', name: { [BEM_LANG]: 'Юзер' }, department: { id: 1 } },
        savingEnabled: true,

        setFilters,
        setLabels,

        permissions,
    };

    beforeEach(() => {
        window.localStorage.removeItem('catalogue.filters');
        setFilters.mockClear();
        setLabels.mockClear();
    });

    it('Should determine saving in local storage based on prop', () => {
        const wrapper = mount(
            <FiltersContainer
                {...commonProps}
                filters={filters}
            />
        );

        wrapper.find('.Filters-Group_type_search .Textinput-Control').simulate('change', { target: { value: 'Service1' } });

        expect(setFilters).toHaveBeenLastCalledWith(expect.anything(), true);

        wrapper.setProps({ savingEnabled: false });

        wrapper.find('.Filters-Group_type_search .Textinput-Control').simulate('change', { target: { value: 'qwerty' } });

        expect(setFilters).toHaveBeenLastCalledWith(expect.anything(), false);

        wrapper.unmount();
    });

    describe('Should change search filter', () => {
        it('on change', () => {
            const wrapper = mount(
                <FiltersContainer
                    {...commonProps}
                    filters={{ ...filters, search: 'ABC' }}
                />
            );

            wrapper.find('.Filters-Group_type_search .Textinput-Control').simulate('change', { target: { value: 'Service1' } });
            expect(setFilters).toHaveBeenCalledWith({ search: 'Service1' }, expect.anything());
            wrapper.unmount();
        });

        it('on delete', () => {
            const wrapper = mount(
                <FiltersContainer
                    {...commonProps}
                    filters={{ ...filters, search: 'ABC' }}
                />
            );

            wrapper.find('.Filters-Group_type_search .Textinput-Control').simulate('change', { target: { value: '' } });
            expect(setFilters).toHaveBeenCalledWith({ search: '' }, expect.anything());
            wrapper.unmount();
        });
    });

    describe('Should change states filter', () => {
        it('on add in empty filter', () => {
            const wrapper = mount(
                <FiltersContainer
                    {...commonProps}
                    filters={{ ...filters, states: [] }}
                />
            );

            wrapper.find('#states-develop.Checkbox-Control').simulate('change');
            expect(setFilters).toHaveBeenCalledWith({ states: ['develop'] }, expect.anything());
            wrapper.unmount();
        });

        it('on add in non-empty filter', () => {
            const wrapper = mount(
                <FiltersContainer
                    {...commonProps}
                    filters={{ ...filters, states: ['closed'] }}
                />
            );

            wrapper.find('#states-develop.Checkbox-Control').simulate('change');
            expect(setFilters).toHaveBeenCalledWith({ states: ['closed', 'develop'] }, expect.anything());
            wrapper.unmount();
        });

        it('on delete some states filter', () => {
            const wrapper = mount(
                <FiltersContainer
                    {...commonProps}
                    filters={{ ...filters, states: ['closed', 'needinfo'] }}
                />
            );

            wrapper.find('#states-closed.Checkbox-Control').simulate('change');
            expect(setFilters).toHaveBeenCalledWith({ states: ['needinfo'] }, expect.anything());
            wrapper.unmount();
        });

        it('on delete whole states filter', () => {
            const wrapper = mount(
                <FiltersContainer
                    {...commonProps}
                    filters={{ ...filters, states: ['closed'] }}
                />
            );

            wrapper.find('#states-closed.Checkbox-Control').simulate('change');
            expect(setFilters).toHaveBeenCalledWith({ states: '' }, expect.anything());
            wrapper.unmount();
        });
    });

    describe('Should change isSuspicious filter', () => {
        it('on change', () => {
            const wrapper = mount(
                <FiltersContainer
                    {...commonProps}
                    filters={{ ...filters, isSuspicious: [true] }}
                />
            );

            wrapper.find('#is-suspicious-false.Checkbox-Control').simulate('change');
            expect(setFilters).toHaveBeenCalledWith({ isSuspicious: [true, false] }, expect.anything());
            wrapper.unmount();
        });

        it('on delete some filter', () => {
            const wrapper = mount(
                <FiltersContainer
                    {...commonProps}
                    filters={{ ...filters, isSuspicious: [true, false] }}
                />
            );

            wrapper.find('#is-suspicious-false.Checkbox-Control').simulate('change');
            expect(setFilters).toHaveBeenCalledWith({ isSuspicious: [true] }, expect.anything());
            wrapper.unmount();
        });

        it('on delete whole filter', () => {
            const wrapper = mount(
                <FiltersContainer
                    {...commonProps}
                    filters={{ ...filters, isSuspicious: [false] }}
                />
            );

            wrapper.find('#is-suspicious-false.Checkbox-Control').simulate('change');
            expect(setFilters).toHaveBeenCalledWith({ isSuspicious: '' }, expect.anything());
            wrapper.unmount();
        });
    });

    describe('Should change externals filter', () => {
        it('on change filter', () => {
            const wrapper = mount(
                <FiltersContainer
                    {...commonProps}
                    filters={{ ...filters, hasExternalMembers: [true] }}
                />
            );

            wrapper.find('#externals-false.Checkbox-Control').simulate('change');
            expect(setFilters).toHaveBeenCalledWith({ hasExternalMembers: [true, false] }, expect.anything());
            wrapper.unmount();
        });

        it('on delete some filter', () => {
            const wrapper = mount(
                <FiltersContainer
                    {...commonProps}
                    filters={{ ...filters, hasExternalMembers: [true, false] }}
                />
            );

            wrapper.find('#externals-false.Checkbox-Control').simulate('change');
            expect(setFilters).toHaveBeenCalledWith({ hasExternalMembers: [true] }, expect.anything());
            wrapper.unmount();
        });

        it('on delete whole filter', () => {
            const wrapper = mount(
                <FiltersContainer
                    {...commonProps}
                    filters={{ ...filters, hasExternalMembers: [false] }}
                />
            );

            wrapper.find('#externals-false.Checkbox-Control').simulate('change');
            expect(setFilters).toHaveBeenCalledWith({ hasExternalMembers: '' }, expect.anything());
            wrapper.unmount();
        });
    });

    describe('Should click on presets', () => {
        it('on department preset', () => {
            const wrapper = mount(
                <FiltersContainer
                    {...commonProps}
                    // Здесь нет поля department
                    filters={filters}
                />
            );

            wrapper.find('.Filters-Group_type_department .Preset .Button2 button').simulate('click');
            expect(setFilters).toHaveBeenCalledWith({ department: [1] }, expect.anything());
            wrapper.unmount();
        });

        it('on member preset', () => {
            const wrapper = mount(
                <FiltersContainer
                    {...commonProps}
                    // Здесь нет поля member
                    filters={filters}
                />
            );

            wrapper.find('.Filters-Group_type_member .Preset .Button2 button').simulate('click');
            expect(setFilters).toHaveBeenCalledWith({ member: ['user'] }, expect.anything());
            wrapper.unmount();
        });

        it('on owner preset', () => {
            const wrapper = mount(
                <FiltersContainer
                    {...commonProps}
                    // Здесь нет поля owner
                    filters={filters}
                />
            );

            wrapper.find('.Filters-Group_type_owner .Preset .Button2 button').simulate('click');
            expect(setFilters).toHaveBeenCalledWith({ owner: ['user'] }, expect.anything());
            wrapper.unmount();
        });
    });
});
