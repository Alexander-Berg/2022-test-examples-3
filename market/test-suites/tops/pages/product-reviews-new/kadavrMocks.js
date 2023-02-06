import {createEntityPicture} from '@yandex-market/kadavr/mocks/Report/helpers/picture';
import {createProduct} from '@yandex-market/kadavr/mocks/Report/helpers';

const productId = 123;
const categoryId = 91491;
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

const getDefaultProductMock = type => ({
    id: productId,
    slug,
    type,
    titles: {
        raw: title,
    },
    categories: [
        {
            entity: 'category',
            id: categoryId,
            name: 'Мобильные телефоны',
            fullName: 'Мобильные телефоны',
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
                `${slug}/${productId}/reviews/add`,
            expectedOpenGraphDescription: `Страница добавления отзывов на ${title}. Вы можете оценить товар по нескольким характеристикам,` +
                ' а также поставить общий балл.',
            expectedOpenGraphTitle: 'Добавление отзывов — Яндекс.Маркет',
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
                `${slug}/${productId}/reviews/add`,
            expectedOpenGraphDescription: `Страница добавления отзывов на ${title}. Вы можете оценить товар по нескольким характеристикам,` +
                ' а также поставить общий балл.',
            expectedOpenGraphTitle: 'Добавление отзывов — Яндекс.Маркет',
        },
        mocks: {
            'report': createProduct(getDefaultProductMock('cluster'), productId),
        },
    },
};

export {
    product,
    picture,
    categoryId,
};
