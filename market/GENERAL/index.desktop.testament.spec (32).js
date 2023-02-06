import {mockLocation} from '@self/root/src/helpers/testament/mock';
import {makeMirrorDesktop} from '@self/root/src/helpers/testament/mirror';
import {BANK_CARD_SYSTEMS} from '@self/root/src/entities/bankCard';

import {
    baseMockFunctionality,
    mockEmptyCart,
    mockYaPlusUserWithEmptySpendAmount,
    mockNonYaPlusUser,
    mockYaPlusUserWithPositiveEmitAmount,
    mockYaPlusUserWithEmitRestricted,
    mockPaymentSystemExtraCashbackRestricted,
    mockPaymentSystemExtraCashbackAllowedWithoutPromoCashbackProfile,
    mockPaymentSystemExtraCashbackMastercardAllowedWithPromoCashbackProfile,
    mockPaymentSystemExtraCashbackMastercardAllowedWithLargePromoCashback,
    mockPaymentSystemExtraCashbackMirAllowedWithPromoCashbackProfile,
    mockPaymentSystemExtraCashbackMirAllowedWithLargePromoCashback,
} from './mockFunctionality';
import {
    checkCartYaPlusPromoNotShown,
    checkContent,
    checkPaymentSystemPromoBannerNotShown,
    checkPaymentSystemPromoBannerContent,
} from './commonTestCases';
import {checkLinkPlusHome} from './testCases/';

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

    await jestLayer.runCode(() => {
        // eslint-disable-next-line global-require
        const {mockRouterFabric} = require('@self/root/src/helpers/testament/mock');
        mockRouterFabric()({
            'external:plus-tld': ({tld}) => `/plus.yandex.${tld}`,
        });
    }, []);

    await baseMockFunctionality(jestLayer);
});

afterAll(() => {
    mirror.destroy();
});

describe('CartYaPlusPromo', () => {
    describe('Не отображается', () => {
        test('Для не авторизованного пользователя', async () => {
            await checkCartYaPlusPromoNotShown(
                jestLayer,
                apiaryLayer,
                mandrelLayer,
                mockYaPlusUserWithPositiveEmitAmount,
                {
                    user: {
                        isAuth: false,
                    },
                }
            );
        });

        test('При пустой корзине', async () => {
            await checkCartYaPlusPromoNotShown(
                jestLayer,
                apiaryLayer,
                mandrelLayer,
                mockEmptyCart
            );
        });

        test('Для плюсовиков, если нет возможности потратить кешбэк', async () => {
            await checkCartYaPlusPromoNotShown(
                jestLayer,
                apiaryLayer,
                mandrelLayer,
                mockYaPlusUserWithEmptySpendAmount
            );
        });
    });

    describe('Отображается', () => {
        describe('Контент отображается корректно', () => {
            describe('Для неплюсовика', () => {
                test('по умолчанию', async () => {
                    await checkContent(
                        jestLayer,
                        apiaryLayer,
                        mandrelLayer,
                        mockNonYaPlusUser,
                        'Копите и тратьте баллы с Яндекс Плюсом'
                    );
                });

                test('ссылка на дом плюса отображается', async () => {
                    await checkLinkPlusHome(
                        jestLayer,
                        apiaryLayer,
                        mandrelLayer,
                        mockNonYaPlusUser,
                        true
                    );
                });
            });

            describe('Для плюсовика', () => {
                test('по умолчанию', async () => {
                    await checkContent(
                        jestLayer,
                        apiaryLayer,
                        mandrelLayer,
                        mockYaPlusUserWithPositiveEmitAmount,
                        'Купите дешевле на 1 000 ₽',
                        'Спишите  1 000 баллов при оформлении или получите  123 балла за покупку'
                    );
                });

                test('когда баллы нельзя накопить', async () => {
                    await checkContent(
                        jestLayer,
                        apiaryLayer,
                        mandrelLayer,
                        mockYaPlusUserWithEmitRestricted,
                        'Купите дешевле на 1 000 ₽',
                        'Спишите  1 000 баллов при оформлении'
                    );
                });

                test('ссылка на дом плюса не отображается', async () => {
                    await checkLinkPlusHome(
                        jestLayer,
                        apiaryLayer,
                        mandrelLayer,
                        mockYaPlusUserWithEmitRestricted,
                        false
                    );
                });
            });
        });
    });

    describe('PaymentSystemPromoBanner', () => {
        describe('Не отображается', () => {
            test('когда акция недоступна для пользователя', async () => {
                await checkPaymentSystemPromoBannerNotShown(
                    jestLayer,
                    apiaryLayer,
                    mandrelLayer,
                    mockPaymentSystemExtraCashbackRestricted
                );
            });

            test('когда не выбраны акционные товары, нет промо кэшбэк опций', async () => {
                await checkPaymentSystemPromoBannerNotShown(
                    jestLayer,
                    apiaryLayer,
                    mandrelLayer,
                    mockPaymentSystemExtraCashbackAllowedWithoutPromoCashbackProfile
                );
            });
        });

        describe('Для платежной системы mastercard', () => {
            describe('Отображается', () => {
                test('по умолчанию', async () => {
                    await checkPaymentSystemPromoBannerContent(
                        jestLayer,
                        apiaryLayer,
                        mandrelLayer,
                        mockPaymentSystemExtraCashbackMastercardAllowedWithPromoCashbackProfile,
                        'С Mastercard — ещё  500 баллов',
                        'Оплатите заказ картой Mastercard  ® онлайн и получите ещё 10% баллами',
                        BANK_CARD_SYSTEMS.MASTERCARD
                    );
                });

                test('сумма кэшбэка превышает максимального значения по акции', async () => {
                    await checkPaymentSystemPromoBannerContent(
                        jestLayer,
                        apiaryLayer,
                        mandrelLayer,
                        mockPaymentSystemExtraCashbackMastercardAllowedWithLargePromoCashback,
                        'С Mastercard — ещё  2 000 баллов',
                        'Оплатите заказ картой Mastercard  ® онлайн и получите ещё 10% баллами',
                        BANK_CARD_SYSTEMS.MASTERCARD
                    );
                });
            });
        });

        describe('Для платежной системы mir', () => {
            describe('Отображается', () => {
                test('по умолчанию', async () => {
                    await checkPaymentSystemPromoBannerContent(
                        jestLayer,
                        apiaryLayer,
                        mandrelLayer,
                        mockPaymentSystemExtraCashbackMirAllowedWithPromoCashbackProfile,
                        'С картой «Мир» — ещё  500 баллов',
                        'Оплатите заказ  картой «Мир» онлайн и получите ещё 10% баллами',
                        BANK_CARD_SYSTEMS.MIR
                    );
                });

                test('сумма кэшбэка превышает максимального значения по акции', async () => {
                    await checkPaymentSystemPromoBannerContent(
                        jestLayer,
                        apiaryLayer,
                        mandrelLayer,
                        mockPaymentSystemExtraCashbackMirAllowedWithLargePromoCashback,
                        'С картой «Мир» — ещё  2 000 баллов',
                        'Оплатите заказ  картой «Мир» онлайн и получите ещё 10% баллами',
                        BANK_CARD_SYSTEMS.MIR
                    );
                });
            });
        });
    });
});
