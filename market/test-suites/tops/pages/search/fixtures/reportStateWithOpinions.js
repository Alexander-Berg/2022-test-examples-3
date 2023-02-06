import {createProduct} from '@yandex-market/kadavr/mocks/Report/helpers/searchResult';
import mergeReportState from '@yandex-market/kadavr/mocks/Report/helpers/mergeState';
import {popularitySort, opinionsSort} from '@yandex-market/kadavr/mocks/Report/helpers/sort';


const firstProductMock = createProduct({
    showUid: 'testProductShowUid1',
    opinions: 1,
    slug: 'test-product',
    preciseRating: 1,
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
}, 1111);
const secondProductMock = createProduct({
    showUid: 'testProductShowUid2',
    opinions: 2,
    slug: 'test-product',
    preciseRating: 2,
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
}, 2222);
const thirdProductMock = createProduct({
    showUid: 'testProductShowUid3',
    opinions: 3,
    slug: 'test-product',
    preciseRating: 3,
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
}, 3333);

const reportState = mergeReportState([
    firstProductMock,
    secondProductMock,
    thirdProductMock,
    popularitySort,
    opinionsSort,
]);

const opinions = {
    modelOpinions: [{
        id: 1234,
        product: {
            id: 1111,
        },
    }, {
        id: 1235,
        product: {
            id: 2222,
        },
    }, {
        id: 1236,
        product: {
            id: 2222,
        },
    }, {
        id: 1237,
        product: {
            id: 3333,
        },
    }, {
        id: 1238,
        product: {
            id: 3333,
        },
    }, {
        id: 1239,
        product: {
            id: 3333,
        },
    }],
};

export default {
    reportState,
    opinions,
};
