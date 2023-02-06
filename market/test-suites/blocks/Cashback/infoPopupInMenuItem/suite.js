import {makeSuite, makeCase} from 'ginny';
import dayjs from 'dayjs';

// mocks
import {yandexPlusPerk, yandexCashbackPerk} from '@self/root/src/spec/hermione/kadavr-mock/loyalty/perks';
import COOKIES from '@self/root/src/constants/cookie';
import {YANDEX_PLUS_ONBOARDING_TYPE} from '@self/root/src/constants/yaPlus';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

export default makeSuite('Онбординг с информацией о плюсе', {
    params: {
        hasYaPlus: 'есть ли у пользователя подписка плюса',
        isPopupCookieSet: 'выставлена ли кука с показом попапа пользователю',
        // Баланс кешбэка пользователя
        hasYaPlusBalance: 'Баланс кешбэка пользователя',
        hasCashbackAnnihilation: 'Есть ли у пользователя сгорание кешбэка',
        prepareState: 'Функция определяющая состояние приложения под конкретный кейс',
    },
    defaultParams: {
        // авторизован ли пользователь
        isAuthWithPlugin: true,
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
                await this.browser.yaOpenPage(PAGE_IDS_COMMON.INDEX);

                await this.params.prepareState.call(this);
            },
            'При нажатии на пункт меню происходит корректное поведение': makeCase({
                async test() {
                    const {isPopupCookieSet, hasCashbackAnnihilation, hasYaPlus, hasYaPlusBalance} = this.params;

                    // Если выставлена кука с просмотренным попапом
                    if (isPopupCookieSet) {
                        const tabIds = await this.browser.getTabIds();
                        await this.menuItemLink.click();
                        const newTabId = await this.browser.yaWaitForNewTab({startTabIds: tabIds});
                        await this.browser.switchTab(newTabId);
                        await this.browser.getUrl()
                            .should.eventually.to.be.link({
                                hostname: 'plus.yandex.ru',
                                pathname: '/',
                                query: {
                                    utm_source: 'market',
                                    utm_medium: 'link',
                                    utm_campaign: 'MSCAMP-77',
                                    utm_term: 'src_market',
                                    utm_content: 'user_menu',
                                    message: 'market',
                                },
                            }, {
                                skipProtocol: true,
                            });
                    } else {
                        await this.menuItemLink.click();

                        await this.yaPlusContent.isExisting()
                            .should.eventually.to.be.equal(
                                true,
                                'попап должен отображаться'
                            );

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
                    }
                },
            }),
        },
    },
});
