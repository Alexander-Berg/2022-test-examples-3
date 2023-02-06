import React from 'react';
import { render } from 'enzyme';

import { ALL_PERMISSIONS } from '../../../../../common/redux/common.constants';

import { TreeHeader } from './Tree-Header';

const FULL_PERMISSIONS = ALL_PERMISSIONS.reduce((acc, permission) => {
    acc[permission] = true;
    return acc;
}, {});

describe('Tree-Header', () => {
    let referenceWrapper;

    beforeAll(() => {
        referenceWrapper = render(<TreeHeader permissions={FULL_PERMISSIONS} />);
    });

    it('Full render', () => {
        expect(referenceWrapper).toMatchSnapshot();
    });

    describe('Granular access', () => {
        let allColumns;

        beforeAll(() => {
            allColumns = referenceWrapper.find('.Tree-HeaderCell');
        });

        it('Should render state column depending on view_details and view_team permission', () => {
            const wrapper = render(
                <TreeHeader
                    permissions={{
                        ...FULL_PERMISSIONS,
                        view_details: false,
                        view_team: false,
                    }}
                />
            );

            expect(wrapper.find('.Tree-HeaderCell').length).toBe(allColumns.length - 1);
            expect(referenceWrapper.find('.Tree-HeaderCell_type_state').length).toBe(1);
            expect(wrapper.find('.Tree-HeaderCell_type_state').length).toBe(0);
        });

        ['view_details', 'view_team'].forEach(permission => {
            it(`Should render state column depending on ${permission} permission`, () => {
                const wrapper = render(
                    <TreeHeader
                        permissions={{
                            ...FULL_PERMISSIONS,
                            [permission]: false,
                        }}
                    />
                );

                expect(wrapper.find('.Tree-HeaderCell').length).toBe(allColumns.length);
                expect(referenceWrapper.find('.Tree-HeaderCell_type_state').length).toBe(1);
                expect(wrapper.find('.Tree-HeaderCell_type_state').length).toBe(1);
                expect(referenceWrapper.find('.Tree-HeaderCell_type_state')[0].children.length).toBe(1);
                expect(wrapper.find('.Tree-HeaderCell_type_state')[0].children.length).toBe(1);
            });
        });

        it('Should render perfection column depending on view_traffic_light permission', () => {
            const wrapper = render(
                <TreeHeader
                    permissions={{
                        ...FULL_PERMISSIONS,
                        view_traffic_light: false,
                    }}
                />
            );

            expect(wrapper.find('.Tree-HeaderCell').length).toBe(allColumns.length - 1);
            expect(referenceWrapper.find('.Tree-HeaderCell_type_perfection').length).toBe(1);
            expect(wrapper.find('.Tree-HeaderCell_type_perfection').length).toBe(0);
        });
    });
});
