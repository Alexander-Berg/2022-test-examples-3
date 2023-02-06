import {makeSuite, makeCase} from 'ginny';

import CartOffer from '@self/root/src/widgets/content/cart/CartList/components/CartOffer/__pageObject';
import AmountSelect from '@self/root/src/components/AmountSelect/__pageObject';
import bonuses from '@self/root/src/spec/hermione/kadavr-mock/loyalty/bonuses';
import {waitForCartActualization} from '@self/root/src/spec/hermione/scenarios/cart';
import {prepareState} from './prepareState';


module.exports = makeSuite('Автоприменение купона.', {
    issue: 'MARKETFRONT-18092',
    params: {
        bonus: 'Купон фиксированной скидки [получаем из конфига]',
    },
    defaultParams: {
        bonus: bonuses.FIXED,
    },
    story: {
        'Состав корзины позволяет применить купон.': {
            id: 'bluemarket-3383',
            async beforeEach() {
                await prepareState.call(this, [this.params.bonus]);
            },
            'Купон применяется автоматически.': makeCase({
                async test() {
                    await this.bonus.isChecked()
                        .should.eventually.to.be.equal(true, 'Чекбокс купона должен быть чекнут');
                },
            }),
            'При отжатии автоприменного купона - купон не автоприменяется вновь.': makeCase({
                async test() {
                    await this.bonus.clickOnBonus();
                    await this.browser.yaSafeAction(this.orderInfoPreloader.waitForHidden(3500), true);

                    await this.bonus.isChecked()
                        .should.eventually.to.be.equal(false, 'Чекбокс купона должен быть отжатым');
                },
            }),
        },
        'Состав корзины не удовлетворяет требования купона имеющегося у пользователя': {
            id: 'bluemarket-3749',
            async beforeEach() {
                this.setPageObjects({
                    cartOffer: () => this.createPageObject(
                        CartOffer,
                        {parent: this.cartItem}
                    ),
                    amountSelect: () => this.createPageObject(AmountSelect, {parent: this.cartOffer}),
                });
                return prepareState.call(this, []);
            },
            'После изменений состава корзины, пришедший купон автоприменися': makeCase({
                async test() {
                    await this.cartBonuses.isExisting()
                        .should.eventually.to.be.equal(false, 'Блок с купонами не должен отображаться');

                    /**
                     * Обновляем стейт кадавра, что бы пришел бонус после изменения кол-ва элементов
                     * Остальные данные актуализации роли не играют
                     */
                    await this.browser.setState('Loyalty.collections', {
                        bonus: [this.params.bonus],
                        disabledBonuses: [],
                    });

                    await this.amountSelect.plusFromButton();
                    await this.browser.yaScenario(this, waitForCartActualization);

                    await this.bonus.isChecked()
                        .should.eventually.to.be.equal(true, 'Купон должен быть примененным');
                },
            }),
        },
    },
});
