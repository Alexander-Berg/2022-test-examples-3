import {createEntityPicture} from '@yandex-market/kadavr/mocks/Report/helpers/picture';
import {createProduct} from '@yandex-market/kadavr/mocks/Report/helpers';

const productId = 123;
const slug = 'test-smartfon';
const title = 'Тестовый телефон';

const picture = createEntityPicture(
    {
        original: {
            url: '//avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/orig',
        },
    },
    'product', productId,
    '//avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/1hq'
);

const category = {
    entity: 'category',
    id: 91491,
    name: 'Мобильные телефоны',
    fullName: 'Мобильные телефоны',
    slug: 'mobilnye-telefony',
    type: 'guru',
    isLeaf: true,
};

const getDefaultProductMock = type => ({
    id: productId,
    slug,
    type,
    titles: {
        raw: title,
    },
    opinions: 64,
    categories: [category],
    lingua: {
        type: {
            nominative: '',
            genitive: '',
            dative: '',
            accusative: '',
        },
    },
});

const product = {
    guru: {
        routeParams: {
            productId,
            slug,
        },
        testParams: {
            expectedCanonicalLink: 'https://market.yandex.ru/product--' +
                `${slug}/${productId}/reviews`,
            expectedOpenGraphDescription: `${title}:` +
                ' отзывы покупателей на Яндекс Маркете. Достоинства и недостатки товара.' +
                ` Важная информация о товаре ${title}:` +
                ' описание, фотографии, цены, варианты доставки, магазины на карте.',
            expectedOpenGraphTitleRegex: `^Стоит ли покупать ${title}\\? Отзывы на Яндекс\\sМаркете$`,
        },
        mocks: {
            'report': createProduct(getDefaultProductMock('model'), productId),
        },
    },
    cluster: {
        routeParams: {
            productId,
            slug,
        },
        testParams: {
            expectedCanonicalLink: 'https://market.yandex.ru/product--' +
                `${slug}/${productId}/reviews`,
            expectedOpenGraphDescription: `${title}: отзывы покупателей` +
                ' на Яндекс Маркете. Достоинства и недостатки товара.' +
                ` Важная информация о товаре ${title}:` +
                ' описание, фотографии, цены, варианты доставки, магазины на карте.',
            expectedOpenGraphTitleRegex: `^Стоит ли покупать ${title}\\? Отзывы на Яндекс\\sМаркете$`,
        },
        mocks: {
            'report': createProduct(getDefaultProductMock('cluster'), productId),
        },
    },
};


export {
    product,
    picture,
    category,
};
