export default {
    entity: 'page',
    id: 42807,
    rev: 177325,
    type: 'brand',
    name: 'Дефолтная страница',
    hasContextParams: true,
    content: {
        rows: [
            {
                'entity': 'box',
                'name': 'Grid12',
                'props': {
                    'type': 'row',
                    'width': 'maya',
                    'layout': true,
                    'grid': 1,
                },
                'nodes': [
                    {
                        'entity': 'box',
                        'name': 'Grid12',
                        'props': {
                            'type': 'column',
                            'layout': false,
                            'width': 1,
                            'position': 'default',
                            'sticky': false,
                        },
                        'nodes': [
                            {
                                'props': {
                                    'autoplay': false,
                                    'title': 'Свойства виджета',
                                },
                                'resources': {
                                    'garsons': [
                                        {
                                            'count': 12,
                                            'params': {
                                                'id': '152981',
                                            },
                                            'id': 'SimilarBrands',
                                        },
                                    ],
                                },
                                'id': 83123209,
                                'name': 'LogoCarousel',
                                'entity': 'widget',
                            },
                        ],
                    },
                ],
            },
        ],
    },
};
