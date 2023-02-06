import { ResourcesCountsResponse } from '~/src/features/ResourcesCounter/redux/types/requests';

export const getCounterResponse = (): ResourcesCountsResponse => ({
    next: null,
    previous: null,
    results: [
        {
            id: 2,
            service_id: 753,
            resource_type: {
                id: 19,
                name: 'project',
                supplier: {
                    id: 391,
                    slug: 'admintools',
                    name: {
                        ru: 'Кондуктор',
                        en: 'Кондуктор',
                    },
                    parent: 1172,
                },
            },
            count: 0,
        },
        {
            id: 221,
            service_id: 753,
            resource_type: {
                id: 20,
                name: 'group',
                supplier: {
                    id: 391,
                    slug: 'admintools',
                    name: {
                        ru: 'Кондуктор',
                        en: 'Кондуктор',
                    },
                    parent: 1172,
                },
            },
            count: 2,
        },
        {
            id: 381,
            service_id: 753,
            resource_type: {
                id: 21,
                name: 'host',
                supplier: {
                    id: 392,
                    slug: 'admintools-2',
                    name: {
                        ru: 'Кондуктор-2',
                        en: 'Кондуктор-2',
                    },
                    parent: 1172,
                },
            },
            count: 16384,
        },
    ],
});

export const getBotCounterResponse = (): ResourcesCountsResponse => ({
    next: null,
    previous: null,
    results: [
        {
            id: 2000,
            service_id: 842,
            resource_type: {
                id: 9,
                name: 'SRV.EXPANSIONCARDS',
                supplier: {
                    id: 385,
                    slug: 'bot',
                    name: {
                        ru: 'BOT',
                        en: 'BOT',
                    },
                    parent: 742,
                },
            },
            count: 3,
        },
        {
            id: 2237,
            service_id: 842,
            resource_type: {
                id: 6,
                name: 'SRV.DISKDRIVES',
                supplier: {
                    id: 385,
                    slug: 'bot',
                    name: {
                        ru: 'BOT',
                        en: 'BOT',
                    },
                    parent: 742,
                },
            },
            count: 6,
        },
        {
            id: 3806,
            service_id: 842,
            resource_type: {
                id: 132,
                name: 'SRV.CPU',
                supplier: {
                    id: 385,
                    slug: 'bot',
                    name: {
                        ru: 'BOT',
                        en: 'BOT',
                    },
                    parent: 742,
                },
            },
            count: 5,
        },
        {
            id: 3938,
            service_id: 842,
            resource_type: {
                id: 131,
                name: 'SRV.BACKPLANES',
                supplier: {
                    id: 385,
                    slug: 'bot',
                    name: {
                        ru: 'BOT',
                        en: 'BOT',
                    },
                    parent: 742,
                },
            },
            count: 6,
        },
        {
            id: 4189,
            service_id: 842,
            resource_type: {
                id: 1,
                name: 'SRV.NODES',
                supplier: {
                    id: 385,
                    slug: 'bot',
                    name: {
                        ru: 'BOT',
                        en: 'BOT',
                    },
                    parent: 742,
                },
            },
            count: 3,
        },
    ],
});
