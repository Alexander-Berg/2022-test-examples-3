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
                }, {
                    entity: 'widget',
                    name: 'Showcase',
                    id: 29020455,
                    loadMode: 'default',
                    resources: {
                        garsons: [{
                            id: 'PopularBrands',
                            count: 15,
                            params: {rgb: 'GREEN', hid: '198119'},
                        }],
                    },
                    props: {
                        title: 'Популярные бренды',
                        subtitle: {type: 'default'},
                        titleParams: {size: 'm', type: 'default'},
                        paddingTop: 'condensed',
                        paddingBottom: 'condensed',
                        paddingLeft: 'normal',
                        paddingRight: 'normal',
                        theme: 'light',
                        titleStyle: 'default',
                        compensateSideMargin: false,
                        rows: 3,
                        cols: 5,
                        bordered: false,
                        labels: {more: 'Показать ещё'},
                    },
                }],
            }],
        }],
    },
};

