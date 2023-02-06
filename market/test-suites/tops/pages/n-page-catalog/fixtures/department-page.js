export default {
    entity: 'page',
    id: 43659,
    rev: 116689,
    type: 'catalog',
    name: 'Электроника',
    hasContextParams: true,
    content: {
        entity: 'box',
        rows: [{
            entity: 'box',
            name: 'Grid12',
            props: {
                type: 'row',
                width: 'default',
                layout: true,
                grid: 8,
            },
            nodes: [{
                entity: 'box',
                name: 'Grid12',
                props: {
                    type: 'column',
                    layout: false,
                    width: 2,
                    position: 'default',
                },
                nodes: [{
                    entity: 'widget',
                    name: 'NavigationTree',
                    id: 18959119,
                    resources: {
                        garsons: [{
                            id: 'NavigationTree',
                            params: {
                                nid: 54440,
                                depth: 2,
                            },
                        }],
                    },
                }],
            }],
        }, {
            entity: 'box',
            name: 'Grid12',
            props: {
                type: 'row',
                width: 'default',
                layout: true,
                grid: 8,
            },
            nodes: [{
                entity: 'box',
                name: 'Grid12',
                props: {
                    type: 'column',
                    layout: false,
                    width: 1,
                    position: 'default',
                },
                nodes: [{
                    entity: 'widget',
                    name: 'CatalogHeader',
                    id: 36554101,
                    resources: {
                        garsons: [{
                            id: 'CatalogHeader',
                        }],
                    },
                    props: {
                        title: 'Кухонные приборы для приготовления напитков',
                    },
                }],
            }],
        }],
    },
};
