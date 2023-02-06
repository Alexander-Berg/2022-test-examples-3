export default {
    entity: 'page',
    id: 42807,
    rev: 177325,
    type: 'brand',
    name: 'Дефолтная страница',
    hasContextParams: true,
    content: {
        entity: 'box',
        rows: [
            {
                entity: 'box',
                name: 'Grid12',
                props: {
                    type: 'row',
                    width: 'default',
                    layout: true,
                    grid: 1,
                },
                nodes: [
                    {
                        entity: 'box',
                        name: 'Grid12',
                        props: {
                            type: 'column',
                            layout: false,
                            width: 1,
                            position: 'default',
                            sticky: false,
                        },
                        nodes: [
                            {
                                entity: 'widget',
                                name: 'ContentWrapper',
                                id: 49734110,
                                loadMode: 'default',
                                nodes: [
                                    {
                                        entity: 'widget',
                                        name: 'WysiwygText',
                                        id: 49734112,
                                        loadMode: 'default',
                                        resources: {
                                            garsons: [
                                                {
                                                    id: 'WysiwygText',
                                                    params: {
                                                        text: '<h3>СМОТРИТЕ ТАКЖЕ</h3>',
                                                    },
                                                },
                                            ],
                                        },
                                        props: {},
                                    },
                                ],
                                props: {
                                    subtitle: {
                                        type: 'default',
                                    },
                                    titleParams: {
                                        size: 'm',
                                        type: 'default',
                                    },
                                    paddingTop: 'normal',
                                    paddingBottom: 'none',
                                    paddingLeft: 'normal',
                                    paddingRight: 'normal',
                                    backgroundColor: 'f4f4f4',
                                    titleStyle: 'default',
                                    compensateSideMargin: false,
                                },
                            },
                            {
                                props: {
                                    autoplay: true,
                                    title: 'Свойства виджета',
                                },
                                resources: {
                                    garsons: [
                                        {
                                            count: 24,
                                            params: {
                                                id: '153043',
                                            },
                                            id: 'SimilarBrands',
                                        },
                                    ],
                                },
                                id: 49537386,
                                name: 'LogoCarousel',
                                entity: 'widget',
                            },
                        ],
                    },
                ],
            },
        ],
    },
};
