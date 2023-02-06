import {makeCase, makeSuite, mergeSuites} from 'ginny';

import {Button} from '@self/root/src/uikit/components/Button/__pageObject';
import CoinPopup from '@self/root/src/components/CoinPopup/View/__pageObject';
import PopupModal from '@self/root/src/components/PopupModal/__pageObject';
import SimpleCoin from '@self/root/src/components/CoinPopup/SimpleCoin/__pageObject';
import Notification from '@self/root/src/components/Notification/__pageObject';

module.exports = makeSuite('Привязка купона.', {
    id: 'bluemarket-2800',
    issue: 'BLUEMARKET-9351',
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    bonusButton: () => this.createPageObject(Button, {parent: this.bindBonus}),
                    coinPopupWrapper: () => this.createPageObject(CoinPopup),
                    coinPopup: () => this.createPageObject(PopupModal, {parent: this.coinPopupWrapper}),
                    coin: () => this.createPageObject(SimpleCoin, {parent: this.coinPopup}),
                    notification: () => this.createPageObject(Notification),
                });
            },
        },

        makeSuite('По кнопке.', {
            story: {
                'При клике на кнопку "Получить купон"': {
                    beforeEach() {
                        return this.browser.yaWaitForChangeUrl(
                            () => this.bonusButton.click(),
                            5000
                        );
                    },

                    'для купона доступного для привязки': {
                        'происходит переход в коллекцию купонов и отображается попап с новым купоном': makeCase({
                            test() {
                                return checkIfBindSuccess.call(this);
                            },
                        }),
                    },

                    'для купона недоступного для привязки': {
                        'происходит переход в коллекцию купонов и отображается нотификация с ошибкой': makeCase({
                            defaultParams: {
                                isPromoBindAvailable: false,
                            },

                            test() {
                                return checkIfBindFail.call(this);
                            },
                        }),
                    },
                },
            },
        }),

        // происходит после логина
        makeSuite('Автоматически.', {
            defaultParams: {
                isAutoBind: true,
            },
            story: {
                'Для купона доступного для привязки': {
                    'происходит переход в коллекцию купонов и отображается попап с новым купоном': makeCase({
                        test() {
                            return checkIfBindSuccess.call(this);
                        },
                    }),
                },

                'Для купона недоступного для привязки': {
                    'происходит переход в коллекцию купонов и отображается нотификация с ошибкой': makeCase({
                        defaultParams: {
                            isPromoBindAvailable: false,
                        },

                        test() {
                            return checkIfBindFail.call(this);
                        },
                    }),
                },
            },
        })
    ),
});


async function checkIfBindSuccess() {
    await this.browser.getUrl()
        .should.eventually.to.be.link({
            pathname: '/bonus',
            query: {
                bonusId: this.params.promo.id,
            },
        }, {
            skipProtocol: true,
            skipHostname: true,
        });

    await this.coinPopup.waitForVisible();

    await this.coinPopup.isVisible()
        .should.eventually.to.be.equal(
            true, 'Попап с купоном должен отображаться'
        );

    return this.coin.isVisible()
        .should.eventually.to.be.equal(
            true, 'Купон в попапе должен отображаться'
        );
}

async function checkIfBindFail() {
    await this.browser.getUrl()
        .should.eventually.to.be.link({
            pathname: '/bonus',
        }, {
            skipProtocol: true,
            skipHostname: true,
            // параметр удаляется эпиком, тест не успевает его считать
            skipQuery: true,
        });

    await this.notification.waitForNotificationVisible();
    await this.notification.isVisible()
        .should.eventually.to.be.equal(
            true, 'Нотификация об ошибке привязки должно отображаться'
        );

    return this.notification.getText()
        .should.eventually.to.be.equal(
            'Купон неактивен',
            'Нотификация об ошибке привязки купона должо иметь корректный текст'
        );
}
