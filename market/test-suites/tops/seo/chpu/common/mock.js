import {
    createProduct,
    createEntityPicture,
    mergeState,
    createShopInfo,
} from '@yandex-market/kadavr/mocks/Report/helpers';

const productId = 1;
const shopId = 431782;
const shopSlug = 'beru';

const product = {
    slug: 'product',
    categories: [{
        entity: 'category',
        id: 91491,
        name: 'Мобильные телефоны',
        fullName: 'Мобильные телефоны',
        slug: 'mobilnye-telefony',
        type: 'guru',
        isLeaf: true,
    }],
    opinions: 373,
    rating: 4.5,
    ratingCount: 82,
    reviews: 32,
    type: 'model',
    prices: {
        min: '23700',
        max: '47700',
        currency: 'RUR',
        avg: '34900',
    },
    titles: {
        raw: 'Классный продукт',
        highlighted: [{value: 'Классный продукт'}],
    },
    vendor: {
        entity: 'vendor',
        id: 152900,
        name: 'Bosch',
        slug: 'bosch',
    },
};

const shop = createShopInfo({
    entity: 'shop',
    id: shopId,
    status: 'actual',
    oldStatus: 'actual',
    slug: shopSlug,
    ratingToShow: 3.166666667,
    overallGradesCount: 218,
}, shopId);

const picture = createEntityPicture(
    {
        original: {
            url: '//avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/orig',
        },
        thumbnails: [{
            url: '//avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/2hq',
        }],
    },
    'product', productId,
    '//avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/orig/1'
);

const report = mergeState([
    createProduct(product, productId),
    picture,
]);

export {
    report,
    productId,
    shop,
    shopId,
    shopSlug,
};
