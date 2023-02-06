import ProductTabs from '@self/platform/widgets/content/ProductTabs/__pageObject';

export default {
    routes: {
        specs: {
            description: 'Характеристики',
            meta: {
                id: 'marketfront-3482',
                issue: 'MARKETVERSTKA-34564',
            },
            params: {
                expectedPage: 'spec',
                selector: ProductTabs.specs,
            },
        },
        similar: {
            description: 'Похожие товары',
            meta: {
                id: 'marketfront-3611',
                issue: 'MARKETVERSTKA-35124',
            },
            params: {
                expectedPage: 'similar',
                selector: ProductTabs.similar,
            },
        },
        /*
        * Точка входа удалена. Задача на возврат тестов https://st.yandex-team.ru/MARKETFRONT-71298
        geo: {
            description: 'Карта',
            meta: {
                id: 'marketfront-3612',
                issue: 'MARKETVERSTKA-35126',
            },
            params: {
                expectedPage: 'geo',
                selector: ProductTabs.geo,
            },
        },*/
        reviews: {
            description: 'Отзывы на магазин',
            meta: {
                id: 'marketfront-3613',
                issue: 'MARKETVERSTKA-35127',
            },
            params: {
                expectedPage: 'reviews',
                selector: ProductTabs.reviews,
            },
        },
    },
};
