import {find} from 'ambar';
import {makeSuite, makeCase, mergeSuites} from 'ginny';
import {applyPromocode, removePromocode} from '@self/root/src/spec/hermione/scenarios/checkout';
import {SummaryPlaceholder} from '@self/root/src/components/OrderTotalV2/components/SummaryPlaceholder/__pageObject';
import promocodes from '@self/root/src/spec/hermione/configs/checkout/promocodes';
import OrderTotal from '@self/root/src/components/OrderTotalV2/__pageObject';
import Promocode from '@self/root/src/components/Promocode/__pageObject';
import SubmitField from '@self/root/src/components/SubmitField/__pageObject';
import PopupSlider from '@self/root/src/components/PopupSlider/__pageObject';

module.exports = makeSuite('Поле ввода промокода.', {
    feature: 'Поле ввода промокода',
    environment: 'kadavr',
    story: {
        async beforeEach() {
            this.setPageObjects({
                orderTotal: () => this.createPageObject(OrderTotal),
                summaryPlaceholder: () => this.createPageObject(SummaryPlaceholder),
                promocodeWrapper: () => this.createPageObject(Promocode),
                promocodeInput: () => this.createPageObject(SubmitField, {parent: this.promocodeWrapper}),
                promocodePopup: () => this.createPageObject(PopupSlider, {parent: this.promocodeWrapper}),
            });
        },
        'Применение промокода.': mergeSuites(
            makeSuite('Активный промокод', {
                issue: 'MARKETFRONT-20302',
                params: {
                    promocode: 'Активный промокод.',
                    discount: 'Скидка по промокоду',
                },
                defaultParams: {
                    promocode: find(promocode => promocode.status === 'ACTIVE', promocodes).code,
                    discount: 10,
                },
                story: {
                    async beforeEach() {
                        await this.browser.setState('Loyalty.collections.promocodes', [
                            {
                                code: this.params.promocode,
                                discount: this.params.discount,
                            },
                        ]);
                    },
                    'При применении промокода отображается корректная скидка': makeCase({
                        id: 'marketfront-4189',
                        async test() {
                            await this.browser.yaScenario(this, removePromocode);
                            await this.browser.yaScenario(this, applyPromocode, this.params.promocode);

                            await this.orderTotal.getPromocodeDiscount(this.params.promocode)
                                .should.eventually.be.equal(
                                    this.params.discount,
                                    'Отображается корректная скидка по промокоду'
                                );

                            let value = await this.orderTotal.getItemsValue();
                            await this.orderTotal.getPriceValue()
                                .should.eventually.be.equal(
                                    value - this.params.discount,
                                    'Итоговая сумма рассчитана с учетом скидки'
                                );

                            await this.browser.yaScenario(this, removePromocode, this.params.promocode);

                            await this.orderTotal.isPromocodeVisible()
                                .should.eventually.be.equal(
                                    false,
                                    'После удаления промокода скидка не отображается'
                                );

                            value = await this.orderTotal.getItemsValue();
                            return this.orderTotal.getPriceValue()
                                .should.eventually.be.equal(
                                    value,
                                    'После удаления промокода итоговая сумма рассчитана без учета скидки'
                                );
                        },
                    }),
                },
            }),
            makeSuite('Неактивный промокод', {
                issue: 'MARKETFRONT-20301',
                params: {
                    promocode: 'Неактивный промокод.',
                    validationErrors: 'Ошибки применения',
                },
                defaultParams: {
                    promocode: find(promocode => promocode.status === 'INACTIVE', promocodes).code,
                },
                story: {
                    'При применении неактивного промокода отображаются корректные данные': makeCase({
                        id: 'marketfront-4188',
                        async test() {
                            await this.browser.yaScenario(this, removePromocode);
                            await this.browser.yaScenario(this, applyPromocode, this.params.promocode);

                            await this.orderTotal.isPromocodeVisible()
                                .should.eventually.be.equal(
                                    false,
                                    'Скидка по промокоду не отображается'
                                );

                            const value = await this.orderTotal.getItemsValue();
                            return this.orderTotal.getPriceValue()
                                .should.eventually.be.equal(value, 'Итоговая сумма рассчитана без скидки');
                        },
                    }),
                },
            })
        ),
    },
});
