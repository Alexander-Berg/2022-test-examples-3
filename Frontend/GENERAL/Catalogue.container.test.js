import React from 'react';
import { mount } from 'enzyme';

import { FORCE_REPL } from 'tools-access-react-redux-router/src/actions';
import { CatalogueContainer } from './Catalogue.container';

const permissions = {
    can_filter_and_click: true,
    view_all_services: true,
    view_traffic_light: true,
};

jest.mock('./Catalogue');

describe('Catalogue Container', () => {
    it('Should use 0 as default root service id', () => {
        const wrapper = mount(
            <CatalogueContainer
                filtersFromUrl={{}}
                permissions={permissions}
            />
        );

        expect(wrapper.prop('rootServiceId')).toBe(0);

        wrapper.unmount();
    });

    describe('Should use local storage for the root sevices', () => {
        it('Should load saved filters', () => {
            const updateQueryStr = jest.fn();

            window.localStorage.setItem('catalogue.filters', JSON.stringify({ member: ['trshkv'] }));

            const wrapper = mount(
                <CatalogueContainer
                    isUrlEmpty
                    permissions={permissions}
                    rootServiceId={0}
                    updateQueryStr={updateQueryStr}
                />
            );

            // должно прочитать из ls
            expect(updateQueryStr).toHaveBeenCalledWith(expect.objectContaining({ member: ['trshkv'] }), FORCE_REPL);

            wrapper.unmount();
        });

        it('Should save filters from url to local storage', () => {
            const setFiltersRequest = jest.fn();
            let filtersFromUrl = {
                search: 'lookup',
                member: ['trshkv', 'uruz'],
                owner: ['ssav'],
                department: [42],
                states: ['develop'],
                isSuspicious: [true],
                hasExternalMembers: [false],
                tags: [146],
            };

            const wrapper = mount(
                <CatalogueContainer
                    filtersFromUrl={filtersFromUrl}
                    isUrlEmpty={false}
                    permissions={permissions}
                    rootServiceId={0}
                    setFiltersRequest={setFiltersRequest}
                />
            );

            expect(JSON.parse(window.localStorage.getItem('catalogue.filters'))).toEqual(filtersFromUrl);

            wrapper.unmount();
        });
    });

    describe('Should not use local storage for subservices', () => {
        it('Should not load saved filters', () => {
            const updateQueryStr = jest.fn();

            window.localStorage.setItem('catalogue.filters', JSON.stringify({ member: ['trshkv'] }));

            const wrapper = mount(
                <CatalogueContainer
                    updateQueryStr={updateQueryStr}
                    rootServiceId={42}
                    isUrlEmpty
                />
            );

            expect(updateQueryStr).not.toHaveBeenCalled();

            wrapper.unmount();
        });

        it('Should not save filters to local storage', () => {
            const setFiltersRequest = jest.fn();
            let filtersFromUrl = {
                search: 'lookup',
                member: ['trshkv', 'uruz'],
                owner: ['ssav'],
                department: [42],
                states: ['develop'],
                isSuspicious: [true],
                hasExternalMembers: [false],
                tags: [146],
            };

            window.localStorage.setItem('catalogue.filters', JSON.stringify({}));

            const wrapper = mount(
                <CatalogueContainer
                    filtersFromUrl={filtersFromUrl}
                    isUrlEmpty={false}
                    permissions={permissions}
                    rootServiceId={42}
                    setFiltersRequest={setFiltersRequest}
                />
            );

            expect(JSON.parse(window.localStorage.getItem('catalogue.filters'))).toEqual({});

            wrapper.unmount();
        });
    });
});
