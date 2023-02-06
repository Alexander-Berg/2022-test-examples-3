import {pathOrUnsafe} from 'ambar';

import {makeMirrorTouch} from '@self/root/src/helpers/testament/mirror';

import {
    createProduct,
    createOffer,
    mergeState,
} from '@yandex-market/kadavr/mocks/Report/helpers';

import {
    selectOfferModelLink,
} from '@self/platform/entities/offer/selectors';
import {toDecimalPostfixString} from '@self/project/src/helpers/number';

import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';
import {PackedFunction} from '@yandex-market/testament/mirror';

import {getSupplierName} from '@self/platform/entities/offer/getters';

import {CURRENCY_LABEL} from '@self/root/src/entities/currency/constants';
import DefaultOffer from '@self/platform/spec/page-objects/components/DefaultOffer';
import ShopInfo from '@self/platform/components/ShopInfo/__pageObject';


import cpaDefaultOfferMock from './__mock__/cpaMock/cpaDefaultOfferMock';
import cpaProductMock from './__mock__/cpaMock/cpaProductMock';
import cpaPageStateMock from './__mock__/cpaMock/cpaPageStateMock';


// путь к виджету который тестируем
const WIDGET_PATH = '../';

let mirror;
let mandrelLayer;
let apiaryLayer;
let kadavrLayer;
let jestLayer;

const widgetOptions = new PackedFunction(
    (productMock, defaultOfferMock, pageStateMock) => ({
        productPromise: Promise.resolve(productMock),
        defaultOfferPromise: Promise.resolve(defaultOfferMock),
        pageStatePromise: Promise.resolve(pageStateMock),
        isDynamic: true,
    }),
    [cpaProductMock, cpaDefaultOfferMock, cpaPageStateMock]
);

async function makeContext(
    otherParams,
    user = {},
    pageId = 'touch:index'
) {
    mandrelLayer.initContext({
        request: {
            cookie: {
                kadavr_session_id: await kadavrLayer.getSessionId(),
            },
        },
        ...(user.isAuth ? {user} : {}),
        page: {
            pageId,
        },
    });

    const product = createProduct(cpaProductMock, cpaProductMock.id);
    const offer = createOffer(cpaDefaultOfferMock, cpaDefaultOfferMock.wareId);

    await kadavrLayer.setState('report', mergeState([
        product,
        offer,
    ]));
}

const buildUrl = (pageId, params) => `${pageId}_${JSON.stringify(params)}`;

beforeAll(async () => {
    mockIntersectionObserver();
    mirror = await makeMirrorTouch({
        jest: {
            testFilename: __filename,
            jestObject: jest,
        },
        kadavr: {
            asLibrary: true,
        },
    });
    mandrelLayer = mirror.getLayer('mandrel');
    apiaryLayer = mirror.getLayer('apiary');
    kadavrLayer = mirror.getLayer('kadavr');
    jestLayer = mirror.getLayer('jest');

    await jestLayer.doMock(
        require.resolve('@self/project/src/utils/router'),
        () => ({
            buildUrl: (pageId, params) => `${pageId}_${JSON.stringify(params)}`,
        })
    );
});

afterAll(() => {
    mirror.destroy();
    jest.useRealTimers();
});

describe('Дефолтный оффер. (CPA)', () => {
    // MARKETFRONT-96354
    // eslint-disable-next-line jest/no-disabled-tests
    describe.skip('Кликаут', () => {
        // m-touch-3434 Тач.
        it('Название магазина Имеет корректную ссылку', async () => {
            const userTld = 'ru';
            await makeContext({}, {
                isAuth: true,
                tld: userTld,
            });

            const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, widgetOptions);
            const shopNameLink = container.querySelector(DefaultOffer.shopNameLink);

            const shop = cpaDefaultOfferMock.shop;
            const businessId = shop.businessId || shop.business_id;
            const correctLink = selectOfferModelLink(cpaDefaultOfferMock, businessId);
            const correctUrl = buildUrl(correctLink.path, correctLink.params);

            expect(shopNameLink).toHaveAttribute('href', expect.stringMatching(correctUrl));
        });
    });

    describe('Содержимое', () => {
        // m-touch-1784
        // https://testpalm.yandex-team.ru/m-touch/testcases/1784
        it('По умолчанию должен содержать блок доставки', async () => {
            await makeContext({}, {isAuth: true});
            const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, widgetOptions);

            const rootElement = container.querySelector(DefaultOffer.delivery);
            expect(rootElement).toBeTruthy();
        });

        // m-touch-1785
        // https://testpalm.yandex-team.ru/m-touch/testcases/1785
        describe('Кнопка "Добавить в корзину". ', () => {
            it('По умолчанию должна присутствовать.', async () => {
                await makeContext({}, {isAuth: true});
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, widgetOptions);

                const cartButton = container.querySelector(DefaultOffer.cartButton);
                expect(cartButton).not.toBeNull();
            });

            it('Должна иметь корректный текст.', async () => {
                await makeContext({}, {isAuth: true});
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, widgetOptions);

                const cartButton = container.querySelector(DefaultOffer.cartButton);
                // убираем пробелы чтобы не привязываться к нюансам написания
                const cartButtonText = cartButton.textContent.replace(/\s/g, '');

                expect(cartButtonText).toEqual(
                    expect.stringMatching('Добавитьвкорзину')
                );
            });
        });

        // m-touch-1785
        // https://testpalm.yandex-team.ru/m-touch/testcases/1785
        describe('Кнопка перехода в магазин. ', () => {
            it('По умолчанию должна присутствовать', async () => {
                await makeContext({}, {isAuth: true});
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, widgetOptions);

                const shopNameLink = container.querySelector(DefaultOffer.shopNameLink)
                    /**
                     * @expFlag touch_km-new-do-trust-rev
                     * @ticket https://st.yandex-team.ru/MARKETFRONT-71593
                     * nextLine
                     */
                    || container.querySelector(`${DefaultOffer.root} ${ShopInfo.shopNameLink}`);

                expect(shopNameLink).not.toBeNull();
            });

            it('Название магазина корректно', async () => {
                await makeContext({}, {isAuth: true});

                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, widgetOptions);
                const shopName =
                    container.querySelector(DefaultOffer.shopName)
                    /**
                     * @expFlag touch_km-new-do-trust-rev
                     * @ticket https://st.yandex-team.ru/MARKETFRONT-71593
                     * nextLine
                     */
                    || container.querySelector(`${DefaultOffer.root} ${ShopInfo.root}`);

                const shopNameText = shopName.textContent;

                const supplierName = getSupplierName(cpaDefaultOfferMock);
                const correctShopName = supplierName || cpaDefaultOfferMock.shop.name;

                expect(shopNameText).toEqual(
                    expect.stringMatching(correctShopName)
                );
            });
        });

        // m-touch-1786
        // https://testpalm.yandex-team.ru/m-touch/testcases/1786
        describe('Рейтинг магазина. ', () => {
            it('По умолчанию должен присутствовать', async () => {
                await makeContext({}, {isAuth: true});
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, widgetOptions);

                const shopRating =
                    container.querySelector(DefaultOffer.shopRating)
                    /**
                     * @expFlag touch_km-new-do-trust-rev
                     * @ticket https://st.yandex-team.ru/MARKETFRONT-71593
                     * nextLine
                     */
                    || container.querySelector(`${DefaultOffer.root} ${ShopInfo.shopRating}`);

                expect(shopRating).not.toBeNull();
            });


            it('Значение кол-ва звезд корректно', async () => {
                await makeContext({}, {isAuth: true});
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, widgetOptions);

                const shopRating =
                    container.querySelector(DefaultOffer.shopRating)
                    /**
                     * @expFlag touch_km-new-do-trust-rev
                     * @ticket https://st.yandex-team.ru/MARKETFRONT-71593
                     * nextLine
                     */
                    || container.querySelector(`${DefaultOffer.root} ${ShopInfo.shopRating}`);

                const shopRatingText = shopRating.textContent;

                const correctRatingValue = pathOrUnsafe(cpaDefaultOfferMock.shop.ratingToShow,
                    ['supplier', 'ratingToShow'], cpaDefaultOfferMock);

                const correctRatingValueProcessed = parseFloat(correctRatingValue).toFixed(1);

                expect(shopRatingText).toEqual(
                    expect.stringMatching(correctRatingValueProcessed)
                );
            });

            it('Значение кол-ва оценок корректно', async () => {
                await makeContext({}, {isAuth: true});
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, widgetOptions);

                const shopRating =
                    container.querySelector(DefaultOffer.shopRating)
                    /**
                     * @expFlag touch_km-new-do-trust-rev
                     * @ticket https://st.yandex-team.ru/MARKETFRONT-71593
                     * nextLine
                     */
                    || container.querySelector(`${DefaultOffer.root} ${ShopInfo.shopRatingReviewCount}`);

                const shopRatingText = shopRating.textContent;

                const correctGradesCount = pathOrUnsafe(cpaDefaultOfferMock.shop.newGradesCount,
                    ['supplier', 'newGradesCount'], cpaDefaultOfferMock);
                const correctGradesCountProcessed = toDecimalPostfixString(correctGradesCount);

                expect(shopRatingText).toEqual(
                    expect.stringMatching(correctGradesCountProcessed)
                );
            });
        });

        // m-touch-1783
        // https://testpalm.yandex-team.ru/m-touch/testcases/1783
        describe('Цена товарного предложения. ', () => {
            describe('По умолчанию ', () => {
                it('должна присутствовать', async () => {
                    await makeContext({}, {isAuth: true});
                    const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, widgetOptions);

                    const price = container.querySelector(DefaultOffer.price);
                    expect(price).not.toBeNull();
                });

                it('должна содержать корректное значение', async () => {
                    await makeContext({}, {isAuth: true});
                    const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, widgetOptions);

                    const priceTextContent = container.querySelector(DefaultOffer.price).textContent;
                    const priceOnlyNumbersContent = priceTextContent.match(/\d/g).join('');

                    expect(priceOnlyNumbersContent).toEqual(expect.stringMatching(
                        cpaDefaultOfferMock.prices.value
                    ));
                });

                it('должна содержать корректную валюту', async () => {
                    await makeContext({}, {isAuth: true});
                    const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, widgetOptions);

                    const priceTextContent = container.querySelector(DefaultOffer.price).textContent;

                    expect(priceTextContent).toEqual(expect.stringMatching(
                        CURRENCY_LABEL[cpaDefaultOfferMock.prices.currency]
                    ));
                });
            });
        });
    });
});
