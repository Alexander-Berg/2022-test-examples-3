export default {
    category: {
        entity: 'category',
        fullName: 'Товары для дома',
        id: 90666,
        isLeaf: false,
        modelsCount: 2226208,
        name: 'Товары для дома',
        nid: 54422,
        offersCount: 11266338,
        type: 'gurulight',
        viewType: 'list',
    },
    childrenType: 'mixed',
    entity: 'navnode',
    fullName: 'Товары для дома',
    hasPromo: false,
    icons: [],
    id: 54422,
    isLeaf: false,
    link: {
        params: {
            hid: [
                '90666',
            ],
            nid: [
                '54422',
            ],
        },
        target: 'department',
    },
    name: 'Товары для дома',
    navnodes: [
        {
            category: {
                entity: 'category',
                fullName: 'Хозяйственные товары',
                id: 10607801,
                isLeaf: false,
                modelsCount: 12765,
                name: 'Хозяйственные товары',
                nid: 58621,
                offersCount: 365990,
                type: 'gurulight',
                viewType: 'list',
            },
            childrenType: 'mixed',
            entity: 'navnode',
            fullName: 'Хозяйственные товары',
            hasPromo: false,
            id: 58621,
            isLeaf: false,
            link: {
                params: {
                    hid: [
                        '10607801',
                    ],
                    nid: [
                        '58621',
                    ],
                },
                target: 'catalog',
            },
            name: 'Хозяйственные товары',
            navnodes: [
                {
                    category: {
                        entity: 'category',
                        fullName: 'Хранение вещей',
                        id: 12805274,
                        isLeaf: false,
                        modelsCount: 916,
                        name: 'Хранение вещей',
                        nid: 62776,
                        offersCount: 25653,
                        type: 'gurulight',
                        viewType: 'list',
                    },
                    childrenType: 'mixed',
                    entity: 'navnode',
                    fullName: 'Хранение вещей',
                    hasPromo: false,
                    id: 62776,
                    isLeaf: false,
                    link: {
                        params: {
                            hid: [
                                '12805274',
                            ],
                            nid: [
                                '62776',
                            ],
                        },
                        target: 'catalog',
                    },
                    name: 'Хранение вещей',
                    navnodes: [
                        {
                            category: {
                                entity: 'category',
                                fullName: 'Чехлы для хранения одежды',
                                id: 12807782,
                                isLeaf: true,
                                modelsCount: 45,
                                name: 'Чехлы для одежды',
                                nid: 62790,
                                offersCount: 1680,
                                type: 'gurulight',
                                viewType: 'grid',
                            },
                            childrenType: 'gurulight',
                            entity: 'navnode',
                            fullName: 'Чехлы для хранения одежды',
                            hasPromo: false,
                            id: 62790,
                            isLeaf: true,
                            link: {
                                params: {
                                    hid: [
                                        '12807782',
                                    ],
                                    nid: [
                                        '62790',
                                    ],
                                },
                                target: 'catalog',
                            },
                            name: 'Чехлы для одежды',
                            rootNavnode: {
                                entity: 'navnode',
                                id: 54422,
                            },
                            slug: 'chekhly-dlia-khraneniia-odezhdy',
                            type: 'category',
                        },
                    ],
                    rootNavnode: {
                        entity: 'navnode',
                        id: 54422,
                    },
                    slug: 'khranenie-veshchei',
                    type: 'category',
                },
            ],
            rootNavnode: {
                entity: 'navnode',
                id: 54422,
            },
            slug: 'khoziaistvennye-tovary',
            type: 'category',
        },
    ],
    rootNavnode: {
        entity: 'navnode',
        id: 54422,
    },
    slug: 'tovary-dlia-doma',
    type: 'category',
};
