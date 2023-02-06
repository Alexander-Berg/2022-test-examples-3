import {mockLocation} from '@self/root/src/helpers/testament/mock';
import {makeMirrorDesktop} from '@self/root/src/helpers/testament/mirror';

import {mirCard, masterCard} from './mockData';
import {
    baseMockFunctionality,
    mockPaymentSystemPromoRestricted,
    mockSelectPromoPaymentSystem,
    mockSpendCashbackOption,
    mockPaymentSystemMastercardPromoAllowed,
    mockPaymentSystemMastercardPromoAllowedWithLargePromoCashback,
    mockPaymentSystemMirPromoAllowed,
    mockPaymentSystemMirPromoAllowedWithLargePromoCashback,
} from './mockFunctionality';
import {
    checkPaymentSystemPromoBannerNotShown,
    checkPaymentSystemPromoBannerContent,
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

    await baseMockFunctionality(jestLayer);
});

afterAll(() => {
    mirror.destroy();
});

describe('PaymentSystemPromoBanner', () => {
    describe('Не отображается', () => {
        test('при недоступном кэшбэке', async () => {
            await checkPaymentSystemPromoBannerNotShown(
                jestLayer,
                apiaryLayer,
                mandrelLayer,
                mockPaymentSystemPromoRestricted,
                mirCard
            );
        });

        test('выбран акционный тип платежной системы', async () => {
            await checkPaymentSystemPromoBannerNotShown(
                jestLayer,
                apiaryLayer,
                mandrelLayer,
                mockSelectPromoPaymentSystem,
                masterCard
            );
        });

        test('выбрана опция списания кэшбэка', async () => {
            await checkPaymentSystemPromoBannerNotShown(
                jestLayer,
                apiaryLayer,
                mandrelLayer,
                mockSpendCashbackOption,
                mirCard
            );
        });
    });

    describe('Отображается для mastercard', () => {
        test('по умолчанию', async () => {
            await checkPaymentSystemPromoBannerContent(
                jestLayer,
                apiaryLayer,
                mandrelLayer,
                mockPaymentSystemMastercardPromoAllowed,
                mirCard,
                'С Mastercard — ещё  500 баллов',
                'Оплатите заказ картой Mastercard  ® онлайн и получите ещё 10% баллами'
            );
        });

        test('сумма кэшбэка превышает максимального значения по акции', async () => {
            await checkPaymentSystemPromoBannerContent(
                jestLayer,
                apiaryLayer,
                mandrelLayer,
                mockPaymentSystemMastercardPromoAllowedWithLargePromoCashback,
                mirCard,
                'С Mastercard — ещё  2 000 баллов',
                'Оплатите заказ картой Mastercard  ® онлайн и получите ещё 10% баллами'
            );
        });
    });

    describe('Отображается для mir', () => {
        test('по умолчанию', async () => {
            await checkPaymentSystemPromoBannerContent(
                jestLayer,
                apiaryLayer,
                mandrelLayer,
                mockPaymentSystemMirPromoAllowed,
                masterCard,
                'С картой «Мир» — ещё  500 баллов',
                'Оплатите заказ  картой «Мир» онлайн и получите ещё 10% баллами'
            );
        });

        test('сумма кэшбэка превышает максимального значения по акции', async () => {
            await checkPaymentSystemPromoBannerContent(
                jestLayer,
                apiaryLayer,
                mandrelLayer,
                mockPaymentSystemMirPromoAllowedWithLargePromoCashback,
                masterCard,
                'С картой «Мир» — ещё  2 000 баллов',
                'Оплатите заказ  картой «Мир» онлайн и получите ещё 10% баллами'
            );
        });
    });
});
