import {createPrice} from '@yandex-market/kadavr/mocks/Report/helpers/price';

const PRICE = createPrice(300, 'RUB', 300, false, {
    /**
     * Из-за бага в json-schema-faker явно указываем отсутствие скидки
     */
    discount: null,
});

const category = {
    entity: 'category',
    id: 91491,
    name: 'Мобильные телефоны',
    fullName: 'Мобильные телефоны',
    type: 'guru',
    isLeaf: true,
};

const testShop = {
    id: 123,
    qualityRating: 4,
    overallGradesCount: 111,
    newGradesCount: 111,
    newGradesCount3M: 10,
    name: 'test shop',
    slug: 'test-shop',
    feed: {},
};

const anotherTestShop = {
    id: 124,
    qualityRating: 4,
    overallGradesCount: 111,
    newGradesCount: 111,
    newGradesCount3M: 10,
    name: 'second test shop',
    slug: 'second-test-shop',
};

const offerUrls = {
    encrypted: 'https://ya.ru',
};

const defaultQuestion = productId => ({
    id: 1,
    text: 'lol',
    product: {
        entity: 'product',
        id: productId,
    },
});

export {
    PRICE,

    category,
    testShop,
    anotherTestShop,
    offerUrls,
    defaultQuestion,
};
