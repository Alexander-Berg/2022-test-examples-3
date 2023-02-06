import {mockLocation} from '@self/root/src/helpers/testament/mock';
import {makeMirrorTouch} from '@self/root/src/helpers/testament/mirror';

import {
    baseMockFunctionality,
    mockPaymentSystemExtraCashbackAvailable,
    mockPaymentSystemExtraCashbackRestricted,
    mockSelectNotPromotionalPaymentSystem,
    mockPaymentSystemExtraCashbackAvailableWithSpendCashbackOption,
    mockPaymentSystemMastercardPromoBannerDisplayed,
    mockPaymentSystemMastercardPromoBannerWithLargePromoCashback,
    mockPaymentSystemMirPromoBannerDisplayed,
    mockPaymentSystemMirPromoBannerWithLargePromoCashback,
    mockPaymentSystemPromoBannerWithPromoRestricted,
    mockPaymentSystemPromoBannerWithSelectPromoPaymentSystem,
    mockPaymentSystemPromoBannerWithSpendCashbackOption,
} from './mockFunctionality';
import {
    checkBlockWithInfoAboutAdditionalCashback,
    checkPaymentSystemPromoBanner,
} from './commonTestCases';
import {
    checkPromoPaymentSystemIndicator,
} from './testCases/';

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

// Расскип https://st.yandex-team.ru/MARKETFRONT-73016
// eslint-disable-next-line market/ginny/no-skip
describe('EditPaymentOption', () => {
    beforeAll(async () => {
        await baseMockFunctionality(jestLayer);
    });

    describe('Информация о дополнительном кэшбэке', () => {
        // SKIPPED MARKETFRONT-96354
        // eslint-disable-next-line jest/no-disabled-tests
        test.skip('Отображается', () => checkBlockWithInfoAboutAdditionalCashback(
            jestLayer,
            apiaryLayer,
            mandrelLayer,
            mockPaymentSystemExtraCashbackAvailable,
            ' 10%'
        ));

        describe('Не отображается', () => {
            // SKIPPED MARKETFRONT-96354
            // eslint-disable-next-line jest/no-disabled-tests
            test.skip('при недоступном кэшбэке', () => checkBlockWithInfoAboutAdditionalCashback(
                jestLayer,
                apiaryLayer,
                mandrelLayer,
                mockPaymentSystemExtraCashbackRestricted
            ));

            test('выбран не акционный тип платежной системы', () => checkBlockWithInfoAboutAdditionalCashback(
                jestLayer,
                apiaryLayer,
                mandrelLayer,
                mockSelectNotPromotionalPaymentSystem
            ));

            test('выбрана опция списания кэшбэка', () => checkBlockWithInfoAboutAdditionalCashback(
                jestLayer,
                apiaryLayer,
                mandrelLayer,
                mockPaymentSystemExtraCashbackAvailableWithSpendCashbackOption
            ));
        });
    });

    describe('Индикатор акционной платежной системы', () => {
        test('Отображается', () => checkPromoPaymentSystemIndicator(
            jestLayer,
            apiaryLayer,
            mandrelLayer,
            mockPaymentSystemExtraCashbackAvailable,
            true
        ));

        describe('Не отображается', () => {
            test('при недоступном кэшбэке', () => checkPromoPaymentSystemIndicator(
                jestLayer,
                apiaryLayer,
                mandrelLayer,
                mockPaymentSystemExtraCashbackRestricted
            ));

            test('выбран не акционный тип платежной системы', () => checkPromoPaymentSystemIndicator(
                jestLayer,
                apiaryLayer,
                mandrelLayer,
                mockSelectNotPromotionalPaymentSystem
            ));

            test('выбрана опция списания кэшбэка', () => checkPromoPaymentSystemIndicator(
                jestLayer,
                apiaryLayer,
                mandrelLayer,
                mockPaymentSystemExtraCashbackAvailableWithSpendCashbackOption
            ));
        });
    });

    describe('Акционный информер', () => {
        describe('Отображается для mastercard', () => {
            test('по умолчанию', () => checkPaymentSystemPromoBanner(
                jestLayer,
                apiaryLayer,
                mandrelLayer,
                mockPaymentSystemMastercardPromoBannerDisplayed,
                'Оплатите заказ картой Mastercard  ® онлайн и получите дополнительно  500 баллов'
            ));

            test('сумма кэшбэка превышает максимального значения по акции', () => checkPaymentSystemPromoBanner(
                jestLayer,
                apiaryLayer,
                mandrelLayer,
                mockPaymentSystemMastercardPromoBannerWithLargePromoCashback,
                'Оплатите заказ картой Mastercard  ® онлайн и получите дополнительно  2 000 баллов'
            ));
        });

        describe('Отображается для mir', () => {
            test('по умолчанию', () => checkPaymentSystemPromoBanner(
                jestLayer,
                apiaryLayer,
                mandrelLayer,
                mockPaymentSystemMirPromoBannerDisplayed,
                'Оплатите заказ картой  онлайн и получите дополнительно  500 баллов'
            ));

            test('сумма кэшбэка превышает максимального значения по акции', () => checkPaymentSystemPromoBanner(
                jestLayer,
                apiaryLayer,
                mandrelLayer,
                mockPaymentSystemMirPromoBannerWithLargePromoCashback,
                'Оплатите заказ картой  онлайн и получите дополнительно  2 000 баллов'
            ));
        });

        describe('Не отображается', () => {
            test('при недоступном кэшбэке', () => checkPaymentSystemPromoBanner(
                jestLayer,
                apiaryLayer,
                mandrelLayer,
                mockPaymentSystemPromoBannerWithPromoRestricted
            ));

            test('выбран акционный тип платежной системы', () => checkPaymentSystemPromoBanner(
                jestLayer,
                apiaryLayer,
                mandrelLayer,
                mockPaymentSystemPromoBannerWithSelectPromoPaymentSystem
            ));

            test('выбрана опция списания кэшбэка', () => checkPaymentSystemPromoBanner(
                jestLayer,
                apiaryLayer,
                mandrelLayer,
                mockPaymentSystemPromoBannerWithSpendCashbackOption
            ));
        });
    });
});
