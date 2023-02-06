export default {
    entity: 'navnode',
    hasPromo: false,
    id: 54419,
    slug: 'bytovaia-tekhnika',
    isLeaf: false,
    childrenType: 'mixed',
    category: {
        entity: 'category',
        fullName: 'Бытовая техника',
        id: 198118,
        isLeaf: false,
        modelsCount: 195063,
        name: 'Бытовая техника',
        slug: 'bytovaya-tekhnika',
        nid: 54419,
        offersCount: 841998,
        type: 'gurulight',
        viewType: 'list',
    },
    link: {
        params: {
            hid: [
                '198118',
            ],
            nid: [
                '54419',
            ],
        },
        target: 'department',
    },
    fullName: 'Бытовая техника',
    name: 'Бытовая техника',
    rootNavnode: {
        entity: 'navnode',
        id: 54419,
    },
    navnodes: [
        {
            entity: 'navnode',
            category: {
                entity: 'category',
                fullName: 'Мелкая техника для кухни',
                slug: 'melkaia-tekhnika-dlia-kukhni',
                id: 90579,
                isLeaf: false,
                modelsCount: 39072,
                name: 'Мелкая техника для кухни',
                nid: 54472,
                offersCount: 199156,
                type: 'gurulight',
                viewType: 'list',
            },
            childrenType: 'mixed',
            hasPromo: true,
            fullName: 'Мелкая техника для кухни',
            id: 54472,
            isLeaf: false,
            link: {
                params: {
                    hid: [
                        '90579',
                    ],
                    nid: [
                        '54472',
                    ],
                },
                target: 'catalog',
            },
            slug: 'melkaia-tekhnika-dlia-kukhni',
            name: 'Мелкая техника для кухни',
            navnodes: [
                {
                    entity: 'navnode',
                    category: {
                        entity: 'category',
                        fullName: 'Кухонные приборы для приготовления напитков',
                        slug: 'kukhonnye-pribory-dlia-prigotovleniia-napitkov',
                        id: 12327560,
                        isLeaf: false,
                        modelsCount: 11276,
                        name: 'Приготовление напитков',
                        nid: 60898,
                        offersCount: 67915,
                        type: 'gurulight',
                        viewType: 'list',
                    },
                    id: 60898,
                    isLeaf: false,
                    link: {
                        params: {
                            hid: [
                                '12327560',
                            ],
                            nid: [
                                '60898',
                            ],
                        },
                        target: 'catalog',
                    },
                    childrenType: 'guru',
                    slug: 'kukhonnye-pribory-dlia-prigotovleniia-napitkov',
                    fullName: 'Кухонные приборы для приготовления напитков',
                    name: 'Кухонные приборы для приготовления напитков',
                    rootNavnode: {
                        entity: 'navnode',
                        id: 54419,
                    },
                },
            ],
            rootNavnode: {
                entity: 'navnode',
                id: 54419,
            },
            type: 'category',
        },
    ],
    type: 'category',
};
