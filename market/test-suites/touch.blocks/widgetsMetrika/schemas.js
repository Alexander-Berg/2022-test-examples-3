/** Параметры для валидации метрики (схемы), для каждого гарсона вне зависимости от типа виджета */

/** объекты схем, которые передаются в js-shcema должны быть созданы после импорта библиотеки */
require('js-schema');

module.exports = {
    AlsoViewed: {
        root: {
            reqId: String,
            cmsWidgetId: [Number, String],
            props: {
                title: String,
            },
            garsons: Array.of({
                id: 'AlsoViewedProductsDenormalized',
            }),
        },
        snippet: {
            // entity: 'product',
            // реальный report возвращает числа, а кадавровый хэлпер строку
            productId: [Number, String],
        },
    },

    ProductAccessories: {
        root: {
            reqId: String,
            cmsWidgetId: [Number, String],
            garsons: Array.of({
                id: 'ProductAccessories',
            }),
        },
        snippet: {
            // entity: 'product',
            productId: [Number, String],
            reasonsToBuy: Array.of(String),
            skuId: String,
        },
    },

    AttractiveModels: {
        root: {
            reqId: String,
            cmsWidgetId: [Number, String],
            props: {
                title: String,
            },
            garsons: Array.of({
                id: 'AttractiveModels',
            }),
        },
        snippet: {
            // entity: 'product',
            productId: [Number, String],
        },
    },

    CommonlyPurchasedProducts: {
        root: {
            reqId: String,
            cmsWidgetId: [Number, String],
            props: {
                title: String,
            },
            garsons: Array.of({
                id: 'CommonlyPurchasedProducts',
            }),
        },
        snippet: {
            // entity: 'product',
            productId: [Number, String],
            skuId: String,
        },
    },

    History: {
        root: {
            reqId: String,
            cmsWidgetId: [Number, String],
            props: {
                title: String,
            },
            garsons: Array.of({
                id: 'History',
            }),
        },
        snippet: {
            // entity: 'sku',
            skuId: String,
            productId: [Number, String],
        },
    },

    PopularProducts: {
        root: {
            reqId: String,
            cmsWidgetId: [Number, String],
            props: {
                title: String,
            },
            garsons: Array.of({
                id: 'PopularProducts',
            }),
        },
        snippet: {
            // entity: 'product',
            productId: [Number, String],
        },
    },

    Deals: {
        root: {
            reqId: String,
            cmsWidgetId: Number,
            props: {
                title: String,
            },
            garsons: Array.of({
                id: 'Deals',
            }),
        },
        snippet: {
            // entity: 'product',
            productId: [Number, String],
        },
    },

    GroupSkuByIds: {
        root: {
            reqId: String,
            cmsWidgetId: Number,
            garsons: Array.of({
                id: 'GroupSkuByIds',
            }),
        },
        snippet: {
            // entity: 'sku',
            skuId: String,
            productId: [Number, String],
        },
    },
};
