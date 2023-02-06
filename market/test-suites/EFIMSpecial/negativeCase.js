import {makeSuite, mergeSuites, makeCase} from 'ginny';

import {setLoyaltyErrorsState} from '@self/root/src/spec/hermione/scenarios/kadavr';
import FatalError from '@self/root/src/components/FatalError/__pageObject';
import {Button} from '@self/root/src/uikit/components/Button/__pageObject';
import FutureCoinNotAvailable from '@self/root/src/components/FutureCoinNotAvailable/__pageObject';
import EFIMExpired from '@self/root/src/components/EFIMExpired/__pageObject';
import {bindBonus, TARGET_STEP} from '@self/project/src/spec/hermione/scenarios/EFIMSpecial';

module.exports = makeSuite('Сценарии с ошибками', {
    issue: 'BLUEMARKET-10732',
    defaultParams: {
        isAuth: true,
    },
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    fatalErrorBlock: () => this.createPageObject(FatalError, {
                        parent: this.futureCoinPopup,
                    }),
                    fatalErrorButton: () => this.createPageObject(Button, {
                        parent: this.futureCoinPopup,
                    }),
                    futureCoinInfoWhenPromoNotAvailable: () =>
                        this.createPageObject(FutureCoinNotAvailable, {
                            parent: this.futureCoinPopup,
                        }),
                    efimExpired: () => this.createPageObject(EFIMExpired, {parent: this.efimCoinBlock}),
                });
            },
        },
        makeSuite('Ошибка при открытии попапа с привязкой', {
            id: 'bluemarket-3417',
            story: {
                async beforeEach() {
                    await this.browser.setState(
                        'Loyalty.collections.promos',
                        []
                    );

                    await this.browser.yaOpenPage('market:special', {
                        semanticId: 'bonusy',
                    });

                    return this.browser.yaScenario(this, bindBonus, TARGET_STEP.FUTURE_COIN_POPUP);
                },
                'Контент попапа должен содержать ошибку': makeCase({
                    async test() {
                        return this.fatalErrorBlock.isExisting()
                            .should.eventually.to.be.equal(
                                true,
                                'Контент попапа, должен содержать ошибку!'
                            );
                    },
                }),
                'Кнопка "Выбрать купоны" должна перезагружать страницу': makeCase({
                    async test() {
                        await this.browser.yaWaitForPageUnloaded(() => this.popupBindButton.click());
                        await this.browser.getUrl()
                            .should.eventually.to.be.link({
                                pathname: '/special/bonusy',
                            }, {
                                mode: 'match',
                                skipProtocol: true,
                                skipHostname: true,
                            });
                        await this.futureCoinPopup.isExisting()
                            .should.eventually.to.be.equal(
                                false,
                                'Попап с ошибкой не должен отображаться после перезагрузки старницы'
                            );
                    },
                }),
            },
        }),
        makeSuite('Ошибка во время привязки купона', {
            id: 'bluemarket-3418',
            story: {
                async beforeEach() {
                    await this.browser.yaScenario(this, setLoyaltyErrorsState, {
                        handlerName: 'createBonusByPromoId',
                        code: 'SOME_ERROR',
                        message: 'Some message',
                        statusCode: 422,
                    });

                    await this.browser.yaOpenPage('market:special', {
                        semanticId: 'bonusy',
                    });

                    return this.browser.yaScenario(this, bindBonus, TARGET_STEP.BIND_BONUS_FAIL);
                },
                'Открыт попап "Похоже купон уже разобрали"': makeCase({
                    async test() {
                        return this.futureCoinInfoWhenPromoNotAvailable.isExisting()
                            .should.eventually.to.be.equal(
                                true,
                                'Попап "Похоже купон уже разобрали", должен отображаться'
                            );
                    },
                }),
            },
        }),
        makeSuite('Ожидаемая ошибка PROMO_GROUP_EXPIRED', {
            id: 'bluemarket-3416',
            story: {
                async beforeEach() {
                    await this.browser.yaScenario(this, setLoyaltyErrorsState, {
                        handlerName: 'getFutureBonusesByPromoGroupToken',
                        code: 'PROMO_GROUP_EXPIRED',
                        message: 'Some message',
                        statusCode: 422,
                    });
                    return this.browser.yaOpenPage('market:special', {
                        semanticId: 'bonusy',
                    });
                },
                'Экран "Акция закончилась" отображается': makeCase({
                    test() {
                        return this.efimExpired.isExisting()
                            .should.eventually.be.equal(
                                true,
                                'Блок с концом акции должен отображаться'
                            );
                    },
                }),
            },
        }),
        makeSuite('Непредвиденная ошибка при открытии страницы', {
            id: 'bluemarket-3415',
            story: {
                async beforeEach() {
                    await this.browser.yaScenario(this, setLoyaltyErrorsState, {
                        handlerName: 'getFutureBonusesByPromoGroupToken',
                        code: 'UNKNOWN_ERROR',
                        message: 'Some message',
                        statusCode: 422,
                    });
                    return this.browser.yaOpenPage('market:special', {
                        semanticId: 'bonusy',
                    });
                },
                'Экран "Акция закончилась" отображается': makeCase({
                    test() {
                        return this.efimExpired.isExisting()
                            .should.eventually.be.equal(
                                true,
                                'Блок с концом акции должен отображаться'
                            );
                    },
                }),
            },
        })
    ),
});
