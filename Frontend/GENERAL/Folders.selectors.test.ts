import { mockStoreData } from '../../../test-data/storeData';
import { mockFolders } from '../Folders.mock';
import {
    getExpandedFolderMock,
    getExpandedProviderMock,
    getExpandedResourceMock,
    getExpandedResourceTypeMock,
    getFolderDtoMock,
    getFoldersResponseMock,
    getProviderDtoMock,
    getResourceDtoMock,
    getResourceSegmentationSegmentDtoMock,
    getResourceTypeDtoMock,
} from '~/test/jest/mocks/data/dispenser/folders';
import { Store } from '../../../store';
import {
    IFolderTableRootResource as IRootResource,
    IFolderTableFolders as IFolders,
} from '../../components/FolderTable';
import {
    selectAccountResource,
    selectAccountsData,
    selectAvailableResources,
    selectAvailableResourcesGrouped,
    selectFolderResources,
    selectFoldersResources,
    selectProviderAccounts,
    selectProviderResources,
    selectTableFolders,
} from './Folders.selectors';
import {
    IFullProviderAccounts,
    FullAccountResource,
} from '../Folders.lib';

const testService = 100500;
const noService = 100501;

const state: Store = {
    ...mockStoreData,
    folders: {
        [testService]: mockFolders,
    },
};

describe('Folder selectors', () => {
    it('Should return undefined on missed data', () => {
        const folders = selectTableFolders(state, { serviceId: noService });
        expect(folders).toBeUndefined();

        const accounts = selectAccountsData(state, { serviceId: noService });
        expect(accounts.folder).toBeUndefined();
        expect(accounts.provider).toBeUndefined();

        const foldersResources = selectFoldersResources(state, {
            serviceId: noService,
        });
        expect(foldersResources).toBeUndefined();

        const folderResources = selectFolderResources(state, {
            serviceId: noService,
            folderId: 'not exists',
        });
        expect(folderResources).toBeUndefined();

        const providerResources = selectProviderResources(state, {
            serviceId: noService,
            folderId: 'not exists',
            providerId: 'not exists',
        });
        expect(providerResources).toBeUndefined();
    });

    it('Should select account resource', () => {
        const resource = selectAccountResource(state, {
            serviceId: testService,
            account: {
                id: 'special-accountid',
            },
            folderId: '11d1bcdb-3edc-4c21-8a79-4570e3c09c21',
            providerId: '1437b48c-b2d6-4ba5-84db-5cb1f20f6533',
            resourceId: '995de13e-a417-470e-a642-ebd012e69003',
        }) as FullAccountResource;

        expect(resource).toBeDefined();

        expect(resource.balance.forEditAmount).toBe('100');
        expect(resource.provided.forEditAmount).toBe('50');
    });

    it('Should select provider accounts', () => {
        const providerAccounts = selectProviderAccounts(state, {
            serviceId: testService,
            folderId: '11d1bcdb-3edc-4c21-8a79-4570e3c09c21',
            providerId: '1437b48c-b2d6-4ba5-84db-5cb1f20f6533',
        }) as IFullProviderAccounts;

        expect(providerAccounts.accounts).toBeDefined();
        expect(providerAccounts.accounts['special-accountid']).toBeDefined();
        expect(providerAccounts.accounts['special-accountid'].resources).toHaveLength(1);
    });

    it('Should select empty account resource on non provided', () => {
        const resource = selectAccountResource(state, {
            serviceId: testService,
            account: {
                id: 'special-accountid2',
            },
            folderId: '11d1bcdb-3edc-4c21-8a79-4570e3c09c21',
            providerId: '96e779cf-7d3f-4e74-ba41-c2acc7f04235',
            resourceId: '71aa2e62-d26e-4f53-b581-29c7610b300f',
        }) as FullAccountResource;

        expect(resource).toBeDefined();

        expect(resource.balance.forEditAmount).toBe('100');
        expect(resource.provided.forEditAmount).toBe('0');
    });

    it('Should select available provider resources for account', () => {
        const availableResources = selectAvailableResources(state, {
            serviceId: testService,
            account: {
                id: 'special-accountid',
                accountsSpacesId: 'special-account-space-id',
                displayName: 'special account',
                folderId: '11d1bcdb-3edc-4c21-8a79-4570e3c09c21',
                deleted: false,
            },
            folderId: '11d1bcdb-3edc-4c21-8a79-4570e3c09c21',
            providerId: '1437b48c-b2d6-4ba5-84db-5cb1f20f6533',
        });

        expect(availableResources?.['995de13e-a417-470e-a642-ebd012e69003']).toBeDefined();
    });

    it('Should select folders for service', () => {
        // каст `as IFolders` - для исключения возможного null
        const folders = selectTableFolders(state, { serviceId: testService }) as IFolders;
        expect(folders).not.toBeNull();
        expect(folders).toHaveLength(2);

        const folder = folders[0];
        expect(folder.id).toBe('11d1bcdb-3edc-4c21-8a79-4570e3c09c21');
        expect(folder.name).toBe('Проверочная папка');
        expect(folder.providers).toHaveLength(2);

        const providerYDB = folder.providers[0];
        expect(providerYDB.id).toBe('1437b48c-b2d6-4ba5-84db-5cb1f20f6533');
        expect(providerYDB.name).toBe('YDB');
        expect(providerYDB.meteringKey).toBe('ydb');

        expect(providerYDB.resources).toHaveLength(1);

        const resourceTypeRam = providerYDB.resources[0];
        expect(resourceTypeRam.id).toBe('8908d0f9-e05d-47b6-bbf9-6f1cdb34b17c');
        expect(resourceTypeRam.name).toBe('RAM');
        expect(resourceTypeRam.balance.label).toBe('−2,000,022 B');

        expect((resourceTypeRam as IRootResource).resources).toBeDefined();
        expect((resourceTypeRam as IRootResource).resources).toHaveLength(1);

        const resourceYDB = (resourceTypeRam as IRootResource).resources?.[0];
        expect(resourceYDB?.id).toBe('995de13e-a417-470e-a642-ebd012e69003');
        expect(resourceYDB?.name).toBe('YDB-RAM-SAS');
        expect(resourceYDB?.provided.label).toBe('102,000,024 B');

        const providerYP = folder.providers[1];
        expect(providerYP.id).toBe('96e779cf-7d3f-4e74-ba41-c2acc7f04235');
        expect(providerYP.name).toBe('YP');
        expect(providerYP.meteringKey).toBe('yp');
        expect(providerYP.resources).toHaveLength(2);

        const resourceTypeHDD = providerYP.resources[0];
        expect(resourceTypeHDD.name).toBe('HDD');
        expect(resourceTypeHDD.id).toBe('44f93060-e367-44e6-b069-98c20d03dd81');
        expect(resourceTypeHDD.quota.value).toBe('2004');

        expect((resourceTypeHDD as IRootResource).resources).toBeDefined();
        expect((resourceTypeHDD as IRootResource).resources).toHaveLength(2);

        const resourceYPTrafficGroup = providerYP.resources[1] as IRootResource;
        expect(resourceYPTrafficGroup?.id).toBe('44f93060-e367-44e6-b069-98c20d03dd82');
        expect(resourceYPTrafficGroup?.name).toBe('NET');

        const resourceYPTraffic = resourceYPTrafficGroup.resources[0];
        expect(resourceYPTraffic?.id).toBe('f81e3bdb-210c-497a-ab43-d22657e16526');
        expect(resourceYPTraffic?.name).toBe('YP-TRAFFIC');

        expect((resourceYPTraffic as IRootResource)?.resources).toBeUndefined();

        expect(folders).toMatchSnapshot();
    });

    describe('selectAvailableResourcesGrouped', () => {
        const selectProps = {
            serviceId: testService,
            folderId: 'folder1',
            providerId: 'provider1',
            account: {
                id: '',
                accountsSpacesId: 'accountSpace1',
                displayName: '',
                folderId: '',
                deleted: false,
            },
        };

        describe('Should group resources by type and segments', () => {
            const mock = getFoldersResponseMock({
                resourceTypes: [
                    getResourceTypeDtoMock({
                        providerId: 'provider1',
                        id: 'resourceType1',
                        name: 'Type with order',
                    }),
                ],
                providers: [
                    getProviderDtoMock({
                        id: 'provider1',
                        resourceTypeGroupingOrder: 1,
                    }),
                ],
                resources: [
                    getResourceDtoMock({
                        id: 'resource1',
                        displayName: 'Resource 1',
                        providerId: 'provider1',
                        resourceSegments: [
                            getResourceSegmentationSegmentDtoMock({
                                groupingOrder: 1,
                                segmentId: 'segment1',
                                segmentName: 'Segment 1',
                            }),
                        ],
                        resourceTypeId: 'resourceType1',
                        accountsSpacesId: 'accountSpace1',
                    }),
                    getResourceDtoMock({
                        id: 'resource2',
                        displayName: 'Resource 2',
                        providerId: 'provider1',
                        resourceSegments: [
                            getResourceSegmentationSegmentDtoMock({
                                groupingOrder: 0,
                                segmentId: 'segment2',
                                segmentName: 'Segment 2',
                            }),
                        ],
                        resourceTypeId: 'resourceType1',
                        accountsSpacesId: 'accountSpace1',
                    }),
                    getResourceDtoMock({
                        id: 'resource3',
                        displayName: 'Resource 3',
                        providerId: 'provider1',
                        resourceSegments: [
                            getResourceSegmentationSegmentDtoMock({
                                groupingOrder: 0,
                                segmentId: 'segment2',
                                segmentName: 'Segment 2',
                            }),
                        ],
                        resourceTypeId: 'resourceType1',
                        accountsSpacesId: 'accountSpace1',
                    }),
                    getResourceDtoMock({
                        id: 'resource4',
                        displayName: 'Resource 4',
                        providerId: 'provider1',
                        resourceSegments: [
                            getResourceSegmentationSegmentDtoMock({
                                groupingOrder: 2,
                                segmentId: 'segment3',
                                segmentName: 'Segment 3',
                            }),
                        ],
                        resourceTypeId: 'resourceType1',
                        accountsSpacesId: 'accountSpace1',
                    }),
                ],
                folders: [
                    getExpandedFolderMock({
                        folder: getFolderDtoMock({ id: 'folder1' }),
                        providers: [
                            getExpandedProviderMock({
                                providerId: 'provider1',
                                resourceTypes: [
                                    getExpandedResourceTypeMock({
                                        resourceTypeId: 'resourceType1',
                                        resources: [
                                            getExpandedResourceMock({ resourceId: 'resource1' }),
                                            getExpandedResourceMock({ resourceId: 'resource2' }),
                                            getExpandedResourceMock({ resourceId: 'resource3' }),
                                            getExpandedResourceMock({ resourceId: 'resource4' }),
                                        ],
                                    }),
                                ],
                            }),
                        ],
                    }),
                ],
            });

            const actual = selectAvailableResourcesGrouped({
                ...state,
                folders: {
                    [testService]: mock,
                },
            }, selectProps);

            it('Should group resources with equal type and segmentation together', () => {
                expect(actual?.length).toBe(3);
                expect(actual?.[0].resources.length).toBe(1);
                expect(actual?.[0].resources.map(r => r.resourceId)).toStrictEqual(['resource1']);
                expect(actual?.[1].resources.length).toBe(2);
                expect(actual?.[1].resources.map(r => r.resourceId)).toStrictEqual(['resource2', 'resource3']);
                expect(actual?.[2].resources.length).toBe(1);
                expect(actual?.[2].resources.map(r => r.resourceId)).toStrictEqual(['resource4']);
            });

            it('Should sort types and segments with equal groupingOrder in one group alphabetically', () => {
                expect(actual?.[0].title).toBe('Segment 1 — Type with order');
            });

            it('Should sort types and segments in one group according their groupingOrder', () => {
                expect(actual?.[1].title).toBe('Segment 2 — Type with order');
                expect(actual?.[2].title).toBe('Type with order — Segment 3');
            });
        });

        it('Should sort groups alphabetically', () => {
            const mock = getFoldersResponseMock({
                resources: [
                    getResourceDtoMock({
                        id: 'resource1',
                        displayName: 'Resource 1',
                        providerId: '',
                        resourceSegments: [
                            getResourceSegmentationSegmentDtoMock({
                                groupingOrder: 0,
                                segmentId: 'segment1',
                                segmentName: 'ZYX',
                            }),
                        ],
                        resourceTypeId: 'resourceType1',
                        accountsSpacesId: 'accountSpace1',
                    }),
                    getResourceDtoMock({
                        id: 'resource2',
                        displayName: 'Resource 2',
                        providerId: '',
                        resourceSegments: [
                            getResourceSegmentationSegmentDtoMock({
                                groupingOrder: 0,
                                segmentId: 'segment2',
                                segmentName: 'ABC',
                            }),
                        ],
                        resourceTypeId: 'resourceType1',
                        accountsSpacesId: 'accountSpace1',
                    }),
                ],
                folders: [
                    getExpandedFolderMock({
                        folder: getFolderDtoMock({ id: 'folder1' }),
                        providers: [
                            getExpandedProviderMock({
                                providerId: 'provider1',
                                resourceTypes: [
                                    getExpandedResourceTypeMock({
                                        resourceTypeId: 'resourceType1',
                                        resources: [
                                            getExpandedResourceMock({ resourceId: 'resource1' }),
                                            getExpandedResourceMock({ resourceId: 'resource2' }),
                                        ],
                                    }),
                                ],
                            }),
                        ],
                    }),
                ],
            });

            const actual = selectAvailableResourcesGrouped({
                ...state,
                folders: {
                    [testService]: mock,
                },
            }, selectProps);

            expect(actual?.[0].title).toBe('ABC');
            expect(actual?.[1].title).toBe('ZYX');
        });

        it('Should sort resources in a group alphabetically', () => {
            const mock = getFoldersResponseMock({
                resources: [
                    getResourceDtoMock({
                        id: 'resource1',
                        displayName: 'B',
                        providerId: '',
                        resourceSegments: [
                            getResourceSegmentationSegmentDtoMock({
                                groupingOrder: 0,
                                segmentId: 'segment1',
                                segmentName: 'Segment 1',
                            }),
                        ],
                        resourceTypeId: 'resourceType1',
                        accountsSpacesId: 'accountSpace1',
                    }),
                    getResourceDtoMock({
                        id: 'resource2',
                        displayName: 'C',
                        providerId: '',
                        resourceSegments: [
                            getResourceSegmentationSegmentDtoMock({
                                groupingOrder: 0,
                                segmentId: 'segment1',
                                segmentName: 'Segment 1',
                            }),
                        ],
                        resourceTypeId: 'resourceType1',
                        accountsSpacesId: 'accountSpace1',
                    }),
                    getResourceDtoMock({
                        id: 'resource3',
                        displayName: 'A',
                        providerId: '',
                        resourceSegments: [
                            getResourceSegmentationSegmentDtoMock({
                                groupingOrder: 0,
                                segmentId: 'segment1',
                                segmentName: 'Segment 1',
                            }),
                        ],
                        resourceTypeId: 'resourceType1',
                        accountsSpacesId: 'accountSpace1',
                    }),
                ],
                folders: [
                    getExpandedFolderMock({
                        folder: getFolderDtoMock({ id: 'folder1' }),
                        providers: [
                            getExpandedProviderMock({
                                providerId: 'provider1',
                                resourceTypes: [
                                    getExpandedResourceTypeMock({
                                        resourceTypeId: 'resourceType1',
                                        resources: [
                                            getExpandedResourceMock({ resourceId: 'resource1' }),
                                            getExpandedResourceMock({ resourceId: 'resource2' }),
                                            getExpandedResourceMock({ resourceId: 'resource3' }),
                                        ],
                                    }),
                                ],
                            }),
                        ],
                    }),
                ],
            });

            const actual = selectAvailableResourcesGrouped({
                ...state,
                folders: {
                    [testService]: mock,
                },
            }, selectProps);

            expect(actual?.[0].resources[0].resource.displayName).toBe('A');
            expect(actual?.[0].resources[1].resource.displayName).toBe('B');
            expect(actual?.[0].resources[2].resource.displayName).toBe('C');
        });

        it('Should exclude segments and types with groupingOrder == -1 from grouping', () => {
            const mock = getFoldersResponseMock({
                resourceTypes: [
                    getResourceTypeDtoMock({
                        providerId: 'providerWithOrder',
                        id: 'resourceTypeWithOrder',
                        name: 'Type with order',
                    }),
                    getResourceTypeDtoMock({
                        providerId: 'providerWithoutOrder',
                        id: 'resourceTypeWithoutOrder',
                        name: 'Type without order',
                    }),
                ],
                providers: [
                    getProviderDtoMock({
                        id: 'providerWithOrder',
                        resourceTypeGroupingOrder: 0,
                    }),
                    getProviderDtoMock({
                        id: 'providerWithoutOrder',
                        resourceTypeGroupingOrder: -1,
                    }),
                ],
                resources: [
                    getResourceDtoMock({
                        id: 'resource1',
                        displayName: 'Resource 1',
                        providerId: 'providerWithOrder',
                        resourceSegments: [
                            getResourceSegmentationSegmentDtoMock({
                                groupingOrder: -1,
                                segmentId: 'segmentWithoutOrder',
                                segmentName: 'Segment without order',
                            }),
                        ],
                        resourceTypeId: 'resourceTypeWithOrder',
                        accountsSpacesId: 'accountSpace1',
                    }),
                    getResourceDtoMock({
                        id: 'resource2',
                        displayName: 'Resource 2',
                        providerId: 'providerWithoutOrder',
                        resourceSegments: [
                            getResourceSegmentationSegmentDtoMock({
                                groupingOrder: 1,
                                segmentId: 'segmentWithOrder',
                                segmentName: 'Segment with order',
                            }),
                        ],
                        resourceTypeId: 'resourceTypeWithoutOrder',
                        accountsSpacesId: 'accountSpace1',
                    }),
                ],
                folders: [
                    getExpandedFolderMock({
                        folder: getFolderDtoMock({ id: 'folder1' }),
                        providers: [
                            getExpandedProviderMock({
                                providerId: 'providerWithOrder',
                                resourceTypes: [
                                    getExpandedResourceTypeMock({
                                        resourceTypeId: 'resourceTypeWithoutOrder',
                                        resources: [getExpandedResourceMock({ resourceId: 'resource1' })],
                                    }),
                                ],
                            }),
                            getExpandedProviderMock({
                                providerId: 'providerWithoutOrder',
                                resourceTypes: [
                                    getExpandedResourceTypeMock({
                                        resourceTypeId: 'resourceTypeWithOrder',
                                        resources: [getExpandedResourceMock({ resourceId: 'resource2' })],
                                    }),
                                ],
                            }),
                        ],
                    }),
                ],
            });

            const actualWithType = selectAvailableResourcesGrouped({
                ...state,
                folders: {
                    [testService]: mock,
                },
            }, {
                ...selectProps,
                providerId: 'providerWithOrder',
            });

            const actualWithSegment = selectAvailableResourcesGrouped({
                ...state,
                folders: {
                    [testService]: mock,
                },
            }, {
                ...selectProps,
                providerId: 'providerWithoutOrder',
            });

            expect(actualWithType?.[0].title).toBe('Type with order');
            expect(actualWithSegment?.[0].title).toBe('Segment with order');
        });

        it('Should interpret undefined grouping order as -1', () => {
            const mock = getFoldersResponseMock({
                resourceTypes: [
                    getResourceTypeDtoMock({
                        providerId: 'provider1',
                        id: 'resourceType1',
                        name: 'Type with order',
                    }),
                ],
                providers: [
                    getProviderDtoMock({
                        id: 'provider1',
                        resourceTypeGroupingOrder: 0,
                    }),
                ],
                resources: [
                    getResourceDtoMock({
                        id: 'resource1',
                        displayName: 'Resource 1',
                        providerId: 'provider1',
                        resourceSegments: [
                            getResourceSegmentationSegmentDtoMock({
                                segmentId: 'segment1',
                                segmentName: 'Segment without order',
                            }),
                        ],
                        resourceTypeId: 'resourceType1',
                        accountsSpacesId: 'accountSpace1',
                    }),
                ],
                folders: [
                    getExpandedFolderMock({
                        folder: getFolderDtoMock({ id: 'folder1' }),
                        providers: [
                            getExpandedProviderMock({
                                providerId: 'provider1',
                                resourceTypes: [
                                    getExpandedResourceTypeMock({
                                        resourceTypeId: 'resourceType1',
                                        resources: [getExpandedResourceMock({ resourceId: 'resource1' })],
                                    }),
                                ],
                            }),
                        ],
                    }),
                ],
            });

            const actual = selectAvailableResourcesGrouped({
                ...state,
                folders: {
                    [testService]: mock,
                },
            }, selectProps);

            expect(actual?.[0].title).toBe('Type with order');
        });

        it('Should substitute undefined names with id', () => {
            const mock = getFoldersResponseMock({
                resourceTypes: [
                    getResourceTypeDtoMock({
                        providerId: 'provider1',
                        id: 'resourceType1',
                        name: undefined,
                    }),
                ],
                providers: [
                    getProviderDtoMock({
                        id: 'provider1',
                        resourceTypeGroupingOrder: 0,
                    }),
                ],
                resources: [
                    getResourceDtoMock({
                        id: 'resource1',
                        displayName: undefined,
                        providerId: 'provider1',
                        resourceSegments: [
                            getResourceSegmentationSegmentDtoMock({
                                segmentId: 'segment1',
                                segmentName: undefined,
                                groupingOrder: 0,
                            }),
                        ],
                        resourceTypeId: 'resourceType1',
                        accountsSpacesId: 'accountSpace1',
                    }),
                ],
                folders: [
                    getExpandedFolderMock({
                        folder: getFolderDtoMock({ id: 'folder1' }),
                        providers: [
                            getExpandedProviderMock({
                                providerId: 'provider1',
                                resourceTypes: [
                                    getExpandedResourceTypeMock({
                                        resourceTypeId: 'resourceType1',
                                        resources: [getExpandedResourceMock({ resourceId: 'resource1' })],
                                    }),
                                ],
                            }),
                        ],
                    }),
                ],
            });

            const actual = selectAvailableResourcesGrouped({
                ...state,
                folders: {
                    [testService]: mock,
                },
            }, selectProps);

            expect(actual?.[0].title).toBe('resourceType1 — segment1');
        });
    });
});
