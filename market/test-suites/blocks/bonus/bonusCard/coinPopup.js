import {makeSuite, makeCase} from 'ginny';

import Coin from '@self/root/src/uikit/components/Coin/__pageObject';
import SimpleCoin from '@self/root/src/components/CoinPopup/SimpleCoin/__pageObject';
import CoinPopup from '@self/root/src/components/CoinPopup/View/__pageObject';

module.exports = makeSuite('Содержимое попапа', {
    environment: 'kadavr',
    params: {
        coin: null,
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                coin: () => this.createPageObject(Coin),
                coinPopup: () => this.createPageObject(CoinPopup),
                coinInsidePopup: () => this.createPageObject(SimpleCoin, {
                    parent: this.coinPopup,
                }),
            });

            await this.coin.click();

            await this.coinPopup.waitForVisible();

            return this.coinPopup.isVisible().should.eventually.be.equal(
                true,
                'Должен отображаться попап с купоном'
            );
        },

        'Отображается карточка купона': makeCase({
            test() {
                return this.coinInsidePopup.isVisible().should.eventually.to.be.equal(
                    true,
                    'Карточка купона должна отображаться'
                );
            },
        }),
    },
});
