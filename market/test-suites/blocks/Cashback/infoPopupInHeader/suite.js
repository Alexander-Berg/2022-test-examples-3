import {makeSuite, makeCase} from 'ginny';
import dayjs from 'dayjs';

// mocks
import {yandexPlusPerk, yandexCashbackPerk} from '@self/root/src/spec/hermione/kadavr-mock/loyalty/perks';
import COOKIES from '@self/root/src/constants/cookie';
import {YANDEX_PLUS_ONBOARDING_TYPE} from '@self/root/src/constants/yaPlus';

export default makeSuite('Онбординг с информацией о плюсе', {
    params: {
        isAuthWithPlugin: 'авторизован ли пользователь',
        hasYaPlus: 'есть ли у пользователя подписка плюса',
        isPopupCookieSet: 'выставлена ли кука с показом попапа пользователю',
        // Баланс кешбэка пользователя
        hasYaPlusBalance: 'Баланс кешбэка пользователя',
        hasCashbackAnnihilation: 'Есть ли у пользователя сгорание кешбэка',
    },
    defaultParams: {
        // авторизован ли пользователь
        isAuthWithPlugin: false,
        // есть ли у пользователя подписка плюса
        hasYaPlus: false,
        // выставлена ли кука с показом попапа пользователю
        isPopupCookieSet: false,
        // Баланс кешбэка пользователя
        hasYaPlusBalance: false,
        // Есть ли у пользователя сгорание кешбэка
        hasCashbackAnnihilation: false,
    },
    story: {
        'По умолчанию': {
            async beforeEach() {
                const {hasYaPlus, hasYaPlusBalance, isPopupCookieSet, hasCashbackAnnihilation} = this.params;
                const perks = [];
                if (hasYaPlus) {
                    perks.push(yandexPlusPerk);
                }
                if (hasYaPlusBalance) {
                    perks.push(yandexCashbackPerk);
                }

                await this.browser.setState('Loyalty.collections.perks', perks);

                if (hasCashbackAnnihilation) {
                    await this.browser.setState('CashbackAnnihilator.collections.annihilationInfos', [
                        {
                            wallet_id: 'w/qwerty12345',
                            balance_to_annihilate: '100',
                            currency: 'RUB',
                            annihilation_date: dayjs().add(2, 'day').toISOString(),
                        },
                    ]);
                }
                if (isPopupCookieSet) {
                    await this.browser.yaSetCookie({
                        name: COOKIES.YA_PLUS_ONBOARDING,
                        value: hasCashbackAnnihilation
                            ? YANDEX_PLUS_ONBOARDING_TYPE.CASHBACK_ANNIHILATION
                            : YANDEX_PLUS_ONBOARDING_TYPE.YA_PLUS_WITH_BALANCE,
                    });
                }
                await this.browser.yaOpenPage('market:index');
            },
            'при нажатии на бейдж плюса в шапке, открывается попап с иноформацией о плюсе': makeCase({
                async test() {
                    const {isAuthWithPlugin, isPopupCookieSet} = this.params;
                    // Если пользователь не авторизован или выставлена кука с просмотренным попапом скипаем тест
                    if (isPopupCookieSet || !isAuthWithPlugin) {
                        return this.skip('При клике должна открываться страница плюса');
                    }
                    await this.headerPlusBalance.click();

                    return this.yaPlusContent.isExisting()
                        .should.eventually.to.be.equal(
                            true,
                            'попап должен отображаться'
                        );
                },
            }),
            'при нажатии на бейдж плюса в шапке, выставляется кука с правильным значением': makeCase({
                async test() {
                    const {isAuthWithPlugin, isPopupCookieSet, hasCashbackAnnihilation} = this.params;
                    // Если пользователь не авторизован или выставлена кука с просмотренным попапом скипаем тест
                    if (isPopupCookieSet || !isAuthWithPlugin) {
                        return this.skip('При клике должна открываться страница плюса');
                    }
                    const {hasYaPlus, hasYaPlusBalance} = this.params;
                    await this.headerPlusBalance.click();
                    await this.popupModal.clickOnCrossButton();
                    const yaPlusOnboardingCookie = await this.browser.getCookie(COOKIES.YA_PLUS_ONBOARDING);
                    // копипаста из резолвера src/resolvers/yandexPlusOnboarding/resolveYandexPlusOnboardingType.js
                    let cookieValueType = null;
                    if (hasYaPlus && hasYaPlusBalance) {
                        cookieValueType = YANDEX_PLUS_ONBOARDING_TYPE.YA_PLUS_WITH_BALANCE;
                    }
                    if (hasYaPlus && !hasYaPlusBalance) {
                        cookieValueType = YANDEX_PLUS_ONBOARDING_TYPE.YA_PLUS_WITHOUT_BALANCE;
                    }
                    if (!hasYaPlus && hasYaPlusBalance) {
                        cookieValueType = YANDEX_PLUS_ONBOARDING_TYPE.NON_YA_PLUS_WITH_BALANCE;
                    }
                    if (!hasYaPlus && !hasYaPlusBalance) {
                        cookieValueType = YANDEX_PLUS_ONBOARDING_TYPE.NON_YA_PLUS_WITHOUT_BALANCE;
                    }
                    if (hasCashbackAnnihilation) {
                        cookieValueType = YANDEX_PLUS_ONBOARDING_TYPE.CASHBACK_ANNIHILATION;
                    }
                    // end
                    await this.expect(yaPlusOnboardingCookie.value)
                        .to.be.equal(
                            cookieValueType,
                            `Кука ${COOKIES.YA_PLUS_ONBOARDING} должна хранить значение ${cookieValueType}`
                        );
                },
            }),
            'при нажатии на бейдж плюса в шапке, открывается страница Плюса': makeCase({
                async test() {
                    const {isAuthWithPlugin, isPopupCookieSet} = this.params;
                    // Если пользователь авторизован или не выставлена кука с просмотренным попапом скипаем тест
                    if (!isPopupCookieSet && isAuthWithPlugin) {
                        return this.skip('При клике должен открываться попап');
                    }
                    const tabIds = await this.browser.getTabIds();
                    await this.headerPlusBalance.click();
                    const newTabId = await this.browser.yaWaitForNewTab({startTabIds: tabIds});
                    await this.browser.switchTab(newTabId);
                    return this.browser.getUrl()
                        .should.eventually.to.be.link({
                            hostname: 'plus.yandex.ru',
                            pathname: '/',
                            query: {
                                utm_source: 'market',
                                utm_medium: 'banner',
                                utm_campaign: 'MSCAMP-77',
                                utm_term: 'src_market',
                                utm_content: 'onboarding',
                                message: 'market',
                            },
                        }, {
                            skipProtocol: true,
                        });
                },
            }),
        },
    },
});
