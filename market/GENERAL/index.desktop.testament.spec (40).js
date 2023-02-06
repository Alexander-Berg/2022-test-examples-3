import {mockLocation} from '@self/root/src/helpers/testament/mock';
import {makeMirrorDesktop} from '@self/root/src/helpers/testament/mirror';

import {
    baseMockFunctionality,
    mockPaymentSystemExtraCashbackAvailable,
    mockPaymentSystemExtraCashbackRestricted,
    mockSelectNotPromotionalPaymentSystem,
    mockPaymentSystemExtraCashbackAvailableWithSpendCashbackOption,
} from './mockFunctionality';
import {
    checkBlockWithInfoAboutAdditionalCashback,
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
});

afterAll(() => {
    mirror.destroy();
});

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
            'Действует дополнительный кешбэк 10%'
        ));

        describe('Не отображается', () => {
            test('при недоступном кэшбэке', () => checkBlockWithInfoAboutAdditionalCashback(
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
});
