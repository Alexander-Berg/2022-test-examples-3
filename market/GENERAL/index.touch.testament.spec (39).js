import {mockLocation, mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';
import {makeMirrorTouch} from '@self/root/src/helpers/testament/mirror';
import {YA_PLUS_BADGE_NOTIFICATION_TYPE} from '@self/root/src/constants/yaPlus';

import {checkHeaderPlusBalanceContent} from './testCases';

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
 * Функцию checkHeaderPlusBalanceContent мы вызываем в describe, передавать в нее слои инициализируемые в beforeAll нельзя,
 * так как beforeAll вызывается перед it, и в данном случае слои будут еще не инициализированы
 */
const getLayers = async () => {
    if (!mirror) {
        mockLocation();
        mockIntersectionObserver();
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

        await jestLayer.doMock(
            require.resolve('@self/root/src/widgets/content/PromoTooltip'),
            () => ({create: () => Promise.resolve(null)})
        );
        await jestLayer.doMock(
            require.resolve('@self/root/src/widgets/content/YandexPlusOnboarding'),
            () => ({create: () => Promise.resolve(null)})
        );
        await jestLayer.doMock(
            require.resolve('@self/root/src/widgets/content/CashbackInfoPopup'),
            () => ({create: () => Promise.resolve(null)})
        );
        await jestLayer.doMock(
            require.resolve('@self/root/src/widgets/content/WelcomeCashbackOnboardingTooltip'),
            () => ({create: () => Promise.resolve(null)})
        );
    }

    return {
        jestLayer,
        mandrelLayer,
        apiaryLayer,
    };
};

describe('HeaderPlusBalance', () => {
    describe('пользователь без баллов, видел онбординг', () => checkHeaderPlusBalanceContent(
        getLayers,
        {
            shouldShowOnboarding: false,
            hasCashbackAnnihilation: false,
            balance: null,
        },
        {}
    ));
    describe('пользователь без баллов, не видел онбординг', () => checkHeaderPlusBalanceContent(
        getLayers,
        {
            shouldShowOnboarding: true,
            hasCashbackAnnihilation: false,
            balance: null,
        },
        {
            notificationType: YA_PLUS_BADGE_NOTIFICATION_TYPE.RED_CIRCLE,
        }
    ));
    describe('пользователь c баллами, видел онбординг', () => checkHeaderPlusBalanceContent(
        getLayers,
        {
            shouldShowOnboarding: false,
            hasCashbackAnnihilation: false,
            balance: 100,
        },
        {
            cashbackBalance: '100',
            cashbackBalanceWithVisibleHidden: 'Ваши баллы:100',
        }
    ));
    describe('пользователь c баллами, не видел онбординг', () => checkHeaderPlusBalanceContent(
        getLayers,
        {
            shouldShowOnboarding: true,
            hasCashbackAnnihilation: false,
            balance: 100,
        },
        {
            cashbackBalance: '100',
            cashbackBalanceWithVisibleHidden: 'Ваши баллы:100',
            notificationType: YA_PLUS_BADGE_NOTIFICATION_TYPE.RED_CIRCLE,
        }
    ));
    describe('пользователь c баллами и сгоранием кешбэка, видел онбординг', () => checkHeaderPlusBalanceContent(
        getLayers,
        {
            shouldShowOnboarding: false,
            hasCashbackAnnihilation: true,
            balance: 100,
        },
        {
            cashbackBalance: '100',
            cashbackBalanceWithVisibleHidden: 'Ваши баллы:100',
            notificationType: YA_PLUS_BADGE_NOTIFICATION_TYPE.FIRE_SIGN,
        }
    ));
    describe('пользователь c баллами и сгоранием кешбэка, не видел онбординг', () => checkHeaderPlusBalanceContent(
        getLayers,
        {
            shouldShowOnboarding: true,
            hasCashbackAnnihilation: true,
            balance: 100,
        },
        {
            cashbackBalance: '100',
            cashbackBalanceWithVisibleHidden: 'Ваши баллы:100',
            notificationType: YA_PLUS_BADGE_NOTIFICATION_TYPE.FIRE_SIGN,
        }
    ));
});
