import {createPriceRange} from '@yandex-market/kadavr/mocks/Report/helpers';
import offersMocks from './offers.mock';

export default {
    id: '12345',
    deletedId: '',
    type: 'model',
    categories: [
        {
            entity: 'category',
            id: 91491,
            slug: 'mobilnye-telefony',
            name: 'Мобильные телефоны',
            fullName: 'Мобильные телефоны',
            type: 'guru',
            isLeaf: true,
        },
    ],
    navnodes: [
        {
            entity: 'navnode',
            id: 54726,
            name: 'Мобильные телефоны',
            slug: 'mobilnye-telefony',
            fullName: 'Мобильные телефоны',
            isLeaf: true,
            rootNavnode: {},
        },
    ],
    offers: {
        items: Object.keys(offersMocks),
    },
    prices: createPriceRange(7290, 9000, 'RUB'),
    slug: 'something',
};
