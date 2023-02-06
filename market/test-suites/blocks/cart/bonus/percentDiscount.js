import {makeSuite, makeCase} from 'ginny';

import bonuses from '@self/root/src/spec/hermione/kadavr-mock/loyalty/bonuses';
import {prepareState} from './prepareState';

module.exports = makeSuite('на процент скидки.', {
    params: {
        percentBonus: 'Купон на процент скидки [получаем из конфига]',
    },
    defaultParams: {
        percentBonus: bonuses.PERCENT,
    },
    story: {
        async beforeEach() {
            return prepareState.call(this, [this.params.percentBonus]);
        },
        'У сниппета появился бейджик.': makeCase({
            id: 'bluemarket-2480',
            async test() {
                // отщелкиваем автопримененный бонус
                await this.bonus.clickOnBonus();
                await this.browser.yaSafeAction(this.orderInfoPreloader.waitForHidden(3500), true);

                await this.coinsBadges.isVisible()
                    .should.eventually.to.be.equal(false, 'Бейджик купона не должен отображаться');

                await this.bonus.clickOnBonus();

                await this.browser.yaSafeAction(this.orderInfoPreloader.waitForHidden(3500), true);

                await this.coinsBadges.isVisible()
                    .should.eventually.to.be.equal(true, 'Бейджик купона должен отображаться');
            },
        }),
        'У купона стал выбран чекбокс.': makeCase({
            id: 'bluemarket-2480',
            async test() {
                // отщелкиваем автопримененный бонус
                await this.bonus.clickOnBonus();
                await this.browser.yaSafeAction(this.orderInfoPreloader.waitForHidden(3500), true);

                await this.bonus.isChecked()
                    .should.eventually.to.be.equal(false, 'Чекбокс купона не должен быть чекнут');

                await this.bonus.clickOnBonus();

                await this.browser.yaSafeAction(this.orderInfoPreloader.waitForHidden(3500), true);

                await this.bonus.isChecked()
                    .should.eventually.to.be.equal(true, 'Чекбокс купона должен быть чекнут');
            },
        }),
    },
});
