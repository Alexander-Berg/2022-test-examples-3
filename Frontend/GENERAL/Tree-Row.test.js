import React from 'react';
import { render } from 'enzyme';

import { TreeRow } from './Tree-Row';

jest.mock('../../../../Perfection/components/PerfectionTrafficLights/PerfectionTrafficLights.container');
jest.mock('../../../../../common/components/ComponentWithTooltip/ComponentWithTooltip');
jest.mock('../../Icon/components/ServiceState/ServiceStateIcon');
jest.mock('../../Icon/components/ServiceWarnings/ServiceWarningsIcon');

const permissions = {
    can_filter_and_click: true,
    view_all_services: true,
    view_traffic_light: true,
    view_details: true,
    view_team: true,
};

const data = {
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
};

const trafficLights = [
    {
        id: 3852,
        group: {
            code: 'children',
            id: 4,
            name: { ru: 'Вложенные сервисы' },
        },
        level: 'ok',
        issuesCount: 0,
    },
    {
        id: 1706,
        group: {
            code: 'understandability',
            id: 3,
            name: { ru: 'Понятность' },
        },
        level: 'warning',
        issuesCount: 2,
    },

];

const tags = [
    {
        id: 654,
        name: { ru: 'тэг', en: 'tag' },
        slug: 'tag',
        color: 'rgb(1, 1, 1)',
    },
];

describe('Should render Tree-Row', () => {
    describe('with/without isOpen icon', () => {
        it('open icon', () => {
            const wrapper = render(
                <TreeRow
                    data={data}
                    isOpen
                    onClick={() => {}}
                    permissions={permissions}
                />
            );

            expect(wrapper).toMatchSnapshot();
        });

        it('closed icon', () => {
            const wrapper = render(
                <TreeRow
                    data={data}
                    isOpen={false}
                    onClick={() => {}}
                    permissions={permissions}
                />
            );

            expect(wrapper).toMatchSnapshot();
        });

        it('without icon', () => {
            const wrapper = render(
                <TreeRow
                    data={{
                        ...data,
                        childrenCount: 0,
                    }}
                    isOpen
                    onClick={() => {}}
                    permissions={permissions}
                />
            );

            expect(wrapper).toMatchSnapshot();
        });
    });

    describe('with state icon with type', () => {
        it('develop', () => {
            const wrapper = render(
                <TreeRow
                    data={{ ...data, state: 'develop' }}
                    isOpen
                    onClick={() => {}}
                    permissions={permissions}
                />
            );

            expect(wrapper).toMatchSnapshot();
        });

        it('supported', () => {
            const wrapper = render(
                <TreeRow
                    data={{ ...data, state: 'supported' }}
                    isOpen
                    onClick={() => {}}
                    permissions={permissions}
                />
            );

            expect(wrapper).toMatchSnapshot();
        });

        it('needinfo', () => {
            const wrapper = render(
                <TreeRow
                    data={{ ...data, state: 'needinfo' }}
                    isOpen
                    onClick={() => {}}
                    permissions={permissions}
                />
            );

            expect(wrapper).toMatchSnapshot();
        });

        it('closed', () => {
            const wrapper = render(
                <TreeRow
                    data={{ ...data, state: 'closed' }}
                    isOpen
                    onClick={() => {}}
                    permissions={permissions}
                />
            );

            expect(wrapper).toMatchSnapshot();
        });
    });

    describe('with warning icon with type', () => {
        it('no exportable', () => {
            const wrapper = render(
                <TreeRow
                    data={{ ...data, isExternals: false }}
                    isOpen
                    onClick={() => {}}
                    permissions={permissions}
                />
            );

            expect(wrapper).toMatchSnapshot();
        });

        it('has external members', () => {
            const wrapper = render(
                <TreeRow
                    data={{ ...data, hasExternalMembers: true }}
                    isOpen
                    onClick={() => {}}
                    permissions={permissions}
                />
            );

            expect(wrapper).toMatchSnapshot();
        });
    });

    describe('with dashes in', () => {
        it('owner', () => {
            const wrapper = render(
                <TreeRow
                    data={{ ...data, owner: null }}
                    isOpen
                    onClick={() => {}}
                    permissions={permissions}
                />
            );

            expect(wrapper).toMatchSnapshot();
        });

        it('people count', () => {
            const wrapper = render(
                <TreeRow
                    data={{ ...data, peopleCount: null }}
                    isOpen
                    onClick={() => {}}
                    permissions={permissions}
                />
            );

            expect(wrapper).toMatchSnapshot();
        });

        it('robots count', () => {
            const wrapper = render(
                <TreeRow
                    data={{ ...data, robotsCount: null }}
                    isOpen
                    onClick={() => {}}
                    permissions={permissions}
                />
            );

            expect(wrapper).toMatchSnapshot();
        });

        it('traffic lights', () => {
            const wrapper = render(
                <TreeRow
                    data={{ ...data, trafficLights: null }}
                    isOpen
                    onClick={() => {}}
                    permissions={permissions}
                />
            );

            expect(wrapper).toMatchSnapshot();
        });
    });

    describe('matched/unmatched', () => {
        it('matched', () => {
            const wrapper = render(
                <TreeRow
                    data={{ ...data, matched: true }}
                    isOpen
                    onClick={() => {}}
                    permissions={permissions}
                />
            );

            expect(wrapper).toMatchSnapshot();
        });

        it('unmatched', () => {
            const wrapper = render(
                <TreeRow
                    data={{ ...data, matched: false }}
                    isOpen
                    onClick={() => {}}
                    permissions={permissions}
                />
            );

            expect(wrapper).toMatchSnapshot();
        });
    });

    describe('with a shift', () => {
        it('0', () => {
            const wrapper = render(
                <TreeRow
                    data={{ ...data, level: 0 }}
                    isOpen
                    onClick={() => {}}
                    permissions={permissions}
                />
            );

            expect(wrapper).toMatchSnapshot();
        });

        it('1', () => {
            const wrapper = render(
                <TreeRow
                    data={{ ...data, level: 1 }}
                    isOpen
                    onClick={() => {}}
                    permissions={permissions}
                />
            );

            expect(wrapper).toMatchSnapshot();
        });

        it('2', () => {
            const wrapper = render(
                <TreeRow
                    data={{ ...data, level: 2 }}
                    isOpen
                    onClick={() => {}}
                    permissions={permissions}
                />
            );

            expect(wrapper).toMatchSnapshot();
        });
    });

    describe('considering permissions', () => {
        describe('for state', () => {
            it('Should not render state cell without permissions view_details and view_team', () => {
                const wrapper = render(
                    <TreeRow
                        data={data}
                        isOpen
                        onClick={() => {}}
                        permissions={{
                            ...permissions,
                            view_details: false,
                            view_team: false,
                        }}
                    />
                );

                expect(wrapper).toMatchSnapshot();
            });

            it('Should render empty state cell without permission view_details', () => {
                const wrapper = render(
                    <TreeRow
                        data={data}
                        isOpen
                        onClick={() => {}}
                        permissions={{
                            ...permissions,
                            view_details: false,
                        }}
                    />
                );

                expect(wrapper).toMatchSnapshot();
            });

            it('Should render state cell with permission view_details', () => {
                const wrapper = render(
                    <TreeRow
                        data={data}
                        isOpen
                        onClick={() => {}}
                        permissions={{
                            ...permissions,
                            view_details: true,
                        }}
                    />
                );

                expect(wrapper).toMatchSnapshot();
            });
        });
    });

    describe('with trafficLights', () => {
        it('show with permission [view_traffic_light]', () => {
            const wrapper = render(
                <TreeRow
                    data={{
                        ...data,
                        trafficLights,
                    }}
                    isOpen
                    onClick={() => {}}
                    permissions={{
                        ...permissions,
                        view_traffic_light: true,
                    }}
                />
            );

            expect(wrapper).toMatchSnapshot();
        });

        it('hide without permission [view_traffic_light]', () => {
            const wrapper = render(
                <TreeRow
                    data={{
                        ...data,
                        trafficLights,
                    }}
                    isOpen
                    onClick={() => {}}
                    permissions={{
                        ...permissions,
                        view_traffic_light: false,
                    }}
                />
            );

            expect(wrapper).toMatchSnapshot();
        });
    });

    describe('with tags', () => {
        it('show with permission [view_tags]', () => {
            const wrapper = render(
                <TreeRow
                    data={{
                        ...data,
                        tags,
                    }}
                    isOpen
                    onClick={() => {}}
                    permissions={{
                        ...permissions,
                        view_tags: true,
                    }}
                />
            );

            expect(wrapper).toMatchSnapshot();
        });

        it('hide without permission [view_tags]', () => {
            const wrapper = render(
                <TreeRow
                    data={{
                        ...data,
                        tags,
                    }}
                    isOpen
                    onClick={() => {}}
                    permissions={{
                        ...permissions,
                        view_tags: false,
                    }}
                />
            );

            expect(wrapper).toMatchSnapshot();
        });
    });
});
