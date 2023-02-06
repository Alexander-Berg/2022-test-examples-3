import {createProduct} from '@yandex-market/kadavr/mocks/Report/helpers/searchResult';
import {createEntityPicture} from '@yandex-market/kadavr/mocks/Report/helpers/picture';
import mergeReportState from '@yandex-market/kadavr/mocks/Report/helpers/mergeState';

const PRODUCT_ID = 123;

const productOptions = {
    type: 'model',
    id: PRODUCT_ID,
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
    titles: {
        raw: 'Смартфон Apple iPhone 7 128GB',
        highlighted: [{
            value: 'Смартфон Apple iPhone 7 128GB',
        }],
    },
};

const mockedProduct = createProduct(productOptions, PRODUCT_ID);

const picture1 = createEntityPicture(
    {
        original: {
            url: '//avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/orig',
        },
        thumbnails: [{
            url: '//avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/2hq',
        }],
    },
    'product', PRODUCT_ID,
    '//avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/orig/1'
);

const picture2 = createEntityPicture(
    {
        original: {
            url: '//avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/orig',
        },
        thumbnails: [{
            url: '//avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/2hq',
        }],
    },
    'product', PRODUCT_ID,
    '//avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/orig/2'
);

const picture3 = createEntityPicture(
    {
        original: {
            url: '//avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/orig',
        },
        thumbnails: [{
            url: '//avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/2hq',
        }],
    },
    'product', PRODUCT_ID,
    '//avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/orig/3'
);

const product = mergeReportState([
    mockedProduct,
    picture1,
    picture2,
    picture3,
]);

export {
    product,
    productOptions,
};
