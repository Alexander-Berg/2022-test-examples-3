import {mockLocation} from '@self/root/src/helpers/testament/mock';
import {makeMirrorTouch} from '@self/root/src/helpers/testament/mirror';

import {baseMockFunctionality} from './mockFunctionality';
import {
    widgetNonDisplayedForNonAuthUser,
    widgetNonDisplayedWhenNotCashbackBalanceAndEmitRestricted,
    widgetNonDisplayedWhenEmitSpendRestrictedWithoutReasond,
    correctDisplayedEmitAllowed,
    correctDisplayedSpendAllowed,
    correctDisplayedEmitRestricted,
    correctDisplayedSpendRestricted,
    correctDisplayedSpendRestrictedWhenCashbackBalanceNegative,
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

describe('CheckoutCashbackControl', () => {
    describe('Не авторизованный пользователь', () => {
        test('Не отображается', () => widgetNonDisplayedForNonAuthUser(jestLayer, apiaryLayer, mandrelLayer));
    });

    describe('Авторизованный пользователь', () => {
        beforeAll(async () => {
            await baseMockFunctionality(jestLayer);
        });

        describe('Не отображается', () => {
            test('если нет баллов плюса и нельзя накопить',
                () => widgetNonDisplayedWhenNotCashbackBalanceAndEmitRestricted(jestLayer, apiaryLayer, mandrelLayer)
            );

            test('если накопление и списание недоступны без причины',
                () => widgetNonDisplayedWhenEmitSpendRestrictedWithoutReasond(jestLayer, apiaryLayer, mandrelLayer)
            );
        });

        describe('Отображается', () => {
            describe('Опция доступна', () => {
                test('накопления', () => correctDisplayedEmitAllowed(jestLayer, apiaryLayer, mandrelLayer));

                test('списания', () => correctDisplayedSpendAllowed(jestLayer, apiaryLayer, mandrelLayer));
            });

            describe('Опция не доступна', () => {
                test('накопления', () => correctDisplayedEmitRestricted(jestLayer, apiaryLayer, mandrelLayer));

                describe('Списания', () => {
                    test('по умолчанию', () => correctDisplayedSpendRestricted(jestLayer, apiaryLayer, mandrelLayer));

                    test('у пользователя отрицательный баланс',
                        () => correctDisplayedSpendRestrictedWhenCashbackBalanceNegative(jestLayer, apiaryLayer, mandrelLayer)
                    );
                });
            });
        });
    });
});
