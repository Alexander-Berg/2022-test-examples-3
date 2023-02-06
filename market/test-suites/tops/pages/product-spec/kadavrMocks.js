import {createEntityPicture} from '@yandex-market/kadavr/mocks/Report/helpers/picture';
import {createProduct} from '@yandex-market/kadavr/mocks/Report/helpers';


const productId = 123;
const slug = 'test-smartfon';
const title = 'Тестовый телефон';
const categoryName = 'Мобильные телефоны';

const picture = createEntityPicture(
    {
        original: {
            url: '//avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/orig',
        },
    },
    'product', productId,
    '//avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/1hq'
);

const getDefaultProductMock = type => ({
    id: productId,
    slug,
    type,
    titles: {
        raw: title,
    },
    opinions: 64,
    categories: [
        {
            entity: 'category',
            id: 91491,
            name: categoryName,
            fullName: categoryName,
            slug: 'mobilnye-telefony',
            type: 'guru',
            isLeaf: true,
        },
    ],
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
                `${slug}/${productId}/spec`,
            expectedOpenGraphDescription: 'Подробные характеристики' +
                ` модели ${title}` +
                ' — с описанием всех особенностей.' +
                ' А также цены, рейтинг магазинов и отзывы покупателей.',
            expectedOpenGraphTitle: 'Характеристики модели' +
                ` ${title} — ${categoryName} — Яндекс.Маркет`,
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
                `${slug}/${productId}/spec`,
            expectedOpenGraphDescription: 'Подробные характеристики' +
                ` товара  ${title}` +
                ' — с описанием всех особенностей.' +
                ' А также цены, рейтинг магазинов и отзывы покупателей.',
            expectedOpenGraphTitle: `Характеристики товара  ${title}` +
                ` — ${categoryName} — Яндекс.Маркет`,
        },
        mocks: {
            'report': createProduct(getDefaultProductMock('cluster'), productId),
        },
    },
};


export {
    product,
    picture,
};
