import {createProduct, createOfferForProduct} from '@yandex-market/kadavr/mocks/Report/helpers/searchResult';
import mergeReportState from '@yandex-market/kadavr/mocks/Report/helpers/mergeState';

import dataFixture from '../../n-page-product-offers/fixtures/data';
import productFixture from '../../n-page-product-offers/fixtures/productOptions';
import offerFixture from './offerCpa';

const offerIds = new Array(12).fill(0).map((_, idx) => idx);

const mockedProducts = offerIds.map(offerId => createProduct(productFixture, offerId));

const offerMock = {
    ...offerFixture,
    beruFeeShow: '123',
    urls: {
        cpa: '/redir/cpa',
        U_DIRECT_OFFER_CARD_URL: '//market.yandex.ru/offer/kC99LqNF37bPZo26iOjHIg?cpc=RZ4czfFWtyuFack',
    },
};

const mockedOffers = offerIds.map(offerId => createOfferForProduct(offerMock, offerId, offerId));

const reportState = mergeReportState([
    dataFixture,
    ...mockedProducts,
    ...mockedOffers,
]);

export default reportState;
