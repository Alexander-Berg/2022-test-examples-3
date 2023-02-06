import {makeMirrorTouch} from '@self/root/src/helpers/testament/mirror';

import {
    createProduct,
    createOffer,
    mergeState,
} from '@yandex-market/kadavr/mocks/Report/helpers';

import {
    // selectOfferModelLinkInShop,
    selectOfferShopEncryptedLink,
} from '@self/platform/entities/offer/selectors';

import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';
import {PackedFunction} from '@yandex-market/testament/mirror';

import {CURRENCY_LABEL} from '@self/root/src/entities/currency/constants';
import DefaultOffer from '@self/platform/spec/page-objects/components/DefaultOffer';
import {
    OFFER_WITH_DELIVERY,
    OFFER_ID,
    PRODUCT,
    PRODUCT_ID,
    PRODUCT_PROMISE_MOCK,
    DEFAULT_OFFER_PROMISE_MOCK,
    PAGE_STATE_PROMISE_MOCK,
} from './__mock__/paymentSystemCashbackMock';

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
    [PRODUCT_PROMISE_MOCK, DEFAULT_OFFER_PROMISE_MOCK, PAGE_STATE_PROMISE_MOCK]
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

    const product = createProduct(PRODUCT, PRODUCT_ID);
    const offer = createOffer(OFFER_WITH_DELIVERY, OFFER_ID);

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

describe('Дефолтный оффер. (CPC)', () => {
    // m-touch-3434
    // https://testpalm.yandex-team.ru/m-touch/testcases/3434
    // SKIPPED MARKETFRONT-96354
    // eslint-disable-next-line jest/no-disabled-tests
    describe.skip('Кликаут', () => {
        it('Кнопка "В магазин" Имеет корректную ссылку', async () => {
            const userTld = 'ru';
            await makeContext({}, {
                isAuth: true,
                tld: userTld,
            });

            const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, widgetOptions);
            const clickoutLink = container.querySelector(DefaultOffer.clickoutLink);

            const correctLink = selectOfferShopEncryptedLink(OFFER_WITH_DELIVERY, userTld);
            const correctUrl = buildUrl(correctLink.path, correctLink.params);

            expect(clickoutLink).toHaveAttribute('href', expect.stringMatching(correctUrl));
        });

        it('Название магазина Имеет корректную ссылку', async () => {
            const userTld = 'ru';
            await makeContext({}, {
                isAuth: true,
                tld: userTld,
            });

            const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, widgetOptions);
            const shopNameLink = container.querySelector(DefaultOffer.shopNameLink);

            const correctLink = selectOfferShopEncryptedLink(OFFER_WITH_DELIVERY, userTld);
            const correctUrl = buildUrl(correctLink.path, correctLink.params);

            expect(shopNameLink).toHaveAttribute('href', expect.stringMatching(correctUrl));
        });
    });

    describe('Содержимое', () => {
        // m-touch-1784
        // https://testpalm.yandex-team.ru/m-touch/testcases/1784
        describe('Блок доставки. ', () => {
            // SKIPPED MARKETFRONT-96354
            // eslint-disable-next-line jest/no-disabled-tests
            it.skip('По умолчанию должен присутствовать.', async () => {
                await makeContext({}, {isAuth: true});
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, widgetOptions);

                const rootElement = container.querySelector(DefaultOffer.delivery);
                expect(rootElement).not.toBeNull();
            });

            it('Должен иметь корректный текст.', async () => {
                await makeContext({}, {isAuth: true});
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, widgetOptions);

                const deliveryElement = container.querySelector(DefaultOffer.delivery);
                const deliveryText = deliveryElement && deliveryElement.textContent;

                // убираем пробелы чтобы не привязываться к нюансам написания
                const deliveryTextNoSpaces = (deliveryText || '').replace(/\s/g, '');

                expect(deliveryTextNoSpaces).toEqual(
                    expect.stringMatching('Самовывоз сегодня — бесплатноКурьером сегодня — бесплатно'.replace(/\s/g, ''))
                );
            });
        });

        // m-touch-1785
        // https://testpalm.yandex-team.ru/m-touch/testcases/1785
        it('Кнопка перехода в магазин. По умолчанию должна присутствовать', async () => {
            await makeContext({}, {isAuth: true});
            const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, widgetOptions);

            const offerClickoutButton = container.querySelector(DefaultOffer.offerClickoutButton);
            expect(offerClickoutButton).not.toBeNull();
        });

        // m-touch-1786
        // https://testpalm.yandex-team.ru/m-touch/testcases/1786
        it('Рейтинг магазина. По умолчанию должен присутствовать', async () => {
            await makeContext({}, {isAuth: true});
            const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, widgetOptions);

            const shopRating = container.querySelector(DefaultOffer.shopRating);
            expect(shopRating).not.toBeNull();
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
                        OFFER_WITH_DELIVERY.prices.value
                    ));
                });

                it('должна содержать корректную валюту', async () => {
                    await makeContext({}, {isAuth: true});
                    const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, widgetOptions);

                    const priceTextContent = container.querySelector(DefaultOffer.price).textContent;

                    expect(priceTextContent).toEqual(expect.stringMatching(
                        CURRENCY_LABEL[OFFER_WITH_DELIVERY.prices.currency]
                    ));
                });
            });
        });
    });
});
