import {mockLocation} from '@self/root/src/helpers/testament/mock';
import {makeMirrorTouch} from '@self/root/src/helpers/testament/mirror';

import {baseMockFunctionality} from './mockFunctionality';
import {
    emitAllowedTestCase,
    emitRestrictedTestCase,
    spendAllowedTestCase,
    spendRestrictedTestCase,
    hasCorrectCheckoutSummaryForValidSubscription,
    hasCorrectCheckoutSummaryForInvalidSubscription,
    makeCheckForSummaryAdditionalValues,
} from './testCases';

/** @type {Mirror} */
let mirror;
/** @type {JestLayer} */
let jestLayer;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;

beforeAll(async () => {
    mockLocation();
    mirror = await makeMirrorTouch({
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
});

afterAll(() => {
    mirror.destroy();
});

describe('CheckoutSummary', () => {
    /**
     * @expFlag all_station-subscription
     * @ticket MARKETFRONT-57855
     * @start
     */
    // Расскип https://st.yandex-team.ru/MARKETFRONT-73016
    // eslint-disable-next-line market/ginny/no-skip
    describe('Покупка товара по подписке', () => {
        beforeEach(() => baseMockFunctionality(jestLayer));
        // bluemarket-4166
        test('Правильное вью валидного заказа', () => hasCorrectCheckoutSummaryForValidSubscription(jestLayer, apiaryLayer, mandrelLayer));
        // bluemarket-4166
        test('Правильное вью невалидного заказа', () => hasCorrectCheckoutSummaryForInvalidSubscription(jestLayer, apiaryLayer, mandrelLayer));
    });
    /**
     * @expFlag all_station-subscription
     * @ticket MARKETFRONT-57855
     * @end
     */

    // Расскип https://st.yandex-team.ru/MARKETFRONT-73016
    // eslint-disable-next-line market/ginny/no-skip
    describe('Кэшбэк', () => {
        beforeEach(() => baseMockFunctionality(jestLayer));
        describe('Выбрано Накопление', () => {
            test('Накопление доступно', () => emitAllowedTestCase(jestLayer, apiaryLayer, mandrelLayer));

            test('Накопление не доступно', () => emitRestrictedTestCase(jestLayer, apiaryLayer, mandrelLayer));
        });

        describe('Выбрано Списание', () => {
            test('Списание доступно', () => spendAllowedTestCase(jestLayer, apiaryLayer, mandrelLayer));

            test('Списание не доступно', () => spendRestrictedTestCase(jestLayer, apiaryLayer, mandrelLayer));
        });
    });

    describe('Доп услуги', () => {
        describe('Стоимость', () => {
            describe('отображается', () => {
                test('для доставки лифтом', () => makeCheckForSummaryAdditionalValues(jestLayer, apiaryLayer, mandrelLayer, {
                    isVisible: true,
                    liftingType: 'ELEVATOR',
                    price: '1 000',
                }));
                test('для доставки грузовым лифтом', () => makeCheckForSummaryAdditionalValues(jestLayer, apiaryLayer, mandrelLayer, {
                    isVisible: true,
                    liftingType: 'CARGO_ELEVATOR',
                    price: '100',
                }));
                test('для доставки без лифта', () => makeCheckForSummaryAdditionalValues(jestLayer, apiaryLayer, mandrelLayer, {
                    isVisible: true,
                    liftingType: 'MANUAL',
                    price: '100 000',
                }));
            });
            describe('не отображается', () =>
                test('если не выбран параметр подъема на этаж', () =>
                    makeCheckForSummaryAdditionalValues(jestLayer, apiaryLayer, mandrelLayer, {
                        isVisible: false,
                    })));
        });
    });
});
