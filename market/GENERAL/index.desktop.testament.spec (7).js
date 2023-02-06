import {assign} from 'ambar';
import {waitFor} from '@testing-library/dom';
import {
    createProduct,
    createOffer,
    mergeState,
} from '@yandex-market/kadavr/mocks/Report/helpers';

import {buildUrl} from '@self/project/src/utils/router';
import {mockLocation} from '@self/root/src/helpers/testament/mock';
import {makeMirrorDesktop} from '@self/root/src/helpers/testament/mirror';

import RatingStars from '@self/root/src/components/RatingStars/__pageObject';

import EmptyProductReviews from '../__pageObject';
import {guruMock, offerMock} from './__mocks__/index.mock';

const widgetPath = '../';

/** @type {Mirror} */
let mirror;
/** @type {JestLayer} */
let jestLayer;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;

const mock = guruMock.mock;

async function makeContext(user = {}) {
    return mandrelLayer.initContext({
        user,
        request: {
            productSlug: mock.slug,
            productId: mock.id,
        },
    });
}


beforeAll(async () => {
    mockLocation();
    mirror = await makeMirrorDesktop({
        jest: {
            testFilename: __filename,
            jestObject: jest,
        },
        kadavr: {
            skipLayer: true,
        },
    });
    jestLayer = mirror.getLayer('jest');
    mandrelLayer = mirror.getLayer('mandrel');
    apiaryLayer = mirror.getLayer('apiary');


    const product = createProduct(mock, mock.id);
    const offer = createOffer(assign({
        benefit: {
            type: 'recommended',
            description: 'Хорошая цена от надёжного магазина',
            isPrimary: true,
        },
    }, offerMock), offerMock.wareId);

    const state = mergeState([product, offer, {
        data: {
            search: {
                totalOffersBeforeFilters: 2,
            },
        },
    }]);

    await jestLayer.runCode((state, product, productResolvers, productReviews, platformProductResolvers) => {
        jest.doMock(platformProductResolvers, () => ({
            __esModule: true,
            checkIfProductMoved: jest.fn().mockReturnValue(product),
            getProductIdForReview: jest.fn().mockReturnValue(product.id),
        }));
        jest.doMock(productResolvers, () => ({
            __esModule: true,
            resolveRawProductsByIdOnly: jest.fn().mockResolvedValue(state),
        }));
        jest.doMock(productReviews, () => ({
            __esModule: true,
            resolveProductReviewForCurrentUser: jest.fn().mockResolvedValue({result: [], collections: {}}),
            resolveProductReviewAverageGradeForCurrentUser: jest.fn().mockResolvedValue(3),
        }));
    }, [
        state,
        product.collections.product['12345'],
        require.resolve('@self/project/src/resolvers/product'),
        require.resolve('@self/platform/resolvers/reviews/product'),
        require.resolve('@self/platform/resolvers/products'),
    ]);
});

afterAll(() => {
    mirror.destroy();
});

describe('Блок "Нет отзывов".', () => {
    describe('Пользователь авторизован.', () => testWidget(true));
    describe('Пользователь не авторизован.', () => testWidget());
});

function testWidget(isAuth = false) {
    beforeEach(async () => {
        await makeContext({isAuth});
    });
    test('Кнопка "Оставить отзыв" cодержит корректную ссылку на страницу создания отзыва', async () => {
        const expectedUrl = buildUrl('market:product-reviews-add', {
            productId: mock.id,
            slug: mock.slug,
            retpath: 'http%3A%2F%2Flocalhost%2F',
        });

        const {container} = await apiaryLayer.mountWidget(widgetPath);
        const addReviewButton = container.querySelector(EmptyProductReviews.addReviewButton);
        const href = addReviewButton.getAttribute('href');
        return expect(href).toEqual(expectedUrl);
    });
    describe('Сохранение оценки при клике на звезду', () => {
        test('Звёзды рейтинга по умолчанию должны отображать закрашенные звёзды в соответствии с рейтингом', async () => {
            const {container} = await apiaryLayer.mountWidget(widgetPath);
            const widget = container.querySelector(EmptyProductReviews.root);
            const ratingStars = container.querySelector(RatingStars.root);
            await waitFor(() => {
                expect(widget).toBeVisible();
                expect(ratingStars).toBeVisible();
            });
            await step('Устанавливаем рейтинг', async () => {
                const ratingButton = container.querySelector(RatingStars.rate);
                ratingButton.click();
            });
            await step('Отображаются закрашенные звёзды в соответствии с рейтингом', async () => {
                const rating = Number(ratingStars.getAttribute('data-rate'));
                expect(rating).toEqual(3);
            });
        });
        test('Заголовок рейтинга cодержит правильный текст', async () => {
            const {container} = await apiaryLayer.mountWidget(widgetPath);
            const widget = container.querySelector(EmptyProductReviews.root);
            const ratingStars = container.querySelector(RatingStars.root);
            await waitFor(() => {
                expect(widget).toBeVisible();
                expect(ratingStars).toBeVisible();
            });
            await step('Устанавливаем рейтинг', async () => {
                const ratingButton = container.querySelector(RatingStars.rate);
                ratingButton.click();
            });
            await step('Заголовок рейтинга содержит правильный текст', async () => {
                const ratingHeading = container.querySelector(EmptyProductReviews.ratingHeading);
                expect(ratingHeading.textContent).toEqual('Спасибо, оценка принята');
            });
        });
    });
    describe('Сохранение оценки после перезагрузки', () => {
        test('Звёзды рейтинга по умолчанию должны отображать закрашенные звёзды в соответствии с рейтингом', async () => {
            const {container} = await apiaryLayer.mountWidget(widgetPath);
            const widget = container.querySelector(EmptyProductReviews.root);
            const ratingStars = container.querySelector(RatingStars.root);
            await waitFor(() => {
                expect(widget).toBeVisible();
                expect(ratingStars).toBeVisible();
            });
            await step('Устанавливаем рейтинг', async () => {
                const ratingButton = container.querySelector(RatingStars.rate);
                ratingButton.click();
            });
            await step('Обновляем страницу', async () => {
                window.location.reload();
                await waitFor(() => {
                    expect(window.location.reload).toHaveBeenCalled();
                });
            });
            await step('Отображаются закрашенные звёзды в соответствии с рейтингом', async () => {
                const rating = Number(ratingStars.getAttribute('data-rate'));
                expect(rating).toEqual(3);
            });
        });
        test('Заголовок рейтинга cодержит правильный текст', async () => {
            const {container} = await apiaryLayer.mountWidget(widgetPath);
            const widget = container.querySelector(EmptyProductReviews.root);
            const ratingStars = container.querySelector(RatingStars.root);
            await waitFor(() => {
                expect(widget).toBeVisible();
                expect(ratingStars).toBeVisible();
            });
            await step('Устанавливаем рейтинг', async () => {
                const ratingButton = container.querySelector(RatingStars.rate);
                ratingButton.click();
            });
            await step('Обновляем страницу', async () => {
                window.location.reload();
                await waitFor(() => {
                    expect(window.location.reload).toHaveBeenCalled();
                });
            });
            await step('Заголовок рейтинга содержит правильный текст', async () => {
                const ratingHeading = container.querySelector(EmptyProductReviews.ratingHeading);
                expect(ratingHeading.textContent).toEqual('Спасибо, оценка принята');
            });
        });
    });
}
