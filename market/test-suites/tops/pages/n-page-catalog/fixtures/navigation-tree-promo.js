export default {
    entity: 'navnode',
    hasPromo: false,
    id: 54415,
    slug: 'vse-tovary',
    isLeaf: false,
    link: {
        params: {
            hid: [
                '90401',
            ],
            nid: [
                '54415',
            ],
        },
        target: 'catalog',
    },
    name: 'Все товары',
    navnodes: [
        {
            entity: 'navnode',
            isLeaf: false,
            link: {
                params: {
                    hid: [
                        '198119',
                    ],
                    nid: [
                        '54440',
                    ],
                },
                target: 'department',
            },
            id: 54440,
            slug: 'elektronika',
            name: 'Первый узел',
            navnodes: [
                {
                    entity: 'navnode',
                    id: 54437,
                    isLeaf: false,
                    link: {
                        params: {
                            hid: [
                                '91461',
                            ],
                            nid: [
                                '54437',
                            ],
                        },
                        target: 'catalog',
                    },
                    name: 'Подкатегория 1',
                    slug: 'podkategoriya-1',
                    rootNavnode: {
                        entity: 'navnode',
                        id: 54440,
                    },
                },
                {
                    entity: 'navnode',
                    id: 54438,
                    isLeaf: false,
                    link: {
                        params: {
                            hid: [
                                '91461',
                            ],
                            nid: [
                                '54437',
                            ],
                        },
                        target: 'catalog',
                    },
                    name: 'Подкатегория 2',
                    slug: 'podkategoriya-2',
                    rootNavnode: {
                        entity: 'navnode',
                        id: 54440,
                    },
                },
                {
                    entity: 'navnode',
                    id: 54439,
                    isLeaf: false,
                    link: {
                        params: {
                            hid: [
                                '91461',
                            ],
                            nid: [
                                '54437',
                            ],
                        },
                        target: 'catalog',
                    },
                    name: 'Подкатегория 3',
                    slug: 'podkategoriya-3',
                    rootNavnode: {
                        entity: 'navnode',
                        id: 54440,
                    },
                },
            ],
            rootNavnode: {
                entity: 'navnode',
                id: 54415,
            },
            type: 'category',
        },
        {
            entity: 'navnode',
            hasPromo: true,
            id: 54425,
            isLeaf: false,
            link: {
                params: {
                    hid: [
                        '91009',
                    ],
                    nid: [
                        '54425',
                    ],
                },
                target: 'department',
            },
            name: 'Второй узел',
            slug: 'vtoroy-uzel',
            navnodes: [
                {
                    entity: 'navnode',
                    id: 54437,
                    isLeaf: false,
                    link: {
                        params: {
                            hid: [
                                '91461',
                            ],
                            nid: [
                                '54437',
                            ],
                        },
                        target: 'catalog',
                    },
                    name: 'Подкатегория 1',
                    slug: 'podkategoriya-1',
                    rootNavnode: {
                        entity: 'navnode',
                        id: 54425,
                    },
                },
                {
                    entity: 'navnode',
                    id: 54438,
                    isLeaf: false,
                    link: {
                        params: {
                            hid: [
                                '91461',
                            ],
                            nid: [
                                '54437',
                            ],
                        },
                        target: 'catalog',
                    },
                    name: 'Подкатегория 2',
                    slug: 'podkategoriya-2',
                    rootNavnode: {
                        entity: 'navnode',
                        id: 54425,
                    },
                },
                {
                    entity: 'navnode',
                    id: 54439,
                    isLeaf: false,
                    link: {
                        params: {
                            hid: [
                                '91461',
                            ],
                            nid: [
                                '54437',
                            ],
                        },
                        target: 'catalog',
                    },
                    name: 'Подкатегория 3',
                    slug: 'podkategoriya-1',
                    rootNavnode: {
                        entity: 'navnode',
                        id: 54425,
                    },
                },
            ],
            rootNavnode: {
                entity: 'navnode',
                id: 54415,
            },
            type: 'category',
        },
    ],
    type: 'category',
};
