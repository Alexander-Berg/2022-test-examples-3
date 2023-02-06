import {
    getExpandedResourceMock as getRandomExpandedResourceMock,
} from '~/src/features/Dispenser/Dispenser.features/Folders/Folders.mock';

import {
    ExpandedFolder,
    ExpandedProvider,
    ExpandedResource,
    ExpandedResourceType,
    FolderDto,
    FoldersResponse,
    FolderType,
    ProviderDto,
    ResourceDto,
    ResourceSegmentationSegmentDto,
    ResourceTypeDto,
} from '~/src/features/Dispenser/Dispenser.features/Folders';

export const getResourceTypeDtoMock = (data?: Partial<ResourceTypeDto>): ResourceTypeDto => ({
    baseUnit: '',
    description: '',
    ensemble: '',
    id: '',
    key: '',
    name: '',
    providerId: '',
    ...data,
});

export const getResourceSegmentationSegmentDtoMock = (
    data?: Partial<ResourceSegmentationSegmentDto>,
): ResourceSegmentationSegmentDto => ({
    groupingOrder: undefined,
    segmentId: '',
    segmentName: '',
    segmentationId: '',
    segmentationName: '',
    ...data,
});

export const getResourceDtoMock = (data?: Partial<ResourceDto>): ResourceDto => ({
    accountsSpacesId: '',
    defaultUnit: '',
    defaultUnitId: '',
    deleted: false,
    displayName: '',
    id: '',
    managed: true,
    providerId: '',
    readOnly: false,
    resourceSegments: (data?.resourceSegments ?? []).map(segment => getResourceSegmentationSegmentDtoMock(segment)),
    resourceTypeId: '',
    resourceUnits: { allowedUnitIds: [], defaultUnitId: '' },
    specification: '',
    unitsEnsembleId: '',
    ...data,
});

export const getProviderDtoMock = (data?: Partial<ProviderDto>): ProviderDto => ({
    deleted: false,
    description: '',
    id: '',
    key: '',
    managed: true,
    meteringKey: '',
    name: '',
    readOnly: false,
    reserveFolderId: '',
    resourceTypeGroupingOrder: undefined,
    ...data,
});

export const getFolderDtoMock = (data?: Partial<FolderDto>): FolderDto => ({
    deleted: false,
    description: '',
    displayName: '',
    folderType: FolderType.COMMON_DEFAULT_FOR_SERVICE,
    id: '',
    serviceId: 0,
    tags: [],
    version: 0,
    ...data,
});

export const getExpandedResourceMock = (data?: Partial<ExpandedResource>): ExpandedResource => ({
    ...getRandomExpandedResourceMock(),
    resourceId: '',
    ...data,
});

export const getExpandedResourceTypeMock = (data?: Partial<ExpandedResourceType>): ExpandedResourceType => ({
    resourceTypeId: '',
    resources: (data?.resources ?? []).map(resource => getExpandedResourceMock(resource)),
    sums: getRandomExpandedResourceMock(),
    ...data,
});

export const getExpandedProviderMock = (data?: Partial<ExpandedProvider>): ExpandedProvider => ({
    accounts: [],
    providerId: '',
    permissions: [],
    resourceTypes: (data?.resourceTypes ?? []).map(resourceType => getExpandedResourceTypeMock(resourceType)),
    ...data,
});

export const getExpandedFolderMock = (data?: Partial<ExpandedFolder>): ExpandedFolder => ({
    folder: getFolderDtoMock(data?.folder),
    permissions: [],
    providers: (data?.providers ?? []).map(provider => getExpandedProviderMock(provider)),
    ...data,
});

export const getFoldersResponseMock = (data?: Partial<FoldersResponse>): FoldersResponse => ({
    accountsSpaces: [],
    continuationToken: null,
    folders: (data?.folders ?? []).map(folder => getExpandedFolderMock(folder)),
    providers: (data?.providers ?? []).map(provider => getProviderDtoMock(provider)),
    resourceTypes: (data?.resourceTypes ?? []).map(resourceType => getResourceTypeDtoMock(resourceType)),
    resources: (data?.resources ?? []).map(resource => getResourceDtoMock(resource)),
    ...data,
});
