import React, { ReactNode } from 'react';
import { render } from 'enzyme';

import { ProxyContext } from '~/src/features/Dispenser/proxy-context';

import { FoldersView, IFolders } from './FolderTable.lib';
import { FolderTable } from './FolderTable';

const withContext = (Component: ReactNode) => (
    <ProxyContext.Provider
        // @ts-ignore тут не нужен полный шейп контекста
        value={{
            slug: 'd',
            configs: {
                hosts: {
                    datalens: {
                        protocol: 'https:',
                        hostname: 'datalens.yandex-team.ru',
                    },
                },
            },
        }}
    >
        {Component}
    </ProxyContext.Provider>
);

describe('FolderTable', () => {
    const folders: IFolders = [{
        id: 'folder1',
        name: 'Folder 1',

        providers: [{
            id: 'provider1',
            name: 'Provider 1',
            meteringKey: 'provider1',
            readOnly: false,
            managed: true,

            resources: [{
                id: 'resource1',
                name: 'Resource 1',
                readOnly: false,
                managed: true,

                balance: { value: '100', unit: 'GB', label: '100 GB' },
                negativeBalance: { value: '-10', unit: 'GB', label: '-10 GB' },
                quota: { value: '200', unit: 'GB', label: '200 GB' },
                frozenQuota: { value: '0', unit: 'GB', label: '0 GB' },
                provided: { value: '100', unit: 'GB', label: '100 GB' },
                allocated: { value: '50', unit: 'GB', label: '50 GB' },
                providedRatio: 1,
                allocatedRatio: 0.5,

                resources: [{
                    id: 'resource1.1',
                    name: 'Resource 1.1',
                    readOnly: false,
                    managed: true,

                    balance: { value: '100', unit: 'GB', label: '100 GB' },
                    negativeBalance: { value: '-1', unit: 'GB', label: '-1 GB' },
                    quota: { value: '200', unit: 'GB', label: '200 GB' },
                    frozenQuota: { value: '0', unit: 'GB', label: '0 GB' },
                    provided: { value: '100', unit: 'GB', label: '100 GB' },
                    allocated: { value: '50', unit: 'GB', label: '50 GB' },
                    providedRatio: 1,
                    allocatedRatio: 0.5,
                }],
            }],
        }],
    }, {
        id: 'folder2',
        name: 'Folder 2',

        providers: [{
            id: 'provider2',
            name: 'Provider 2',
            meteringKey: 'provider2',
            readOnly: false,
            managed: true,

            resources: [{
                id: 'resource2',
                name: 'Resource 2',
                readOnly: false,
                managed: true,

                balance: { value: '100', unit: 'cores', label: '100 cores' },
                negativeBalance: { value: '0', unit: 'cores', label: '0 cores' },
                quota: { value: '200', unit: 'cores', label: '200 cores' },
                frozenQuota: { value: '0', unit: 'cores', label: '0 cores' },
                provided: { value: '10', unit: 'cores', label: '10 cores' },
                allocated: { value: '50', unit: 'cores', label: '50 cores' },
                providedRatio: 1,
                allocatedRatio: 0.5,

                resources: [{
                    id: 'resource2.1',
                    name: 'Resource 2.1',
                    readOnly: false,
                    managed: true,

                    balance: { value: '100', unit: 'cores', label: '100 cores' },
                    negativeBalance: { value: '0', unit: 'cores', label: '0 cores' },
                    quota: { value: '200', unit: 'cores', label: '200 cores' },
                    frozenQuota: { value: '0', unit: 'cores', label: '0 cores' },
                    provided: { value: '10', unit: 'cores', label: '10 cores' },
                    allocated: { value: '50', unit: 'cores', label: '50 cores' },
                    providedRatio: 1,
                    allocatedRatio: 0.5,
                }],
            }, {
                id: 'resource3',
                name: 'Resource 3',
                readOnly: false,
                managed: true,

                balance: { value: '100', unit: 'units', label: '100 units' },
                negativeBalance: { value: '0', unit: 'units', label: '0 units' },
                quota: { value: '200', unit: 'units', label: '200 units' },
                frozenQuota: { value: '0', unit: 'units', label: '0 units' },
                provided: { value: '10', unit: 'units', label: '10 units' },
                allocated: { value: '50', unit: 'units', label: '50 units' },
                providedRatio: 1,
                allocatedRatio: 0.5,

                resources: [{
                    id: 'resource3.1',
                    name: 'Resource 3.1',
                    readOnly: false,
                    managed: true,

                    balance: { value: '100', unit: 'units', label: '100 units' },
                    negativeBalance: { value: '0', unit: 'units', label: '0 units' },
                    quota: { value: '200', unit: 'units', label: '200 units' },
                    frozenQuota: { value: '0', unit: 'units', label: '0 units' },
                    provided: { value: '10', unit: 'units', label: '10 units' },
                    allocated: { value: '50', unit: 'units', label: '50 units' },
                    providedRatio: 1,
                    allocatedRatio: 0.5,
                }],
            }],
        }, {
            id: 'provider3',
            name: 'Provider 3',
            meteringKey: 'provider3',
            readOnly: false,
            managed: true,

            resources: [{
                id: 'resource4',
                name: 'Resource 4',
                readOnly: false,
                managed: true,

                balance: { value: '1000000', unit: '₽', label: '1 000 000 ₽' },
                negativeBalance: { value: '0', unit: '₽', label: '0 ₽' },
                quota: { value: '50000000', unit: '₽', label: '50 000 000 ₽' },
                frozenQuota: { value: '0', unit: '₽', label: '0 ₽' },
                provided: { value: '100000', unit: '₽', label: '100 000 ₽' },
                allocated: { value: '50000', unit: '₽', label: '50 000 ₽' },
                providedRatio: 1,
                allocatedRatio: 0.5,

                resources: [{
                    id: 'resource4.1',
                    name: 'Resource 4.1',
                    readOnly: false,
                    managed: true,

                    balance: { value: '1', unit: '₽', label: '1 ₽' },
                    negativeBalance: { value: '0', unit: '₽', label: '0 ₽' },
                    quota: { value: '30000000', unit: '₽', label: '30 000 000 ₽' },
                    frozenQuota: { value: '0', unit: '₽', label: '0 ₽' },
                    provided: { value: '50000', unit: '₽', label: '50 000 ₽' },
                    allocated: { value: '25000', unit: '₽', label: '25 000 ₽' },
                    providedRatio: 1,
                    allocatedRatio: 0.5,
                }, {
                    id: 'resource4.2',
                    name: 'Resource 4.2',
                    readOnly: false,
                    managed: true,

                    balance: { value: '999999', unit: '₽', label: '999 999 ₽' },
                    negativeBalance: { value: '0', unit: '₽', label: '0 ₽' },
                    quota: { value: '20000000', unit: '₽', label: '20 000 000 ₽' },
                    frozenQuota: { value: '0', unit: '₽', label: '0 ₽' },
                    provided: { value: '50000', unit: '₽', label: '50 000 ₽' },
                    allocated: { value: '25000', unit: '₽', label: '25 000 ₽' },
                    providedRatio: 1,
                    allocatedRatio: 0.5,
                }],
            }],
        }],
    }, {
        id: 'folder3',
        name: 'Folder 3',

        providers: [],
    }, {
        id: 'folder4',
        name: 'Folder 4',

        providers: [{
            id: 'provider4',
            name: 'Provider 4',
            meteringKey: 'provider4',
            readOnly: false,
            managed: true,

            resources: [{
                id: 'resource5',
                name: 'Resource 5',
                readOnly: false,
                managed: true,

                balance: { value: '-10', unit: 'cores', label: '-10 cores' },
                negativeBalance: { value: '0', unit: 'cores', label: '0 cores' },
                quota: { value: '20', unit: 'cores', label: '20 cores' },
                frozenQuota: { value: '1', unit: 'cores', label: '1 cores' },
                provided: { value: '30', unit: 'cores', label: '30 cores' },
                allocated: { value: '5', unit: 'cores', label: '5 cores' },
                providedRatio: 1,
                allocatedRatio: 0.5,
            }],
        }],
    }];

    const restProps = {
        collapsedFolders: folders.reduce<FoldersView>((fAcc, { id: fId, providers }) => Object.assign(fAcc, {
            [fId]: {
                isCollapsed: false,
                providers: providers.reduce((pAcc, { id: pId }) => Object.assign(pAcc, { [pId]: false }), Object(null)),
            }
        }), Object(null)),
        dispatchCollapseFolders: jest.fn(),
    };

    it('Должна отрендерится таблица', () => {
        expect(render(withContext(
            <FolderTable
                data={folders}
                {...restProps}
            />,
        ))).toMatchSnapshot();
    });

    it('Должна отрендерится таблица с кнопками', () => {
        expect(render(withContext(
            <FolderTable
                folderContainerProps={{
                    tabIndex: 0,
                }}
                folderProps={{
                    type: 'button',
                }}
                providerProps={{
                    type: 'button',
                }}
                rootResourceContainerProps={{
                    tabIndex: 0,
                }}
                rootResourceProps={{
                    type: 'button',
                }}
                rootBalanceProps={{
                    type: 'button',
                }}
                rootQuotaProps={{
                    type: 'button',
                }}
                rootProvidedProps={{
                    type: 'button',
                }}
                rootAllocatedProps={{
                    type: 'button',
                }}
                resourceContainerProps={{
                    tabIndex: 0,
                }}
                resourceProps={{
                    type: 'button',
                }}
                balanceProps={{
                    type: 'button',
                }}
                quotaProps={{
                    type: 'button',
                }}
                providedProps={{
                    type: 'button',
                }}
                allocatedProps={{
                    type: 'button',
                }}

                data={folders}
                {...restProps}
            />,
        ))).toMatchSnapshot();
    });
});
