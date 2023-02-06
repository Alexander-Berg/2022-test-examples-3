import { DictionariesStore } from '../redux/types';

// урезанный словарь кампании 100
export const campaignSettings: DictionariesStore = {
    CAMPAIGN_PROVIDERS_SETTINGS: {
        campaign: {
            id: 100,
            key: 'aug2020',
            name: 'Заказ, август 2020',
            status: 'ACTIVE',
            startDate: '2020-08-01',
            campaignBigOrders: [
                { id: 786, bigOrderDate: '2021-06-30', bigOrderId: 43 },
                { id: 787, bigOrderDate: '2021-09-30', bigOrderId: 44 },
                { id: 788, bigOrderDate: '2021-12-31', bigOrderId: 45 },
            ],
            requestCreationDisabled: false,
            requestModificationDisabledForNonManagers: false,
            botPreOrderIssueKey: 'DISPENSERREQ-4827',
            allowedRequestModificationWhenClosed: false,
            allowedModificationOnMissingAdditionalFields: false,
        },
        segmentations: [
            {
                id: 1,
                key: 'locations',
                name: 'Locations',
                segments: [
                    { id: 1, key: 'VLA', name: 'VLA', priority: 1 },
                    { id: 9, key: 'SAS', name: 'SAS', priority: 1 },
                    { id: 10, key: 'MAN', name: 'MAN', priority: 1 },
                    { id: 42, key: 'MYT', name: 'MYT', priority: 1 },
                ],
                priority: 0,
            },
            {
                id: 76,
                key: 'yp_segment',
                name: 'Сегмент YP',
                segments: [
                    { id: 132, key: 'dev', name: 'Dev', priority: 1 },
                    { id: 133, key: 'default', name: 'Default', priority: 1 },
                ],
                priority: 0,
            },
        ],
        providers: [
            {
                id: 4,
                key: 'nirvana',
                name: 'Nirvana',
                priority: 60,
                resources: [
                    {
                        id: 13,
                        key: 'cpu',
                        name: 'CPU',
                        type: 'PROCESSOR',
                        required: false,
                        default: true,
                        segmentations: [
                            { id: 1, segments: [9] },
                        ],
                        bigOrders: [786, 787, 788],
                        defaultUnit: 'CORES',
                        units: {
                            propertiesByUnitKey: {
                                CORES: {
                                    name: 'cores',
                                    localizationKey: 'nirvana.cpu.CORES',
                                },
                            },
                        },
                    },
                ],
                defaultResources: [
                    {
                        resourceId: 13,
                        segments: [
                            { segmentationId: 1, segmentId: 9 },
                        ],
                    },
                ],
            },
            {
                id: 58,
                key: 'sqs',
                name: 'SQS',
                priority: 130,
                resources: [
                    {
                        id: 304,
                        key: 'write_capacity',
                        name: 'Write throughput',
                        type: 'ENUMERABLE',
                        required: true,
                        default: true,
                        bigOrders: [786, 787, 788],
                        defaultUnit: 'COUNT',
                        units: {
                            propertiesByUnitKey: {
                                COUNT: {
                                    name: 'messages/second',
                                    localizationKey: 'sqs.write_capacity.COUNT',
                                },
                            },
                        },
                    },
                ],
                defaultResources: [
                    {
                        resourceId: 304,
                        segments: [],
                    },
                ],
            },
            {
                id: 17,
                key: 'yp',
                name: 'YP',
                priority: 180,
                resources: [
                    {
                        id: 385,
                        key: 'io_ssd',
                        name: 'IO SSD',
                        type: 'TRAFFIC',
                        required: false,
                        default: true,
                        segmentations: [
                            { id: 1, segments: [1, 9, 10] },
                            { id: 76, segments: [132, 133] },
                        ],
                        bigOrders: [786, 787, 788],
                        defaultUnit: 'MBPS',
                        units: {
                            propertiesByUnitKey: {
                                MBPS: {
                                    name: 'MB/s',
                                    localizationKey: 'yp.io_ssd.MBPS',
                                },
                            },
                        },
                    },
                    {
                        id: 387,
                        key: 'cpu_segmented',
                        name: 'CPU',
                        type: 'PROCESSOR',
                        required: false,
                        default: true,
                        segmentations: [
                            { id: 1, segments: [1, 9, 10] },
                            { id: 76, segments: [132, 133] },
                        ],
                        bigOrders: [786, 787, 788],
                        defaultUnit: 'CORES',
                        units: {
                            propertiesByUnitKey: {
                                CORES: {
                                    name: 'cores',
                                    localizationKey: 'yp.cpu_segmented.CORES',
                                },
                            },
                        },
                    },
                    {
                        id: 389,
                        key: 'hdd_segmented',
                        name: 'HDD',
                        type: 'STORAGE',
                        required: false,
                        default: true,
                        segmentations: [
                            { id: 1, segments: [1, 9, 10] },
                            { id: 76, segments: [132, 133] },
                        ],
                        bigOrders: [786, 787, 788],
                        defaultUnit: 'TEBIBYTE',
                        units: {
                            propertiesByUnitKey: {
                                GIBIBYTE: {
                                    name: 'GiB',
                                    localizationKey: 'yp.hdd_segmented.GIBIBYTE',
                                },
                                MEBIBYTE: {
                                    name: 'MiB',
                                    localizationKey: 'yp.hdd_segmented.MEBIBYTE',
                                },
                                TEBIBYTE: {
                                    name: 'TiB',
                                    localizationKey: 'yp.hdd_segmented.TEBIBYTE',
                                },
                            },
                        },
                    },
                ],
                defaultResources: [
                    {
                        resourceId: 387,
                        segments: [
                            { segmentationId: 1, segmentId: 9 },
                            { segmentationId: 76, segmentId: 132 },
                        ],
                    },
                    {
                        resourceId: 389,
                        segments: [
                            { segmentationId: 1, segmentId: 1 },
                            { segmentationId: 76, segmentId: 133 },
                        ],
                    },
                    {
                        resourceId: 385,
                        segments: [],
                    },
                ],
            },
        ],
    },
};
