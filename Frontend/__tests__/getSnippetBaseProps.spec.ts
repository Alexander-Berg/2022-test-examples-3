import { IProduct, IOffer, IKnownThumbnails, IReportPicture } from '@yandex-turbo/applications/beru.ru/interfaces';
import { buildUrl } from '@yandex-turbo/applications/beru.ru/router';
import { Pages } from '@yandex-turbo/applications/beru.ru/router/routes';
import { getSnippetBaseProps } from '../getSnippetBaseProps';
import * as commonHelpers from '../entities';
import * as priceHelpers from '../price';
import * as offerHelpers from '../entities/offer';
import * as thumbnailHelpers from '../thumbnails';
import * as analytics from '../analytics';

jest.mock('@yandex-turbo/applications/beru.ru/router', () => ({
    buildUrl: jest.fn(() => 'https://test.test'),
}));

describe('getSnippetBaseProps', () => {
    // "Любая" сущность(тут в качесве мока юзается продукт)
    const entity = <IProduct>{
        entity: 'product',
    };
    // Декларация картинки
    const picture = <IReportPicture> {
        entity: 'picture',
    };
    const offer = <IOffer>{
        entity: 'offer',
        titles: {
            raw: 'Бритва Philips',
        },
        slug: 'tovar',
        prices: {
            currency: 'RUB',
        },
        pictures: [
            { ...picture },
        ],
    };
    // Декларация тубм
    const knownThumbnail = <IKnownThumbnails>{
        namespace: 'mpic',
        thumbnails: [],
    };
    let goalParams: ReturnType<typeof analytics.makeGoalParamsFromOffer>;
    // Ф-ии которые вызываются внутри тестируемой
    let getSafeOffer: ReturnType<typeof jest.spyOn>;
    let getStats: ReturnType<typeof jest.spyOn>;
    let getPice: ReturnType<typeof jest.spyOn>;
    let getSkuId: ReturnType<typeof jest.spyOn>;
    let getSafePicture: ReturnType<typeof jest.spyOn>;
    let convertPictureToThumbnail: ReturnType<typeof jest.spyOn>;
    let getAddToCartLink: ReturnType<typeof jest.spyOn>;
    let makeGoalParamsFromOffer: ReturnType<typeof jest.spyOn>;

    beforeEach(() => {
        goalParams = {
            skuId: '123',
            price: 150,
            oldPrice: undefined,
            offerId: '321',
            showPlaceId: '333',
            marketSkuCreator: 'market',
        };
        getSafeOffer = jest.spyOn(commonHelpers, 'getSafeOffer').mockReturnValue(offer);
        getStats = jest.spyOn(commonHelpers, 'getStats').mockReturnValue({
            opinions: 10,
            rating: 20,
            ratingCount: 30,
        });
        getPice = jest.spyOn(priceHelpers, 'getPrice').mockReturnValue({
            price: 135,
            oldPrice: 150,
            percent: 10,
        });
        getSkuId = jest.spyOn(offerHelpers, 'getSkuId').mockReturnValue('1101');
        getSafePicture = jest.spyOn(thumbnailHelpers, 'getSafePicture').mockReturnValue({
            ...picture,
            knownThumbnails: [knownThumbnail],
        });
        convertPictureToThumbnail = jest.spyOn(thumbnailHelpers, 'convertPictureToThumbnail').mockReturnValue({
            src: 'http://path/to/pic',
            srcSet: 'http://path/to/pic 1x, http://path/to/pic 2x',
        });
        getAddToCartLink = jest.spyOn(commonHelpers, 'getAddToCartLink').mockReturnValue('https://add-to-cart.test');
        makeGoalParamsFromOffer = jest.spyOn(analytics, 'makeGoalParamsFromOffer').mockReturnValue(goalParams);
    });

    afterEach(() => {
        getSafeOffer.mockClear();
        getStats.mockClear();
        getPice.mockClear();
        getSkuId.mockClear();
        getSafePicture.mockClear();
        convertPictureToThumbnail.mockClear();
        getAddToCartLink.mockClear();
        makeGoalParamsFromOffer.mockClear();
        buildUrl.mockClear();
    });

    it('должна возвращать undefined если офера нет', () => {
        const getSafeOffer = jest.spyOn(commonHelpers, 'getSafeOffer').mockReturnValue(undefined);

        expect(getSnippetBaseProps(entity, [knownThumbnail])).toBeUndefined();
        expect(getSafeOffer).toHaveBeenCalledTimes(1);
    });

    it('должна возвращать пропсы для BeruSnippetAdapter, который можно построить по данным', () => {
        // Проверяем что возвращаемый ответ корректный
        expect(getSnippetBaseProps(entity, [knownThumbnail])).toEqual({
            skuId: '1101',
            slug: 'tovar',
            title: 'Бритва Philips',
            price: 135,
            oldPrice: 150,
            discountPercent: 10,
            picture: {
                src: 'http://path/to/pic',
                srcSet: 'http://path/to/pic 1x, http://path/to/pic 2x',
            },
            opinions: 10,
            rating: 20,
            ratingCount: 30,
        });
        // Проверяем что ф-ия вызвана с верным аргументом сущности
        expect(getSafeOffer).toHaveBeenCalledWith(entity);
        // Проверяем что в ф-ию передается верное поле цены из офера
        expect(getPice).toHaveBeenCalledWith(offer.prices);
        // Проверяем что skuId извлекается из офера
        expect(getSkuId).toHaveBeenCalledWith(offer);
        // Проверяем что передаются декларации картинок из офера и тумбы
        expect(getSafePicture).toHaveBeenCalledWith([knownThumbnail], offer.pictures);
        // Проверяем что по умолчанию в конвертер не передаются опции о размере картинки
        expect(convertPictureToThumbnail).toHaveBeenCalledWith({
            ...picture,
            knownThumbnails: [knownThumbnail],
        }, undefined, undefined);
        // Проверяем что статистика по оферу берется из входной сущности
        expect(getStats).toHaveBeenCalledWith(entity);
        // проверяем что ссылка на товар не строится(опция не передана)
        expect(buildUrl).not.toHaveBeenCalled();
        // проверяем что ссылка не добавление товара в корзину не строится(опция не передана)
        expect(getAddToCartLink).not.toHaveBeenCalled();
    });

    describe('Дополнительные опции', () => {
        it('значения из опции "picture", должны пробрасываться в convertPictureToThumbnail', () => {
            expect(getSnippetBaseProps(entity, [knownThumbnail], { picture: { size: 200, onlySquare: true } })).toEqual({
                skuId: '1101',
                slug: 'tovar',
                title: 'Бритва Philips',
                price: 135,
                oldPrice: 150,
                discountPercent: 10,
                picture: {
                    src: 'http://path/to/pic',
                    srcSet: 'http://path/to/pic 1x, http://path/to/pic 2x',
                },
                opinions: 10,
                rating: 20,
                ratingCount: 30,
            });
            expect(convertPictureToThumbnail).toHaveBeenCalledWith({
                ...picture,
                knownThumbnails: [knownThumbnail],
            }, 200, true);
        });

        it('если предана опция url, то должна строится ссылка на товар', () => {
            const props = getSnippetBaseProps(entity, [knownThumbnail], { picture: { size: 200, onlySquare: true }, url: true });

            expect(buildUrl).toHaveBeenCalledWith(
                Pages.SKU,
                expect.objectContaining({ skuId: '1101', slug: 'tovar' }),
                expect.objectContaining({ turboLink: true })
            );
            expect(props).toMatchObject({ url: 'https://test.test' });
        });

        it('если передана опция addToCartLink, то должна строится ссылка на добавление товара в корзину', () => {
            const props = getSnippetBaseProps(entity, [knownThumbnail], { picture: { size: 200, onlySquare: true }, addToCartLink: true });

            expect(getAddToCartLink).toHaveBeenCalledWith(offer);
            expect(props).toMatchObject({
                addToCartButton: {
                    url: 'https://add-to-cart.test',
                },
            });
        });

        it('если передана опция addToCartMetrika вместе с addToCartLink, то должна строиться соответствующая цель для отправки в метрику', () => {
            const props = getSnippetBaseProps(entity, [knownThumbnail], { picture: { size: 200, onlySquare: true }, addToCartLink: true, addToCartMetrika: true });

            expect(makeGoalParamsFromOffer).toHaveBeenCalledTimes(1);
            expect(makeGoalParamsFromOffer).toHaveBeenCalledWith(offer);
            expect(props).toMatchObject({
                addToCartButton: expect.objectContaining({
                    yaGoals: {
                        '47628343': [{
                            name: 'ADD-TO-CART',
                            id: '47628343',
                            params: goalParams,
                        }],
                        '44910898': [{
                            name: 'ADD-TO-CART',
                            id: '44910898',
                            params: goalParams,
                        }],
                    },
                }),
            });
        });

        it('если передана опция visibilityMetrika, то должна строится соответствующая цель для отправки в метрику', () => {
            const props = getSnippetBaseProps(entity, [knownThumbnail], { picture: { size: 200, onlySquare: true }, visibilityMetrika: true });

            expect(makeGoalParamsFromOffer).toHaveBeenCalledTimes(1);
            expect(makeGoalParamsFromOffer).toHaveBeenCalledWith(offer);
            expect(props).toMatchObject({
                visibilityGoal: [
                    {
                        id: '47628343',
                        name: 'SNIPPET-VISIBLE',
                        params: goalParams,
                    },
                    {
                        id: '44910898',
                        name: 'SNIPPET-VISIBLE',
                        params: goalParams,
                    },
                ],
            });
        });

        it('если передана опция navigateMetrika, то должна строится соответствующая цель для отправки в метрику', () => {
            const props = getSnippetBaseProps(entity, [knownThumbnail], { picture: { size: 200, onlySquare: true }, navigateMetrika: true });

            expect(makeGoalParamsFromOffer).toHaveBeenCalledTimes(1);
            expect(makeGoalParamsFromOffer).toHaveBeenCalledWith(offer);
            expect(props).toMatchObject({
                navigateGoal: {
                    '47628343': [{
                        name: 'SNIPPET-NAVIGATE',
                        id: '47628343',
                        params: goalParams,
                    }],
                },
            });
        });
    });
});
