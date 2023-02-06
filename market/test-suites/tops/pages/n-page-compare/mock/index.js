import {createProduct} from '@yandex-market/kadavr/mocks/Report/helpers/searchResult';
import mergeReportState from '@yandex-market/kadavr/mocks/Report/helpers/mergeState';
import {makeCatalogerTree} from '@self/project/src/spec/hermione/helpers/metakadavr';

import iphone from './iphone.mock.json';
import iphone2 from './iphone2.mock.json';

export const comparisonsMock = [{
    categoryId: '91491',
    lastUpdate: 1481721780000,
    items: [{
        productId: String(iphone.id),
        lastUpdate: 1481721770000,
    }, {
        productId: String(iphone2.id),
        lastUpdate: 1481721780000,
    }],
}];

export const catalogerMock = makeCatalogerTree('Мобильные телефоны', 91491, 54726, {categoryType: 'guru'});
export const reportMock = mergeReportState([
    createProduct(iphone, iphone.id),
    createProduct(iphone2, iphone2.id),
]);
