import {createProduct, createOfferForProduct} from '@yandex-market/kadavr/mocks/Report/helpers/searchResult';
import mergeReportState from '@yandex-market/kadavr/mocks/Report/helpers/mergeState';

import offerFixture from './offer';
import productOptionsFixture from './productOptions';
import dataFixture from './data';

const params = {productId: 1, slug: 'telefon'};
const mockedProduct = createProduct(productOptionsFixture, params.productId);
const offerId = '3';

export const usedGoods = {
    type: 'as-new',
    desc: 'идеальное',
};

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
    ...offerMock,
    // Добавляем для возможности тестирования бу-функциональности
    usedGoods,
}, params.productId, offerId);

const reportState = mergeReportState([
    mockedProduct,
    offer,
    dataFixture,
]);

export default {
    offerId,
    offerMock,
    params,
    reportState,
};
