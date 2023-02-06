/* eslint-disable global-require */

import responseParser from '../../../src/response-parser/response-parser';

const responses = {
    modelOffers: require('./responses/model-offers-response.json'),
    offersAndAge: require('./responses/offers-and-age.json'),
    offersAndWarning: require('./responses/offers-and-warning.json'),
    clothes: require('./responses/clothes.json')
};

const expectations = {
    common: {
        ab: require('./expectations/ab-data.json'),
        appSettings: require('./expectations/app-settings.json'),
        shopInfo: require('./expectations/shop-info.json'),
        siteInfo: require('./expectations/site-info.json')
    },
    product: {
        model: {
            all: require('./expectations/all-with-model.json'),
            model: require('./expectations/model-data.json'),
            offers: require('./expectations/offers-data.json'),
            urls: require('./expectations/urls-with-model.json'),
            mainOffer: require('./expectations/main-offer-data.json')
        },
        offersAndAge: require('./expectations/all-offers-and-age.json'),
        offersAndWarning: require('./expectations/all-offers-and-warning.json')
    },
    clothess: require('./expectations/clothes.json')
};

describe('response-parser', () => {
    beforeEach(() => {
        window.document.domain = 'www.ozon.ru';
    });

    afterEach(() => {
        window.document.domain = '';
    });

    describe('detection', () => {
        it('should detect product response with model', () => {
            expect(responseParser.isProductResponse(responses.modelOffers)).toEqual(true);
            expect(responseParser.isClothesResponse(responses.modelOffers)).toEqual(false);
        });
    });

    describe('common info', () => {
        it('should extract ab data', () => {
            expect(responseParser.getAbInfo(responses.modelOffers)).toEqual(expectations.common.ab);
        });

        it('should extract default app settings', () => {
            expect(responseParser.getAppSettings(responses.modelOffers))
                .toEqual(expectations.common.appSettings);
        });

        it('should extract shop-info', () => {
            expect(responseParser.getShopInfo(responses.modelOffers)).toEqual(expectations.common.shopInfo);
        });

        it('should extract site-info', () => {
            const siteInfoFields = Object.keys(responseParser.getSiteInfo());

            expect(siteInfoFields).toEqual(Object.keys(expectations.common.siteInfo));
        });
    });

    describe('product offers with model', () => {
        it('should extract model info', () => {
            expect(responseParser.getModel(responses.modelOffers))
                .toEqual(expectations.product.model.model);
        });

        it('should extract offers data', () => {
            expect(responseParser.getPopupOffersInfo(responses.modelOffers))
                .toEqual(expectations.product.model.offers);
        });

        it('should extract product urls', () => {
            expect(responseParser.getProductUrls(responses.modelOffers))
                .toEqual(expectations.product.model.urls);
        });

        it('should extract all info from response', () => {
            expect(responseParser.getParseResult(responses.modelOffers))
                .toEqual(expectations.product.model.all);
        });
    });

    describe('product offers without model', () => {
        it('should parse response with age badge', () => {
            expect(responseParser.getParseResult(responses.offersAndAge))
                .toEqual(expectations.product.offersAndAge);
        });

        it('should parse response with warning', () => {
            expect(responseParser.getParseResult(responses.offersAndWarning))
                .toEqual(expectations.product.offersAndWarning);
        });
    });

    describe('clothes offers', () => {
        it('should parse clothes response', () => {
            document.domain = 'www.wildberries.ru';
            expect(responseParser.getParseResult(responses.clothes))
                .toEqual(expectations.clothess);
        });
    });
});
