import React from 'react';
import { render } from 'enzyme';

import { amount } from '../../../Folders';
import {
    resourceDto_YDB_RAM_SAS,
    resourceDto_YP_HDD_MAN,
} from '../../../Folders/mock/resources';
import type { ChangeHistoryResourcesById } from '../../../Folders/components/ChangeHistory/ChangeHistory.lib';
import { HistoryFolderResourcesTable } from './History-FolderResourcesTable';

const resourcesById: ChangeHistoryResourcesById = {
    [resourceDto_YDB_RAM_SAS.id]: resourceDto_YDB_RAM_SAS,
    [resourceDto_YP_HDD_MAN.id]: resourceDto_YP_HDD_MAN,
};

describe('History', () => {
    describe('render', () => {
        it('full data', () => {
            const wrapper = render(
                <HistoryFolderResourcesTable
                    resourcesById={resourcesById}
                    oldBalance={{
                        amountByResourceId: {
                            [resourceDto_YDB_RAM_SAS.id]: amount('1000.000', 'GiB'),
                        },
                    }}
                    newBalance={{
                        amountByResourceId: {
                            [resourceDto_YDB_RAM_SAS.id]: amount('1500.001', 'GiB'),
                        },
                    }}
                    balanceDelta={{
                        amountByResourceId: {
                            [resourceDto_YDB_RAM_SAS.id]: amount('500.001', 'GiB'),
                        },
                    }}
                    oldQuotas={{
                        amountByResourceId: {
                            [resourceDto_YDB_RAM_SAS.id]: amount('1000.000', 'GiB'),
                        },
                    }}
                    newQuotas={{
                        amountByResourceId: {
                            [resourceDto_YDB_RAM_SAS.id]: amount('-0.042', 'GiB'),
                        },
                    }}
                    quotasDelta={{
                        amountByResourceId: {
                            [resourceDto_YDB_RAM_SAS.id]: amount('-1000.042', 'GiB'),
                        },
                    }}
                />,
            );
            expect(wrapper).toMatchSnapshot();
        });

        it('same quota', () => {
            const wrapper = render(
                <HistoryFolderResourcesTable
                    resourcesById={resourcesById}
                    oldBalance={{
                        amountByResourceId: {
                            [resourceDto_YDB_RAM_SAS.id]: amount('1000.000', 'GiB'),
                        },
                    }}
                    newBalance={{
                        amountByResourceId: {
                            [resourceDto_YDB_RAM_SAS.id]: amount('1500.001', 'GiB'),
                        },
                    }}
                    balanceDelta={{
                        amountByResourceId: {
                            [resourceDto_YDB_RAM_SAS.id]: amount('500.001', 'GiB'),
                        },
                    }}
                    oldQuotas={{
                        amountByResourceId: {
                            [resourceDto_YDB_RAM_SAS.id]: amount('1000.000', 'GiB'),
                        },
                    }}
                    newQuotas={{
                        amountByResourceId: {
                            [resourceDto_YDB_RAM_SAS.id]: amount('1000.000', 'GiB'),
                        },
                    }}
                    quotasDelta={{
                        amountByResourceId: {
                            [resourceDto_YDB_RAM_SAS.id]: amount('0', 'GiB'),
                        },
                    }}
                />,
            );
            expect(wrapper).toMatchSnapshot();
        });

        it('same balance', () => {
            const wrapper = render(
                <HistoryFolderResourcesTable
                    resourcesById={resourcesById}
                    oldBalance={{
                        amountByResourceId: {
                            [resourceDto_YDB_RAM_SAS.id]: amount('1000.000', 'GiB'),
                        },
                    }}
                    newBalance={{
                        amountByResourceId: {
                            [resourceDto_YDB_RAM_SAS.id]: amount('1000.000', 'GiB'),
                        },
                    }}
                    balanceDelta={{
                        amountByResourceId: {
                            [resourceDto_YDB_RAM_SAS.id]: amount('0', 'GiB'),
                        },
                    }}
                    oldQuotas={{
                        amountByResourceId: {
                            [resourceDto_YDB_RAM_SAS.id]: amount('1000.000', 'GiB'),
                        },
                    }}
                    newQuotas={{
                        amountByResourceId: {
                            [resourceDto_YDB_RAM_SAS.id]: amount('-0.042', 'GiB'),
                        },
                    }}
                    quotasDelta={{
                        amountByResourceId: {
                            [resourceDto_YDB_RAM_SAS.id]: amount('-1000.042', 'GiB'),
                        },
                    }}
                />,
            );
            expect(wrapper).toMatchSnapshot();
        });

        it('same quota and balance', () => {
            const wrapper = render(
                <HistoryFolderResourcesTable
                    resourcesById={resourcesById}
                    oldBalance={{
                        amountByResourceId: {
                            [resourceDto_YDB_RAM_SAS.id]: amount('1000.000', 'GiB'),
                        },
                    }}
                    newBalance={{
                        amountByResourceId: {
                            [resourceDto_YDB_RAM_SAS.id]: amount('1000.000', 'GiB'),
                        },
                    }}
                    balanceDelta={{
                        amountByResourceId: {
                            [resourceDto_YDB_RAM_SAS.id]: amount('0', 'GiB'),
                        },
                    }}
                    oldQuotas={{
                        amountByResourceId: {
                            [resourceDto_YDB_RAM_SAS.id]: amount('1000.000', 'GiB'),
                        },
                    }}
                    newQuotas={{
                        amountByResourceId: {
                            [resourceDto_YDB_RAM_SAS.id]: amount('1000.000', 'GiB'),
                        },
                    }}
                    quotasDelta={{
                        amountByResourceId: {
                            [resourceDto_YDB_RAM_SAS.id]: amount('0', 'GiB'),
                        },
                    }}
                />,
            );
            expect(wrapper).toMatchSnapshot();
        });

        it('only quota', () => {
            const wrapper = render(
                <HistoryFolderResourcesTable
                    resourcesById={resourcesById}
                    oldBalance={{ amountByResourceId: {} }}
                    newBalance={{ amountByResourceId: {} }}
                    balanceDelta={{ amountByResourceId: {} }}
                    oldQuotas={{
                        amountByResourceId: {
                            [resourceDto_YDB_RAM_SAS.id]: amount('1000.000', 'GiB'),
                        },
                    }}
                    newQuotas={{
                        amountByResourceId: {
                            [resourceDto_YDB_RAM_SAS.id]: amount('-0.042', 'GiB'),
                        },
                    }}
                    quotasDelta={{
                        amountByResourceId: {
                            [resourceDto_YDB_RAM_SAS.id]: amount('-1000.042', 'GiB'),
                        },
                    }}
                />,
            );
            expect(wrapper).toMatchSnapshot();
        });

        it('only balance', () => {
            const wrapper = render(
                <HistoryFolderResourcesTable
                    resourcesById={resourcesById}
                    oldBalance={{
                        amountByResourceId: {
                            [resourceDto_YDB_RAM_SAS.id]: amount('1000.000', 'GiB'),
                        },
                    }}
                    newBalance={{
                        amountByResourceId: {
                            [resourceDto_YDB_RAM_SAS.id]: amount('1500.001', 'GiB'),
                        },
                    }}
                    balanceDelta={{
                        amountByResourceId: {
                            [resourceDto_YDB_RAM_SAS.id]: amount('500.001', 'GiB'),
                        },
                    }}
                    oldQuotas={{ amountByResourceId: {} }}
                    newQuotas={{ amountByResourceId: {} }}
                    quotasDelta={{ amountByResourceId: {} }}
                />,
            );
            expect(wrapper).toMatchSnapshot();
        });

        it('only quota, same', () => {
            const wrapper = render(
                <HistoryFolderResourcesTable
                    resourcesById={resourcesById}
                    oldBalance={{ amountByResourceId: {} }}
                    newBalance={{ amountByResourceId: {} }}
                    balanceDelta={{ amountByResourceId: {} }}
                    oldQuotas={{
                        amountByResourceId: {
                            [resourceDto_YDB_RAM_SAS.id]: amount('1000.000', 'GiB'),
                        },
                    }}
                    newQuotas={{
                        amountByResourceId: {
                            [resourceDto_YDB_RAM_SAS.id]: amount('1000.000', 'GiB'),
                        },
                    }}
                    quotasDelta={{
                        amountByResourceId: {
                            [resourceDto_YDB_RAM_SAS.id]: amount('0', 'GiB'),
                        },
                    }}
                />,
            );
            expect(wrapper).toMatchSnapshot();
        });

        it('only balance, same', () => {
            const wrapper = render(
                <HistoryFolderResourcesTable
                    resourcesById={resourcesById}
                    oldBalance={{
                        amountByResourceId: {
                            [resourceDto_YDB_RAM_SAS.id]: amount('1000.000', 'GiB'),
                        },
                    }}
                    newBalance={{
                        amountByResourceId: {
                            [resourceDto_YDB_RAM_SAS.id]: amount('1000.000', 'GiB'),
                        },
                    }}
                    balanceDelta={{
                        amountByResourceId: {
                            [resourceDto_YDB_RAM_SAS.id]: amount('0', 'GiB'),
                        },
                    }}
                    oldQuotas={{ amountByResourceId: {} }}
                    newQuotas={{ amountByResourceId: {} }}
                    quotasDelta={{ amountByResourceId: {} }}
                />,
            );
            expect(wrapper).toMatchSnapshot();
        });

        it('no data', () => {
            const wrapper = render(
                <HistoryFolderResourcesTable
                    resourcesById={resourcesById}
                    oldBalance={{ amountByResourceId: {} }}
                    newBalance={{ amountByResourceId: {} }}
                    balanceDelta={{ amountByResourceId: {} }}
                    oldQuotas={{ amountByResourceId: {} }}
                    newQuotas={{ amountByResourceId: {} }}
                    quotasDelta={{ amountByResourceId: {} }}
                />,
            );
            expect(wrapper).toMatchSnapshot();
        });
    });
});
