import {createProduct, createOffer} from '@yandex-market/kadavr/mocks/Report/helpers/searchResult';
import {createPriceRange} from '@yandex-market/kadavr/mocks/Report/helpers/price';
import {createEntityPicture} from '@yandex-market/kadavr/mocks/Report/helpers/picture';
import mergeReportState from '@yandex-market/kadavr/mocks/Report/helpers/mergeState';

import {routes} from '@self/platform/spec/hermione/configs/routes';

const phoneProductRoute = routes.product.phone;
const SHOP_ID = 1925;
const SHOP_SLUG = 'tekhnopark';

const productOptions = {
    type: 'model',
    prices: createPriceRange(300, 400, 'RUB'),
    slug: 'smartfon-apple-iphone-7-128gb',
    categories: [
        {
            entity: 'category',
            id: 91491,
            name: 'Мобильные телефоны',
            fullName: 'Мобильные телефоны',
            slug: 'mobilnye-telefony',
            type: 'guru',
            isLeaf: true,
        },
    ],
    offers: {
        count: 7,
    },
};

const picture = createEntityPicture(
    {
        original: {
            url: '//avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/orig',
        },
    },
    'product', phoneProductRoute.productId,
    '//avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/1hq'
);

const mockedProduct = createProduct(productOptions, phoneProductRoute.productId);
const defaultOfferOptions = {
    type: 'offer',
    benefit: {
        description: 'Хорошая цена от надёжного магазина',
        isPrimary: true,
        type: 'cheapest',
    },
    prices: createPriceRange(300, 400, 'RUB'),
    shop: {
        entity: 'shop',
        name: 'Технопарк',
        id: SHOP_ID,
        status: 'actual',
        slug: SHOP_SLUG,
        overallGradesCount: 218,
    },
    orderMinCost: {
        value: 50500,
        currency: 'RUR',
    },
    urls: {
        encrypted: '/redir/',
    },
};

const defaultOffer = createOffer(defaultOfferOptions);

const productWithDefaultOffer = mergeReportState([
    mockedProduct,
    picture,
    defaultOffer,
]);

export {
    productWithDefaultOffer,
    phoneProductRoute,
};
