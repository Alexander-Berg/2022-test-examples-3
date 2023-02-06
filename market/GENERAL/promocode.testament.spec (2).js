// @flow

import dayjs from 'dayjs';
import 'dayjs/locale/ru';
import {PackedFunction} from '@yandex-market/testament/mirror';

import {makeMirror} from '@self/platform/helpers/testament';
import {createProduct, createOffer, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';
import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';
import {NON_BREAKING_SPACE_CHAR as NBSP} from '@self/root/src/constants/string';
// flowlint-next-line untyped-import:off
import PromocodeInfoBlockPO from '@self/root/src/components/PromocodeInfoBlock/components/PromocodeInfoBlock/__pageObject';

import {
    OFFER,
    OFFER_ID,
    PRODUCT,
    PRODUCT_ID,
    PROMOCODE,
    PRODUCT_PROMISE_MOCK,
    DEFAULT_OFFER_PROMISE_MOCK,
    PAGE_STATE_PROMISE_MOCK,
} from './__mock__/promocodeMock.js';

dayjs.locale('ru');

/** @type {Mirror} */
let mirror;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {KadavrLayer} */
let kadavrLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;
/** @type {JestLayer} */
let jestLayer;

type TestContext = {
    exps?: { [string]: boolean },
    user?: { [string]: mixed },
};

const widgetPath = require.resolve('@self/platform/widgets/parts/DefaultOffer');
const widgetOptions = new PackedFunction(
    (productMock, defaultOfferMock, pageStateMock) => ({
        productPromise: Promise.resolve(productMock),
        defaultOfferPromise: Promise.resolve(defaultOfferMock),
        pageStatePromise: Promise.resolve(pageStateMock),
        isDynamic: true,
    }),
    [PRODUCT_PROMISE_MOCK, DEFAULT_OFFER_PROMISE_MOCK, PAGE_STATE_PROMISE_MOCK]
);

async function makeContext({exps = {}, user = {}}: TestContext = {}) {
    const cookie = {kadavr_session_id: await kadavrLayer.getSessionId()};

    return mandrelLayer.initContext({
        user: {
            ...user,
        },
        request: {
            cookie,
            params: {
                productId: PRODUCT_ID,
            },
            abt: {
                expFlags: exps || {},
            },
        },
    });
}

const setReportState = async () => {
    const product = createProduct(PRODUCT, PRODUCT_ID);
    const offer = createOffer(OFFER, OFFER_ID);

    const reportState = mergeState([
        product,
        offer,
    ]);

    await kadavrLayer.setState('report', reportState);
};

const getPromocodeText = promocode => {
    const percent = promocode.discount.value;
    const promoCodeName = promocode.itemsInfo.promoCode;
    return `−${percent}% по${NBSP}промокоду${NBSP}${promoCodeName}`;
};

describe('Widget: DefaultOffer', () => {
    beforeAll(async () => {
        mockIntersectionObserver();
        mirror = await makeMirror({
            jest: {
                testFilename: __filename,
                jestObject: jest,
            },
            kadavr: {
                asLibrary: true,
            },
        });
        mandrelLayer = mirror.getLayer('mandrel');
        kadavrLayer = mirror.getLayer('kadavr');
        apiaryLayer = mirror.getLayer('apiary');
        jestLayer = mirror.getLayer('jest');
    });

    afterAll(() => {
        mirror.destroy();
        jest.useRealTimers();
    });

    describe('Оффер с промокодом на товар', () => {
        beforeAll(async () => {
            await setReportState();
            await jestLayer.runCode(() => {
                jest.mock(
                    '@self/platform/widgets/pages/ProductPage',
                    () => ({SKU_ID_QUERY_PARAM: 'sku'})
                );
            }, []);
        });

        // SKIPPED MARKETFRONT-96354
        // eslint-disable-next-line jest/no-disabled-tests
        describe.skip('Текст в блок с промокодом', () => {
            // marketfront-5922
            test('Отображается корректно', async () => {
                // Arrange
                await makeContext();

                // Act
                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                // Assert
                const promocodeInfoBlock = container.querySelector(PromocodeInfoBlockPO.rootCopy);
                const text = getPromocodeText(PROMOCODE);
                expect(promocodeInfoBlock.textContent).toBe(text);
            });
        });

        // MARKETFRONT-96354
        // eslint-disable-next-line jest/no-disabled-tests
        describe.skip('Блок с текстом условий', () => {
            // marketfront-5922
            test('Не отображается', async () => {
                // Arrange
                await makeContext();

                // Act
                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                // Assert
                const conditions = container.querySelector(PromocodeInfoBlockPO.conditions);
                expect(conditions).toBeFalsy();
            });
        });

        describe('Иконка копирования', () => {
            // marketfront-5922
            test('Отображается', async () => {
                // Arrange
                await makeContext();

                // Act
                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                // Assert
                const copyIcon = container.querySelector(PromocodeInfoBlockPO.copyIcon);
                expect(copyIcon).toBeTruthy();
            });
        });
    });
});
