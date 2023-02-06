import ProductTabs from '@self/platform/widgets/content/ProductTabs/__pageObject';
import DefaultOffer from '@self/platform/components/DefaultOffer/__pageObject';
import OfferCardSpecs from '@self/platform/spec/page-objects/OfferCardSpecs';
import SearchSimilar from '@self/platform/widgets/content/SearchSimilar/__pageObject';
import ShopReviews from '@self/platform/spec/page-objects/widgets/content/ShopReviews';

export default {
    elementClick: {
        offer: {
            description: 'Заголовок вкладки "Описание"',
            meta: {
                id: 'marketfront-3583',
                issue: 'MARKETVERSTKA-34985',
            },
            selector: `${ProductTabs.offer} ${ProductTabs.link}`,
            expectedGoalName: 'offer-page_mc-tabs-usual',
            scrollOffset: [0, -50],
        },
        specs: {
            description: 'Заголовок вкладки "Характеристики"',
            meta: {
                id: 'marketfront-3584',
                issue: 'MARKETVERSTKA-34984',
            },
            selector: `${ProductTabs.specs} ${ProductTabs.link}`,
            expectedGoalName: 'offer-page_mc-tabs-usual',
            scrollOffset: [0, -50],
        },
        similar: {
            description: 'Заголовок вкладки "Похожие товары"',
            meta: {
                id: 'marketfront-3585',
                issue: 'MARKETVERSTKA-34983',
            },
            selector: `${ProductTabs.similar} ${ProductTabs.link}`,
            expectedGoalName: 'offer-page_mc-tabs-usual',
            scrollOffset: [0, -50],
        },
        /*
        * Точка входа удалена. Задача на возврат тестов https://st.yandex-team.ru/MARKETFRONT-71298
        geo: {
            description: 'Заголовок вкладки "Карта"',
            meta: {
                id: 'marketfront-3586',
                issue: 'MARKETVERSTKA-34982',
            },
            selector: `${ProductTabs.geo} ${ProductTabs.link}`,
            expectedGoalName: 'offer-page_mc-tabs-sticker',
        },*/
        reviews: {
            description: 'Заголовок вкладки "Отзывы на магазин"',
            meta: {
                id: 'marketfront-3587',
                issue: 'MARKETVERSTKA-34981',
            },
            selector: `${ProductTabs.reviews} ${ProductTabs.link}`,
            expectedGoalName: 'offer-page_mc-tabs-usual',
            scrollOffset: [0, -50],
        },
    },
    snippetsVisible: {
        offer: {
            description: 'Контент вкладки "Описание"',
            meta: {
                id: 'marketfront-3512',
                issue: 'MARKETVERSTKA-34582',
            },
            pageId: 'market:offer',
            selector: DefaultOffer.root,
            expectedGoalName: 'offer-page_default-offer_visible',
        },
        specs: {
            description: 'Контент вкладки "Характеристики"',
            meta: {
                id: 'marketfront-3513',
                issue: 'MARKETVERSTKA-34583',
            },
            pageId: 'market:offer-spec',
            selector: OfferCardSpecs.root,
            expectedGoalName: 'offer-spec-page_offer-specs_visible',
        },
        similar: {
            description: 'Контент вкладки "Похожие товары"',
            meta: {
                id: 'marketfront-3514',
                issue: 'MARKETVERSTKA-34584',
            },
            pageId: 'market:offer-similar',
            selector: SearchSimilar.snippet,
            expectedGoalName: 'offer-similar-page_similar_search-results_search-results-paged_search-partition_snippet-list_snippet-card_visible',
        },
        reviews: {
            description: 'Контент вкладки "Отзывы на магазин"',
            meta: {
                id: 'marketfront-3516',
                issue: 'MARKETVERSTKA-34586',
            },
            pageId: 'market:offer-reviews',
            selector: ShopReviews.root,
            expectedGoalName: 'offer-reviews-page_shop-reviews_visible',
        },
    },
};
