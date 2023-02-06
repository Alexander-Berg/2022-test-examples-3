import {createProduct, createOfferForProduct} from '@yandex-market/kadavr/mocks/Report/helpers/searchResult';
import mergeReportState from '@yandex-market/kadavr/mocks/Report/helpers/mergeState';
import giftOffer from '@self/project/src/spec/hermione/fixtures/promo/mocks/offer/televizor';
import {withGenericBundlePromo} from '@self/project/src/spec/hermione/fixtures/genericBundle/withGenericBundlePromo';
import offerFixture from './offer';

import productOptionsFixture from './productOptions';
import dataFixture from './data';

const params = {productId: 1, slug: 'telefon'};
const mockedProduct = createProduct(productOptionsFixture, params.productId);
const offerMock = {
    ...offerFixture,
    ...{
        beruFeeShow: '123',
        urls: {
            cpa: '/redir/cpa',
            U_DIRECT_OFFER_CARD_URL: '//market.yandex.ru/offer/kC99LqNF37bPZo26iOjHIg?cpc=RZ4czfFWtyuFack',
        },
        cpa: 'real',
    },
};
const offer = createOfferForProduct({
    ...withGenericBundlePromo(offerMock, giftOffer),
}, params.productId, '3');

const reportState = mergeReportState([
    mockedProduct,
    offer,
    dataFixture,
    {
        data: {
            offers: [giftOffer],
        },
    },
]);

export default {
    params,
    reportState,
};
