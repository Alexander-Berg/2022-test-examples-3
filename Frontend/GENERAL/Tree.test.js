import React from 'react';
import { render } from 'enzyme';

import { Tree } from './Tree';

jest.mock('./Header/Tree-Header');
jest.mock('./Row/Tree-Row');

const permissions = {
    can_filter_and_click: true,
    view_all_services: true,
    view_traffic_light: true,
    view_details: true,
};

const service1 = {
    data: {
        matched: true,
        level: 0,
        childrenCount: 2,
        showAllFields: true,

        name: { ru: 'КотикиДелаютКусь' },
        slug: 'testservice',

        owner: {
            login: 'gnoma',
            name: { ru: 'Анна Сеничкина' },
        },

        state: 'supported',
        isExportable: true,
        hasExternalMembers: false,

        peopleCount: 1001,

        robotsCount: 11,

        trafficLights: null,
        id: 11,
    },
    isOpen: true,
    childrenDataStatus: 'inited',
};

const service2 = {
    data: {
        matched: false,
        level: 1,
        childrenCount: 2,
        showAllFields: false,

        name: { ru: 'Тестовый' },
        slug: 'testservice1',

        owner: {
            login: 'alexey-zhur',
            name: { ru: 'Алексей Журавлев' },
        },

        state: 'closed',
        isExportable: true,
        hasExternalMembers: false,

        peopleCount: 100,

        robotsCount: 10,

        trafficLights: null,
        id: 100,
    },
    isOpen: false,
    childrenDataStatus: 'inited',
};

const error = { data: { detail: 'Error' }, extra: 'Error' };

describe('Should render Tree', () => {
    it('default', () => {
        const wrapper = render(
            <Tree
                error={null}
                list={[service1, service2]}
                onNodeClick={() => {}}
                onShowUnmatched={() => {}}
                permissions={permissions}
                status="inited"
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('empty', () => {
        const wrapper = render(
            <Tree
                error={null}
                list={[]}
                onNodeClick={() => {}}
                onShowUnmatched={() => {}}
                permissions={permissions}
                status="inited"
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('with error', () => {
        const wrapper = render(
            <Tree
                error={error}
                list={[service1, service2]}
                onNodeClick={() => {}}
                onShowUnmatched={() => {}}
                permissions={permissions}
                status="inited"
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('with loading', () => {
        const wrapper = render(
            <Tree
                error={null}
                list={[service1, service2]}
                onNodeClick={() => {}}
                onShowUnmatched={() => {}}
                permissions={permissions}
                status="loading"
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('with error in service', () => {
        const wrapper = render(
            <Tree
                error={null}
                list={[
                    { ...service1, childrenError: error },
                    service2,
                ]}
                onNodeClick={() => {}}
                onShowUnmatched={() => {}}
                permissions={permissions}
                status="inited"
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('with loading in service', () => {
        const wrapper = render(
            <Tree
                error={null}
                list={[
                    { ...service1, childrenDataStatus: 'loading' },
                    service2,
                ]}
                onNodeClick={() => {}}
                onShowUnmatched={() => {}}
                permissions={permissions}
                status="inited"
            />
        );

        expect(wrapper).toMatchSnapshot();
    });
});
