export default {
    entity: 'page',
    id: 42674,
    rev: 1274458,
    type: 'navnode_touch',
    name: 'Главная страница мобильного (морда белого тача)',
    hasContextParams: true,
    links: [{
        entity: 'navnode',
        id: '54415',
    }],
    content: {
        entity: 'content',
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
                                id: 111441079,
                                entity: 'widget',
                                name: 'EComQuestions',
                                loadMode: 'default',
                                hideForRobots: false,
                                epicModeForLazyLoad: 'default',
                                placeholder: 'SnippetScrollbox',
                                props: {},
                            },
                        ],
                    },
                ],
            },
        ],
    },
};
