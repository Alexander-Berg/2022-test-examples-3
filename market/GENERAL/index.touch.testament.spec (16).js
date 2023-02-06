import {waitFor} from '@testing-library/dom';

import {makeMirror} from '@self/platform/helpers/testament';

// PageObjects
import SideMenuPO from '@self/platform/spec/page-objects/widgets/core/SideMenuRedesign/SideMenu';
import NotAuthUserInfoPO from '@self/platform/spec/page-objects/widgets/core/SideMenuRedesign/NotAuthUserInfo';
import GrowingCashbackMenuItemPO
    from '@self/root/src/widgets/content/GrowingCashbackMenuItem/components/View/__pageObject';
import LinkPO from '@self/root/src/components/Link/__pageObject';
import ReferralProgramMenuItemPO from '@self/root/src/widgets/content/ReferralProgramMenuItem/__pageObject';
import YaPlusMenuItemPO from '@self/root/src/components/YaPlusMenuItem/__pageObject';
import SpecialMenuItemPO from '@self/root/src/components/SpecialMenuItem/__pageObject';
import UserPlusCashbackBalanceBadgePO, {
    BadgeNotification as BadgeNotificationPO,
} from '@self/root/src/components/UserPlusCashbackBalanceBadge/__pageObject';

// mocks
import {
    yandexPlusPerk,
    yandexCashbackPerk,
    growingCashbackPerk,
    referralProgramPerk,
    referralProgramGotFullReward,
} from '@self/root/src/spec/hermione/kadavr-mock/loyalty/perks';
import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';
import {
    TITLE,
    SUBTITLE,
    titleWithNotification,
} from '@self/platform/widgets/core/SideMenuRedesign/__spec__/__mocks__';

// constants
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {NON_BREAKING_SPACE_CHAR as NBSP} from '@self/root/src/constants/string';
import {GROWING_CASHBACK_LANDING_SEMANTIC_ID} from '@self/root/src/constants/growingCashback';
import {YA_PLUS_BADGE_NOTIFICATION_TYPE} from '@self/root/src/constants/yaPlus';

// путь к виджету который тестируем
const WIDGET_PATH = '../';

/** @type {Mirror} */
let mirror;
/** @type {JestLayer} */
let jestLayer;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;
/** @type {KadavrLayer} */
let kadavrLayer;

async function makeContext(user = {}, pageId = 'touch:index') {
    const cookie = {kadavr_session_id: await kadavrLayer.getSessionId()};

    return mandrelLayer.initContext({
        request: {cookie},
        user,
        page: {
            pageId,
        },
    });
}

const mockResolveShouldShowYandexPlusOnboarding = async shouldShow => {
    await jestLayer.backend.runCode(shouldShow => {
        jest.spyOn(require('@self/root/src/resolvers/yandexPlusOnboarding/resolveShouldShowYandexPlusOnboarding'), 'resolveShouldShowYandexPlusOnboarding')
            .mockReturnValue(Promise.resolve({
                result: {
                    shouldShow,
                },
            }));
    }, [shouldShow]);
};

const mockResolveCashbackAnnihilationInfo = async hasCashbackAnnihilation => {
    await jestLayer.backend.runCode(hasCashbackAnnihilation => {
        const {
            cashbackAnnihilation,
            emptyCashbackAnnihilation,
        } = require('@self/root/market/platform.touch/widgets/core/SideMenuRedesign/__spec__/__mocks__');

        jest.spyOn(require('@self/root/src/resolvers/cashbackAnnihilation'), 'resolveCashbackAnnihilationInfo')
            .mockReturnValue(Promise.resolve(
                hasCashbackAnnihilation
                    ? cashbackAnnihilation
                    : emptyCashbackAnnihilation
            ));
    }, [hasCashbackAnnihilation]);
};

const mockResolveToggleFlag = async toggleFlagValue => {
    await jestLayer.backend.runCode(toggleFlagValue => {
        jest.spyOn(require('@self/root/src/resolvers/toggle'), 'resolveToggleFlag')
            .mockImplementation((ctx, toggleFlagId) => {
                if (toggleFlagId === 'all_growing-cashback-web') {
                    return Promise.resolve({id: '', value: toggleFlagValue});
                }

                return Promise.resolve({id: '', value: false});
            });
    }, [toggleFlagValue]);
};

const testsYaPlusMenuItem = ({
    mockFunctionality,
    expectedPrimaryText,
    expectedSecondaryText,
    expectedCashbackBalance,
    expectedNotificationType,
}) => {
    let container;

    beforeAll(async () => {
        await mockFunctionality();

        const widget = await apiaryLayer.mountWidget(WIDGET_PATH);
        container = widget.container;

        const root = container.querySelector(YaPlusMenuItemPO.root);
        await waitFor(() => {
            expect(root).toBeVisible();
        });
    });

    it('Пункт меню содержит корректный текст', async () => {
        const primaryText = container.querySelector(`${YaPlusMenuItemPO.root} ${SpecialMenuItemPO.primaryText}`);
        expect(primaryText.textContent).toBe(expectedPrimaryText);

        const secondaryText = container.querySelector(`${YaPlusMenuItemPO.root} ${SpecialMenuItemPO.secondaryText}`);
        expect(secondaryText.textContent).toBe(expectedSecondaryText);
    });

    it('Пункт меню содержит корректное значение кешбэка', async () => {
        const cashbackBalance = container.querySelector(UserPlusCashbackBalanceBadgePO.root);
        expect(cashbackBalance).not.toBeNull();
        expect(cashbackBalance.textContent).toEqual(expectedCashbackBalance);
    });

    it('Бэйдж нотификации корректно отображается в пункте меню', async () => {
        if (!expectedNotificationType) {
            const badgeNotification = container.querySelector(BadgeNotificationPO.root);
            expect(badgeNotification).toBeNull();
        } else if (expectedNotificationType === YA_PLUS_BADGE_NOTIFICATION_TYPE.RED_CIRCLE) {
            const redCircleBadgeNotification = container.querySelector(BadgeNotificationPO.redCircle);
            expect(redCircleBadgeNotification).not.toBeNull();
        } else if (expectedNotificationType === YA_PLUS_BADGE_NOTIFICATION_TYPE.FIRE_SIGN) {
            const fireSignBadgeNotification = container.querySelector(BadgeNotificationPO.fireSign);
            expect(fireSignBadgeNotification).not.toBeNull();
        }
    });
};

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
    jestLayer = mirror.getLayer('jest');
    mandrelLayer = mirror.getLayer('mandrel');
    apiaryLayer = mirror.getLayer('apiary');
    kadavrLayer = mirror.getLayer('kadavr');

    await jestLayer.doMock(require.resolve('@self/root/src/utils/unsafe/user.js'), () => {
        const originalModule = jest.requireActual('@self/root/src/utils/unsafe/user.js');

        return {
            __esModule: true,
            ...originalModule,
            getRegion: jest.fn().mockReturnValue(require('@self/platform/widgets/core/SideMenuRedesign/__spec__/__mocks__').region),
            getCurrentUser: jest.fn().mockReturnValue(require('@self/platform/widgets/core/SideMenuRedesign/__spec__/__mocks__').currentUser),
        };
    });

    await jestLayer.backend.runCode(s3mds => {
        jest.doMock(s3mds, () => ({
            __esModule: true,
            fetchReferralProgramConfig: jest.fn().mockReturnValue({
                result: {isFullFunctionalEnabled: true},
                collections: {},
            }),
        }));
    }, [require.resolve('@self/root/src/resources/s3mds')]);

    await jestLayer.runCode(growingCashback => {
        const {mockRouter} = require('@self/project/src/helpers/testament/mock');
        mockRouter({
            'external:yandex-passport': '//pass-test.yandex.ru',
            'market:index': '//market.yandex.ru',
            'external:jobs-vacancies-dev': '//jobs/services/market/about',
            'touch:wishlist': '//my/wishlist',
            'touch:compare-lists': '//compare-lists',
            'market:my-tasks': '//my/tasks',
            'external:white-market-referral-landing': '//my/referral',
            'market:special': `//special/${growingCashback}`,
        });
    }, [GROWING_CASHBACK_LANDING_SEMANTIC_ID]);

    await kadavrLayer.setState('Tarantino.data.result', [{
        id: 123665,
        revisionId: 4053458,
        pageName: 'Покупки / Доступные способы связи со службой поддержки',
        type: 'mp_available_support_channels',
        isChatAvailable: true,
        isPhoneAvailable: true,
        unavailablePhoneText: null,
        isDsbsArbitrageAvailable: true,
        isFbsOrExpressChatAvailable: true,
    }]);

    await jestLayer.backend.runCode(() => {
        jest.spyOn(require('@self/root/src/resolvers/express/resolveExpressEntrypointVisibility'), 'resolveExpressEntrypointVisibility')
            .mockReturnValue(Promise.resolve(false));
    }, []);
});

beforeEach(async () => {
    await mockResolveShouldShowYandexPlusOnboarding(false);
    await mockResolveCashbackAnnihilationInfo(false);
    await mockResolveToggleFlag(true);
});

afterAll(() => {
    mirror.destroy();
});

describe('Блок бокового меню.', () => {
    describe('Блок с информацией пользователя.', () => {
        describe('Для неавторизованного пользователя', () => {
            it('отображает кнопку для входа', async () => {
                await makeContext();
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH);
                const rootElement = container.querySelector(NotAuthUserInfoPO.loginButton);
                expect(rootElement).toBeVisible();
            });
            it('Кнопка "Войти" содержит ссылку на форму авторизации', async () => {
                await makeContext();
                const expectedLink = 'passport-auth';
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH);
                const rootElement = container.querySelector(SideMenuPO.root);
                await waitFor(() => {
                    expect(rootElement).toBeVisible();
                });
                const link = container.querySelector(NotAuthUserInfoPO.loginButton);
                expect(link.href).toMatch(expectedLink);
            });
        });
    });

    describe('Пункт меню "Выйти".', () => {
        describe('Для неавторизованного пользователя', () => {
            it('должен отсутствовать', async () => {
                await makeContext();
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH);
                const rootElement = container.querySelector(SideMenuPO.exit);
                expect(rootElement).toBeNull();
            });
        });
    });

    describe('Пункт меню "Растущий кешбэк".', () => {
        describe('Акция доступна', () => {
            describe('По умолчанию', () => {
                describe('Плашка "Растущий кешбэк"', () => {
                    beforeEach(async () => {
                        await makeContext({isAuth: true}, PAGE_IDS_COMMON.INDEX);
                        await kadavrLayer.setState('Loyalty.collections.perks', [growingCashbackPerk]);
                    });
                    it('Пункт меню', async () => {
                        const {container} = await apiaryLayer.mountWidget(WIDGET_PATH);

                        await step('Плашка "Растущий кешбэк" должна отображаться', async () => {
                            await waitFor(() => {
                                expect(container.querySelector(GrowingCashbackMenuItemPO.root)).toBeVisible();
                            });
                        });

                        await step('Заголовок должен содержаться в плашке', async () => {
                            await waitFor(() => {
                                expect(container.querySelector(GrowingCashbackMenuItemPO.growingCashbackMenuItemPrimaryText)).toBeVisible();
                            });
                        });

                        await step('Заголовок должен содержать корректный текст', async () => {
                            const expectedText = `1${NBSP}550${NBSP}баллов${NBSP}Плюса`;
                            const primaryText = container.querySelector(GrowingCashbackMenuItemPO.growingCashbackMenuItemPrimaryText);
                            expect(primaryText.textContent).toBe(expectedText);
                        });

                        await step('Заголовок должен содержаться в плашке', async () => {
                            await waitFor(() => {
                                expect(container.querySelector(GrowingCashbackMenuItemPO.growingCashbackMenuItemSecondaryText)).toBeVisible();
                            });
                        });

                        await step('Подзаголовок должен содержать корректный текст', async () => {
                            const expectedText = `За${NBSP}первые 3${NBSP}заказав${NBSP}приложении от${NBSP}3${NBSP}500${NBSP}₽`;
                            const secondaryText = container.querySelector(GrowingCashbackMenuItemPO.growingCashbackMenuItemSecondaryText);
                            expect(secondaryText.textContent).toBe(expectedText);
                        });

                        await step('Проверяем ссылку на лэндинг', async () => {
                            const expectedLink = 'market:special';
                            const link = container.querySelector(`${GrowingCashbackMenuItemPO.root} ${LinkPO.root}`);
                            expect(link.href).toMatch(expectedLink);
                        });
                    });
                });
            });
        });
    });

    describe('Ссылка "Маркет нанимает".', () => {
        describe('По умолчанию', () => {
            it('Ведет на страницу "Вакансии Яндекс.Маркет"', async () => {
                await makeContext();
                const expectedLink = 'external:jobs-vacancies-dev';
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH);
                const vacancies = container.querySelector(SideMenuPO.vacancies);
                expect(vacancies.href).toMatch(expectedLink);
            });
        });
    });

    describe('По умолчанию', () => {
        it('пункт меню "Избранное" содержит ссылку на страницу', async () => {
            await makeContext();
            const expectedPath = '/my/wishlist';
            const {container} = await apiaryLayer.mountWidget(WIDGET_PATH);
            const wishlist = container.querySelector(SideMenuPO.wishlist);
            expect(wishlist.href).toMatch(expectedPath);
        });
        it('пункт меню "Сравнение товаров" содержит ссылку на страницу', async () => {
            await makeContext();
            const expectedLink = 'touch:compare-lists';
            const {container} = await apiaryLayer.mountWidget(WIDGET_PATH);
            const comparison = container.querySelector(SideMenuPO.comparison);
            expect(comparison.href).toMatch(expectedLink);
        });
        it('пункт меню "Мои публикации" содержит ссылку на страницу', async () => {
            await makeContext();
            const expectedLink = 'market:my-tasks';
            const {container} = await apiaryLayer.mountWidget(WIDGET_PATH);
            const myContent = container.querySelector(SideMenuPO.myContent);
            expect(myContent.href).toMatch(expectedLink);
        });
    });

    describe('Пункт меню "Приглашайте друзей".', () => {
        describe('Акция доступна,', () => {
            describe('Неавторизованный пользователь,', () => {
                beforeEach(async () => {
                    await makeContext();
                    await kadavrLayer.setState('Loyalty.collections.perks', [referralProgramPerk]);
                });
                testReferralProgram();
            });

            describe('Пользователь достиг максимального количества баллов,', () => {
                beforeEach(async () => {
                    await makeContext({isAuth: true});
                    await kadavrLayer.setState('Loyalty.collections.perks', [referralProgramGotFullReward]);
                });
                testReferralProgram();
            });

            describe('Пользователь не достиг максимального количества баллов,', () => {
                beforeEach(async () => {
                    await makeContext({isAuth: true});
                    await kadavrLayer.setState('Loyalty.collections.perks', [referralProgramPerk]);
                });
                testReferralProgram({withNotification: false, withSecondaryText: true});
            });
        });
        describe('Акция не доступна.', () => {
            beforeEach(async () => {
                await makeContext({isAuth: true});
                await kadavrLayer.setState('Loyalty.collections.perks', []);
            });
            it('Плашка не отображается.', async () => {
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH);
                const root = container.querySelector(ReferralProgramMenuItemPO.root);
                expect(root).toBeNull();
            });
        });
    });

    describe('Пункт меню "Яндекс Плюс".', () => {
        describe('Плюсовик без баллов', () => {
            const mockFunctionality = async () => {
                await makeContext({isAuth: true});
                await kadavrLayer.setState('Loyalty.collections.perks', [yandexPlusPerk]);
            };

            testsYaPlusMenuItem({
                mockFunctionality,
                expectedPrimaryText: 'Яндекс Плюс',
                expectedSecondaryText: 'Кешбэк до 3%',
                expectedCashbackBalance: 'Ваш кешбэк:3%',
            });
        });

        describe('Плюсовик с баллов', () => {
            const mockFunctionality = async () => {
                await makeContext({isAuth: true});
                await kadavrLayer.setState('Loyalty.collections.perks', [yandexPlusPerk, yandexCashbackPerk]);
            };

            testsYaPlusMenuItem({
                mockFunctionality,
                expectedPrimaryText: 'Яндекс Плюс',
                expectedSecondaryText: 'Кешбэк до 3%',
                expectedCashbackBalance: 'Ваши баллы:100',
            });
        });

        describe('Плюсовик не видел онбординг', () => {
            const mockFunctionality = async () => {
                await makeContext({isAuth: true});
                await kadavrLayer.setState('Loyalty.collections.perks', [yandexPlusPerk, yandexCashbackPerk]);
                await mockResolveShouldShowYandexPlusOnboarding(true);
            };

            testsYaPlusMenuItem({
                mockFunctionality,
                expectedPrimaryText: 'Яндекс Плюс',
                expectedSecondaryText: 'Кешбэк до 3%',
                expectedCashbackBalance: 'Ваши баллы:100',
                expectedNotificationType: YA_PLUS_BADGE_NOTIFICATION_TYPE.RED_CIRCLE,
            });
        });

        describe('Не плюсовик без баллов', () => {
            const mockFunctionality = async () => {
                await makeContext({isAuth: true});
                await kadavrLayer.setState('Loyalty.collections.perks', []);
            };

            testsYaPlusMenuItem({
                mockFunctionality,
                expectedPrimaryText: 'Подключите Плюс',
                expectedSecondaryText: `Фильмы, музыка и${NBSP}кешбэк баллами`,
                expectedCashbackBalance: 'Ваш кешбэк:3%',
            });
        });

        describe('Не плюсовик с баллов', () => {
            const mockFunctionality = async () => {
                await makeContext({isAuth: true});
                await kadavrLayer.setState('Loyalty.collections.perks', [yandexCashbackPerk]);
            };

            testsYaPlusMenuItem({
                mockFunctionality,
                expectedPrimaryText: 'Подключите Плюс',
                expectedSecondaryText: `Фильмы, музыка и${NBSP}кешбэк баллами`,
                expectedCashbackBalance: 'Ваши баллы:100',
            });
        });

        describe('Не плюсовик не видел онбординг', () => {
            const mockFunctionality = async () => {
                await makeContext({isAuth: true});
                await kadavrLayer.setState('Loyalty.collections.perks', [yandexCashbackPerk]);
                await mockResolveShouldShowYandexPlusOnboarding(true);
            };

            testsYaPlusMenuItem({
                mockFunctionality,
                expectedPrimaryText: 'Подключите Плюс',
                expectedSecondaryText: `Фильмы, музыка и${NBSP}кешбэк баллами`,
                expectedCashbackBalance: 'Ваши баллы:100',
                expectedNotificationType: YA_PLUS_BADGE_NOTIFICATION_TYPE.RED_CIRCLE,
            });
        });

        describe('Не плюсовик со сгоранием кешбэка видел онбординг', () => {
            const mockFunctionality = async () => {
                await makeContext({isAuth: true});
                await kadavrLayer.setState('Loyalty.collections.perks', [yandexCashbackPerk]);
                await mockResolveCashbackAnnihilationInfo(true);
            };

            testsYaPlusMenuItem({
                mockFunctionality,
                expectedPrimaryText: 'Подключите Плюс',
                expectedSecondaryText: `Фильмы, музыка и${NBSP}кешбэк баллами`,
                expectedCashbackBalance: 'Ваши баллы:100',
                expectedNotificationType: YA_PLUS_BADGE_NOTIFICATION_TYPE.FIRE_SIGN,
            });
        });

        describe('Не плюсовик со сгоранием кешбэка не видел онбординг', () => {
            const mockFunctionality = async () => {
                await makeContext({isAuth: true});
                await kadavrLayer.setState('Loyalty.collections.perks', [yandexCashbackPerk]);
                await mockResolveShouldShowYandexPlusOnboarding(true);
                await mockResolveCashbackAnnihilationInfo(true);
            };

            testsYaPlusMenuItem({
                mockFunctionality,
                expectedPrimaryText: 'Подключите Плюс',
                expectedSecondaryText: `Фильмы, музыка и${NBSP}кешбэк баллами`,
                expectedCashbackBalance: 'Ваши баллы:100',
                expectedNotificationType: YA_PLUS_BADGE_NOTIFICATION_TYPE.FIRE_SIGN,
            });
        });
    });
});

function testReferralProgram({withNotification = true, withSecondaryText = false} = {}) {
    describe('Плашка "Приглашайте друзей",', () => {
        it('Плашка отображается.', async () => {
            const {container} = await apiaryLayer.mountWidget(WIDGET_PATH);
            const root = container.querySelector(ReferralProgramMenuItemPO.root);
            await waitFor(() => {
                expect(root).toBeVisible();
            });
        });
        it('Заголовок присутствует в плашке.', async () => {
            const {container} = await apiaryLayer.mountWidget(WIDGET_PATH);
            const specialMenuItemPrimaryText = container.querySelector(ReferralProgramMenuItemPO.specialMenuItemPrimaryText);
            await waitFor(() => {
                expect(specialMenuItemPrimaryText).toBeVisible();
            });
        });
        it('Ссылка содержит корректный url.', async () => {
            const expectedLink = 'market:referral-landing';
            const {container} = await apiaryLayer.mountWidget(WIDGET_PATH);
            const referralLink = container.querySelector(`${ReferralProgramMenuItemPO.root} ${LinkPO.root}`);
            expect(referralLink.href).toMatch(expectedLink);
        });

        if (withNotification) {
            it('Заголовок содержит корректный текст и кружок уведомления подписан для незрячего', async () => {
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH);
                const specialMenuItemPrimaryText = container.querySelector(ReferralProgramMenuItemPO.specialMenuItemPrimaryText);
                expect(specialMenuItemPrimaryText.textContent).toBe(titleWithNotification);
            });
        } else {
            it('Заголовок содержит корректный текст.', async () => {
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH);
                const specialMenuItemPrimaryText = container.querySelector(ReferralProgramMenuItemPO.specialMenuItemPrimaryText);
                expect(specialMenuItemPrimaryText.textContent).toBe(TITLE);
            });
        }

        if (withSecondaryText) {
            it('Подзаголовок содержит корректный текст.', async () => {
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH);
                const specialMenuItemSecondaryText = container.querySelector(ReferralProgramMenuItemPO.specialMenuItemSecondaryText);
                expect(specialMenuItemSecondaryText.textContent).toBe(SUBTITLE);
            });
        } else {
            it('Подзаголовок отсутствует в плашке.', async () => {
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH);
                const specialMenuItemSecondaryText = container.querySelector(ReferralProgramMenuItemPO.specialMenuItemSecondaryText);
                expect(specialMenuItemSecondaryText).toBeNull();
            });
        }
    });
}
