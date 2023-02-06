const {
    SEARCH_PLACE_STANDALONE,
} = require('@self/root/src/entities/searchPlace/constants');
/**
 * CMS Layout
 */
const {
    buildFakeMarkup,
} = require('@self/root/src/resolvers/search/layout/fakeMarkup');


export const SERP_NODE = {
    entity: 'widget',
    name: 'SearchSerp',
    id: 'SearchSerp',
    wrapperProps: {
        paddings: {
            bottom: '5',
        },
    },
    props: {
        searchPlace: SEARCH_PLACE_STANDALONE,
        snippetProps: {
            theme: 'siete',
        },
        withIncuts: false,
    },
};

export const PAGER_NODE = {
    entity: 'widget',
    name: 'SearchPager',
    id: 'SearchPager',
    wrapperProps: {},
    props: {
        searchPlace: SEARCH_PLACE_STANDALONE,
        scrollToAnchor: 'serpTop',
    },
};

const INTENTS_NODE = {
    entity: 'widget',
    name: 'SearchIntents',
    id: 'SearchIntents',
    props: {
        searchPlace: SEARCH_PLACE_STANDALONE,
    },
    wrapperProps: {
        font: {
            size: '200',
        },
        borders: {
            bottom: true,
            color: '$gray400',
        },
        margins: {
            top: '5',
            bottom: '5',
        },
        paddings: {
            bottom: '4',
        },
    },
};

export const FILTERS_NODE = {
    entity: 'widget',
    name: 'SearchFilters',
    id: 'SearchFilters',
    wrapperProps: {
        font: {
            size: '200',
        },
        margins: {
            top: '4',
            bottom: '5',
        },
    },
    props: {
        scrollToAnchor: 'serpTop',
        searchPlace: SEARCH_PLACE_STANDALONE,
    },
};

export const SORT_CONTROLS_NODE = {
    entity: 'widget',
    name: 'SearchControls',
    id: 'SearchControls',
    wrapperProps: {
    // якорь, характеризующий начало выдачи
    // к нему будет скролл при смене страницы
        anchor: 'serpTop',
        margins: {
            top: '5',
            bottom: '6',
        },
        paddings: {
            top: '1',
        },
    },
    props: {
        searchPlace: SEARCH_PLACE_STANDALONE,
        hideIfEmptySearchResults: true,
        sortViewType: 'row',
        hasMapIconLink: true,
    },
};

export const CMS_SEARCH_LAYOUT = buildFakeMarkup(
    'fake-search-layout',
    [
        {
            entity: 'box',
            props: {
                type: 'row',
                withRowReverse: true,
                layout: true,
                grid: 24,
            },
            name: 'Grid24',
            nodes: [
                {
                    entity: 'box',
                    props: {
                        type: 'column',
                        width: 18,
                        breakpoints: [
                            {breakpoint: 's', width: 27},
                        ],
                    },
                    nodes: [
                        INTENTS_NODE,
                        FILTERS_NODE,
                        SORT_CONTROLS_NODE,
                        SERP_NODE,
                        PAGER_NODE,
                    ],
                }],
        },
    ],
    {
        withHeader: false,
        withFooter: false,
    }
);
