import {createProduct, createOfferForProduct} from '@yandex-market/kadavr/mocks/Report/helpers/searchResult';
import mergeReportState from '@yandex-market/kadavr/mocks/Report/helpers/mergeState';

import offerFixture from './offer';
import productOptionsFixture from './productOptions';
import dataFixture from './data';

const cashbackAmount = 100;
const params = {productId: 1, slug: 'telefon'};
const mockedProduct = createProduct(productOptionsFixture, params.productId);
const offer = createOfferForProduct({
    ...offerFixture,
    promo: {
        type: 'blue-cashback',
        value: cashbackAmount,
        tags: ['extra-cashback'],
    },
}, params.productId, '3');

const reportState = mergeReportState([
    mockedProduct,
    offer,
    dataFixture,
]);

export default {
    params,
    reportState,
    cashbackAmount,
};
