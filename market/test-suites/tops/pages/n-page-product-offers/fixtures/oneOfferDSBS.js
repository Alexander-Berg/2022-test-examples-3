import {createProduct, createOfferForProduct} from '@yandex-market/kadavr/mocks/Report/helpers/searchResult';
import mergeReportState from '@yandex-market/kadavr/mocks/Report/helpers/mergeState';
import {offerDSBSMock} from '@self/platform/spec/hermione/fixtures/dsbs';

import offerFixture from './offer';
import productOptionsFixture from './productOptions';
import dataFixture from './data';

const params = {productId: 1, slug: 'telefon'};
const mockedProduct = createProduct(productOptionsFixture, params.productId);
const offerId = '3';

const offerMock = {
    ...offerFixture,
    ...offerDSBSMock,
    ...{
        beruFeeShow: '123',
        urls: {
            cpa: '/redir/cpa',
            U_DIRECT_OFFER_CARD_URL: '//market.yandex.ru/offer/kC99LqNF37bPZo26iOjHIg?cpc=RZ4czfFWtyuFack',
        },
    },
    id: offerId,
    wareId: offerId,
};
delete offerMock.sku;
delete offerMock.marketSku;

const offer = createOfferForProduct(offerMock, params.productId, offerId);

const reportState = mergeReportState([
    mockedProduct,
    offer,
    dataFixture,
]);

export default {
    offerMock,
    params,
    reportState,
};
