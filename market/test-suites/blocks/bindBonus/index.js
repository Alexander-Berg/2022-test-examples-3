import {makeSuite, mergeSuites, prepareSuite} from 'ginny';

import promoMock from '@self/root/src/spec/hermione/kadavr-mock/loyalty/promos';
import BindBonus from '@self/root/src/components/BindBonus/__pageObject';
import {profiles} from '@self/project/src/spec/hermione/configs/profiles';

import bindAuthSuite from './bindAuth';
import bindUnauthSuite from './bindUnauth';

module.exports = makeSuite('Виджет привязки купона.', {
    environment: 'kadavr',
    feature: 'Виджет привязки купона',
    params: {
        isAuth: 'Авторизован ли пользователь',
        promo: 'Проверяемая промо-акция',
        isAutoBind: 'Привязка происходит автоматически',
        isPromoBindAvailable: 'Указывает, доступен ли купон для привязки',
        // параметр нужен, чтобы корректно дождаться загрузки первой страницы, если это страница паспорта
        isPassportPageFirst: 'Начальная страница теста - паспорт',
    },
    defaultParams: {
        promo: promoMock.default,
        isAutoBind: false,
        isPromoBindAvailable: true,
        isPassportPageFirst: false,
    },
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    bindBonus: () => this.createPageObject(BindBonus, {parent: this.bindBonusPage}),
                });

                if (this.params.isAuth) {
                    const profile = profiles['pan-topinambur'];

                    await this.browser.yaLogin(profile.login, profile.password);
                }

                await setKadavrState.call(this);
                return goToTargetPage.call(this);
            },
            afterEach() {
                if (this.params.isAuth) {
                    return this.browser.yaLogout();
                }
            },
        },

        makeSuite('Авторизованный пользователь.', {
            defaultParams: {
                isAuthWithPlugin: true,
            },
            story: mergeSuites(
                prepareSuite(bindAuthSuite, {})
            ),
        }),

        makeSuite('Неавторизованный пользователь.', {
            defaultParams: {
                isAuth: false,
            },
            story: mergeSuites(
                prepareSuite(bindUnauthSuite, {})
            ),
        })
    ),
});

/**
 * Устанавливает бонус доступный или недоступный к применению в стейт кадавра
 */
async function setKadavrState() {
    let promos = [this.params.promo];

    if (!this.params.isPromoBindAvailable) {
        promos = [{
            ...this.params.promo,
            id: 'unknown',
        }];
    }

    return this.browser.setState('Loyalty', {
        collections: {promos},
    });
}

/**
 * Устанавливает правильные параметры для автоматической привязки или отображения страницы с превью акции
 */
function goToTargetPage() {
    const pageParams = {
        token: this.params.promo.id,
    };

    if (!this.params.isAutoBind) {
        // автопривязка происходит, если параметр source не указан
        pageParams.source = this.params.promo.promoCode;
    }

    return this.browser.yaOpenPage('market:bonus-bind', pageParams);
}
