import {createProduct, createOfferForProduct} from '@yandex-market/kadavr/mocks/Report/helpers/searchResult';
import mergeReportState from '@yandex-market/kadavr/mocks/Report/helpers/mergeState';

import offerFixture from './offer';
import productOptionsFixture from './productOptions';
import dataFixture from './data';

const params = {productId: 1, slug: 'telefon'};
const mockedProduct = createProduct(productOptionsFixture, params.productId);
const promo = {
    type: 'cheapest-as-gift',
    landingUrl: 'https://pokupki.market.yandex.ru/promo/2',
    itemsInfo: {
        count: 3,
    },

};
const offer = createOfferForProduct({
    ...offerFixture,
    promos: [promo],
}, params.productId, '3');

const reportState = mergeReportState([
    mockedProduct,
    offer,
    dataFixture,
]);

export default {
    params,
    reportState,
};
