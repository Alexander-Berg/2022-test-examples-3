import {mockLocation, mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';
import {makeMirrorDesktop} from '@self/root/src/helpers/testament/mirror';
import {YA_PLUS_BADGE_NOTIFICATION_TYPE} from '@self/root/src/constants/yaPlus';
import {MARKET_CASHBACK_PERCENT} from '@self/root/src/entities/perkStatus';
import {NON_BREAKING_SPACE_CHAR as NBSP} from '@self/root/src/constants/string';

import {testsYaPlusMenuItem} from './testCases';

/** @type {Mirror} */
let mirror;
/** @type {JestLayer} */
let jestLayer;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;

afterAll(() => {
    mirror.destroy();
});

/**
 * Функция возвращает слои, в первом тесте произойдет их инициализация, далее будут возвращаться инициализированные
 * переменные. Это сделано для того что бы разделить проверки теста на отдельные кейсы, при этом не делая дробление в этом файле.
 * Функцию testsYaPlusMenuItem мы вызываем в describe, передавать в нее слои инициализируемые в beforeAll нельзя,
 * так как beforeAll вызывается перед it, и в данном случае слои будут еще не инициализированы
 */
const getLayers = async () => {
    if (!mirror) {
        mockLocation();
        mockIntersectionObserver();
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

        await jestLayer.doMock(
            require.resolve('@self/root/src/widgets/content/YandexHelpMenuItem'),
            () => ({create: () => Promise.resolve(null)})
        );
        await jestLayer.doMock(
            require.resolve('@self/root/src/widgets/content/ReferralProgramMenuItem'),
            () => ({create: () => Promise.resolve(null)})
        );
        await jestLayer.doMock(
            require.resolve('@self/root/src/widgets/content/GrowingCashbackMenuItem'),
            () => ({create: () => Promise.resolve(null)})
        );
        await jestLayer.doMock(
            require.resolve('@self/root/src/widgets/content/WelcomeCashbackPopup'),
            () => ({create: () => Promise.resolve(null)})
        );
    }

    return {
        jestLayer,
        mandrelLayer,
        apiaryLayer,
    };
};

describe('ProfileMenu', () => {
    describe('Пункт меню "Яндекс Плюс".', () => {
        describe('Плюсовик без баллов', () => testsYaPlusMenuItem(
            getLayers,
            {
                hasYaPlus: true,
                balance: null,
                marketCashbackPercent: MARKET_CASHBACK_PERCENT,
                shouldShowOnboarding: false,
                hasCashbackAnnihilation: false,
            },
            {
                expectedPrimaryText: 'Яндекс Плюс',
                expectedSecondaryText: 'Кешбэк до 3%',
                expectedCashbackBalance: 'Ваш кешбэк:3%',
            }
        ));

        describe('Плюсовик с баллов', () => testsYaPlusMenuItem(
            getLayers,
            {
                hasYaPlus: true,
                balance: 100,
                marketCashbackPercent: MARKET_CASHBACK_PERCENT,
                shouldShowOnboarding: false,
                hasCashbackAnnihilation: false,
            },
            {
                expectedPrimaryText: 'Яндекс Плюс',
                expectedSecondaryText: 'Кешбэк до 3%',
                expectedCashbackBalance: 'Ваши баллы:100',
            }
        ));

        describe('Плюсовик не видел онбординг', () => testsYaPlusMenuItem(
            getLayers,
            {
                hasYaPlus: true,
                balance: 100,
                marketCashbackPercent: MARKET_CASHBACK_PERCENT,
                shouldShowOnboarding: true,
                hasCashbackAnnihilation: false,
            },
            {
                expectedPrimaryText: 'Яндекс Плюс',
                expectedSecondaryText: 'Кешбэк до 3%',
                expectedCashbackBalance: 'Ваши баллы:100',
                expectedNotificationType: YA_PLUS_BADGE_NOTIFICATION_TYPE.RED_CIRCLE,
            }
        ));

        describe('Не плюсовик без баллов', () => testsYaPlusMenuItem(
            getLayers,
            {
                hasYaPlus: false,
                balance: null,
                marketCashbackPercent: MARKET_CASHBACK_PERCENT,
                shouldShowOnboarding: false,
                hasCashbackAnnihilation: false,
            },
            {
                expectedPrimaryText: 'Подключите Плюс',
                expectedSecondaryText: `Фильмы, музыка и${NBSP}кешбэк баллами`,
                expectedCashbackBalance: 'Ваш кешбэк:3%',
            }
        ));

        describe('Не плюсовик с баллов', () => testsYaPlusMenuItem(
            getLayers,
            {
                hasYaPlus: false,
                balance: 100,
                marketCashbackPercent: MARKET_CASHBACK_PERCENT,
                shouldShowOnboarding: false,
                hasCashbackAnnihilation: false,
            },
            {
                expectedPrimaryText: 'Подключите Плюс',
                expectedSecondaryText: `Фильмы, музыка и${NBSP}кешбэк баллами`,
                expectedCashbackBalance: 'Ваши баллы:100',
            }
        ));

        describe('Не плюсовик не видел онбординг', () => testsYaPlusMenuItem(
            getLayers,
            {
                hasYaPlus: false,
                balance: 100,
                marketCashbackPercent: MARKET_CASHBACK_PERCENT,
                shouldShowOnboarding: true,
                hasCashbackAnnihilation: false,
            },
            {
                expectedPrimaryText: 'Подключите Плюс',
                expectedSecondaryText: `Фильмы, музыка и${NBSP}кешбэк баллами`,
                expectedCashbackBalance: 'Ваши баллы:100',
                expectedNotificationType: YA_PLUS_BADGE_NOTIFICATION_TYPE.RED_CIRCLE,
            }
        ));

        describe('Не плюсовик со сгоранием кешбэка видел онбординг', () => testsYaPlusMenuItem(
            getLayers,
            {
                hasYaPlus: false,
                balance: 100,
                marketCashbackPercent: MARKET_CASHBACK_PERCENT,
                shouldShowOnboarding: false,
                hasCashbackAnnihilation: true,
            },
            {
                expectedPrimaryText: 'Подключите Плюс',
                expectedSecondaryText: `Фильмы, музыка и${NBSP}кешбэк баллами`,
                expectedCashbackBalance: 'Ваши баллы:100',
                expectedNotificationType: YA_PLUS_BADGE_NOTIFICATION_TYPE.FIRE_SIGN,
            }
        ));

        describe('Не плюсовик со сгоранием кешбэка не видел онбординг', () => testsYaPlusMenuItem(
            getLayers,
            {
                hasYaPlus: false,
                balance: 100,
                marketCashbackPercent: MARKET_CASHBACK_PERCENT,
                shouldShowOnboarding: true,
                hasCashbackAnnihilation: true,
            },
            {
                expectedPrimaryText: 'Подключите Плюс',
                expectedSecondaryText: `Фильмы, музыка и${NBSP}кешбэк баллами`,
                expectedCashbackBalance: 'Ваши баллы:100',
                expectedNotificationType: YA_PLUS_BADGE_NOTIFICATION_TYPE.FIRE_SIGN,
            }
        ));
    });
});
