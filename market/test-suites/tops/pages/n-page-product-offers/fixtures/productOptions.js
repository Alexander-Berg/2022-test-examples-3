import {createPriceRange} from '@yandex-market/kadavr/mocks/Report/helpers/price';

export default {
    type: 'model',
    slug: 'telefon',
    prices: createPriceRange(300, 400, 'RUB'),
    categories: [{
        entity: 'category',
        id: 91491,
        slug: 'mobilnye-telefony',
        name: 'Мобильные телефоны',
        fullName: 'Мобильные телефоны',
        type: 'guru',
        isLeaf: true,
    }],
    navnodes: [{
        entity: 'navnode',
        id: 54726,
        name: 'Мобильные телефоны',
        slug: 'mobilnye-telefony',
        fullName: 'Мобильные телефоны',
        isLeaf: true,
        rootNavnode: {},
    }],
};
