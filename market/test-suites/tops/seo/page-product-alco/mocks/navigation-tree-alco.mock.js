export default {
    category: {
        entity: 'category',
        fullName: 'Продукты, напитки',
        id: 91307,
        isLeaf: false,
        modelsCount: 53244,
        name: 'Продукты',
        nid: 54434,
        offersCount: 298532,
        type: 'gurulight',
        viewType: 'list',
    },
    childrenType: 'mixed',
    entity: 'navnode',
    fullName: 'Продукты, напитки',
    hasPromo: false,
    id: 54434,
    isLeaf: false,
    link: {
        params: {
            hid: [
                '91307',
            ],
            nid: [
                '54434',
            ],
        },
        target: 'department',
    },
    name: 'Продукты',
    navnodes: [
        {
            category: {
                entity: 'category',
                fullName: 'Алкоголь',
                id: 16155381,
                isLeaf: false,
                kinds: [
                    'alco',
                ],
                modelsCount: 392,
                name: 'Алкоголь',
                nid: 82906,
                offersCount: 32055,
                type: 'gurulight',
                viewType: 'list',
            },
            childrenType: 'mixed',
            entity: 'navnode',
            fullName: 'Алкоголь',
            hasPromo: false,
            id: 82906,
            isLeaf: false,
            link: {
                params: {
                    hid: [
                        '16155381',
                    ],
                    nid: [
                        '82906',
                    ],
                },
                target: 'catalog',
            },
            name: 'Алкоголь',
            navnodes: [
                {
                    category: {
                        entity: 'category',
                        fullName: 'Вино',
                        id: 16155466,
                        isLeaf: true,
                        kinds: [
                            'alco',
                        ],
                        modelsCount: 0,
                        name: 'Вино',
                        nid: 82914,
                        offersCount: 19848,
                        type: 'gurulight',
                        viewType: 'list',
                    },
                    childrenType: 'gurulight',
                    entity: 'navnode',
                    fullName: 'Вино',
                    hasPromo: false,
                    id: 82914,
                    isLeaf: true,
                    link: {
                        params: {
                            hid: [
                                '16155466',
                            ],
                            nid: [
                                '82914',
                            ],
                        },
                        target: 'catalog',
                    },
                    name: 'Вино',
                    rootNavnode: {
                        entity: 'navnode',
                        id: 54434,
                    },
                    slug: 'vino',
                    type: 'category',
                },
            ],
            rootNavnode: {
                entity: 'navnode',
                id: 54434,
            },
            slug: 'alkogol',
            type: 'category',
        },
    ],
    rootNavnode: {
        entity: 'navnode',
        id: 54434,
    },
    slug: 'produkty-napitki',
    type: 'category',
};
