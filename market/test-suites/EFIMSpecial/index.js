import {
    prepareSuite,
    makeSuite,
    mergeSuites,
} from 'ginny';

// mocks
import EFIMSpecialCMSPageMock from '@self/root/src/spec/hermione/kadavr-mock/tarantino/EFIMSpecial';
import efimFutureCoinsMock from '@self/root/src/spec/hermione/kadavr-mock/loyalty/efimFutureCoins';
import promosMock from '@self/root/src/spec/hermione/kadavr-mock/loyalty/promos';
import bindedBonus from '@self/root/src/spec/hermione/kadavr-mock/loyalty/bindedBonus';
import {profiles} from '@self/project/src/spec/hermione/configs/profiles';
// pageObject's
import EFIMCoinBlock from '@self/root/src/widgets/parts/EFIMCoinBlock/components/View/__pageObject';
import {CoinBlock} from '@self/root/src/components/CoinPane/__pageObject';
import Bonus from '@self/root/src/uikit/components/Coin/__pageObject';
import FutureCoinPopup from '@self/root/src/widgets/parts/FutureCoinPopup/components/View/__pageObject';
import CoinPopupCoinInfo from '@self/root/src/components/CoinPopup/Info/__pageObject';
import {Preloader} from '@self/root/src/components/Preloader/__pageObject';
import {Button} from '@self/root/src/uikit/components/Button/__pageObject';
// test Suites
import authBind from '@self/project/src/spec/hermione/test-suites/EFIMSpecial/authBind';
import unauthBind from '@self/project/src/spec/hermione/test-suites/EFIMSpecial/unauthBind';
import expiredCoin from '@self/project/src/spec/hermione/test-suites/EFIMSpecial/expiredCoin';
import endPromo from '@self/project/src/spec/hermione/test-suites/EFIMSpecial/endPromo';
import successBindedPopup from '@self/project/src/spec/hermione/test-suites/EFIMSpecial/successBindedPopup';
import negativeCase from '@self/project/src/spec/hermione/test-suites/EFIMSpecial/negativeCase';

module.exports = makeSuite('ЕФИМ', {
    params: {
        isAuth: 'Пользователь залогинен',
    },
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    efimCoinBlock: () => this.createPageObject(EFIMCoinBlock),
                    coinBlock: () => this.createPageObject(CoinBlock),
                    bonus: () => this.createPageObject(Bonus, {
                        root: `${CoinBlock.root}:first-child`,
                    }),
                    futureCoinPopup: () => this.createPageObject(FutureCoinPopup),
                    coinPopupCoinInfo: () => this.createPageObject(CoinPopupCoinInfo),
                    popupBindButton: () => this.createPageObject(Button, {
                        parent: this.futureCoinPopup,
                    }),
                    futureCoinPopupLoader: () => this.createPageObject(Preloader),
                });

                if (this.params.isAuth) {
                    const profile = profiles['pan-topinambur'];

                    await this.browser.yaLogin(profile.login, profile.password);
                }
            },
            afterEach() {
                if (this.params.isAuth) {
                    return this.browser.yaLogout();
                }
            },
        },
        makeSuite('Активный купон', {
            story: mergeSuites(
                {
                    async beforeEach() {
                        await setKadavrState.call(
                            this,
                            efimFutureCoinsMock.activeCoin,
                            {
                                ...promosMock.active,
                                id: 'somepromokey',
                            }
                        );

                        await this.browser.yaOpenPage('market:special', {
                            semanticId: 'bonusy',
                        });
                    },
                },
                prepareSuite(authBind),
                prepareSuite(unauthBind)
            ),
        }),
        makeSuite('Истекший купон', {
            story: mergeSuites(
                {
                    async beforeEach() {
                        await setKadavrState.call(
                            this,
                            efimFutureCoinsMock.expiredCoin,
                            {
                                ...promosMock.expired,
                                id: 'somepromokey',
                            }
                        );
                        await this.browser.yaOpenPage('market:special', {
                            semanticId: 'bonusy',
                        });
                    },
                },
                prepareSuite(expiredCoin, {
                    params: {
                        isAuth: true,
                    },
                })
            ),
        }),
        makeSuite('Конец акции', {
            story: mergeSuites(
                {
                    async beforeEach() {
                        await setKadavrState.call(
                            this,
                            efimFutureCoinsMock.endPromo,
                            null
                        );
                        await this.browser.yaOpenPage('market:special', {
                            semanticId: 'bonusy',
                        });
                    },
                },
                prepareSuite(endPromo)
            ),
        }),

        makeSuite('Автоматическая привязка', {
            story: mergeSuites(
                {
                    async beforeEach() {
                        await setKadavrState.call(
                            this,
                            efimFutureCoinsMock.activeCoin,
                            {
                                ...promosMock.active,
                                id: 'somepromokey',
                            }
                        );
                        await this.browser.yaOpenPage('market:special', {
                            semanticId: 'bonusy',
                            bonusPromoSource: 'source',
                            forceBind: 1,
                        });
                    },
                },
                prepareSuite(successBindedPopup)
            ),
        }),
        makeSuite('Ошибки', {
            story: mergeSuites(
                {
                    async beforeEach() {
                        await setKadavrState.call(
                            this,
                            efimFutureCoinsMock.activeCoin,
                            {
                                ...promosMock.active,
                                id: 'somepromokey',
                            }
                        );
                    },
                },
                prepareSuite(negativeCase)
            ),
        })
    ),
});

async function setKadavrState(futureCoinsMock, promoCoinMock) {
    /**
     * Tarantino отвечает за cms мок, кидаем туда ответ cms
     */
    await this.browser.setState(
        'Tarantino.data.result',
        [EFIMSpecialCMSPageMock]
    );

    return this.browser.setState(
        'Loyalty.collections', {
            /**
             * Пробрасывает ответ ручки getFutureBonusesByPromoGroupSource
             */
            futureBonusCollection: futureCoinsMock,

            /**
             * Пробрасываем ответ ручки /promo/coin/<id>
             */
            promos: [promoCoinMock],

            /**
             * Пробрасываем ответ ручки /coins/createCoin/v2
             */
            bindedBonusCollection: bindedBonus,
        }
    );
}
