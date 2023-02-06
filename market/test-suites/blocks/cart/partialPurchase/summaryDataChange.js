import {makeSuite, makeCase} from 'ginny';

import {buildFormattedRoundedWeightWithoutSpaces} from '@self/root/src/entities/cargo';
import {waitForCartActualization} from '@self/root/src/spec/hermione/scenarios/cart';
import {
    getTotalItemsCount,
    getTotalPrice,
    getTotalWeight,
} from './helpres';

export default makeSuite('Изменение данных в саммари при установки/снятии чекбоксов в сниппете.', {
    feature: 'Изменение данных в саммари при установки/снятии чекбоксов в сниппете',
    environment: 'kadavr',
    story: {
        'При установки/снятии чекбокса данные в саммари должны соответствовать выбранным товарам': makeCase({
            async test() {
                await this.browser.allure.runStep(
                    'Открыть страницу корзины',
                    async () => {
                        await this.firstCartItem.isVisible().should.eventually.to.be.equal(
                            true,
                            'На странице отображается сниппет первого добавленного товара'
                        );
                        await this.secondCartItem.isVisible().should.eventually.to.be.equal(
                            true,
                            'На странице отображается сниппет второго добавленного товара'
                        );

                        await this.firstCheckbox.isChecked().should.eventually.be.equal(
                            true,
                            'В первом сниппете установлена галочка'
                        );
                        await this.secondCheckbox.isChecked().should.eventually.be.equal(
                            true,
                            'Во втором сниппете установлена галочка'
                        );

                        const itemsCount = getTotalItemsCount(this.params.carts);
                        await this.orderTotal.getItemsCount().should.eventually.be.equal(
                            itemsCount,
                            `В саммари отображается количество товаров: ${itemsCount}`
                        );

                        const weight = getTotalWeight(this.params.carts);
                        const formattedWeight = buildFormattedRoundedWeightWithoutSpaces(weight);
                        await this.orderTotal.getItemsWeightValueText().should.eventually.be.equal(
                            formattedWeight,
                            `В саммари отображается вес заказа: ${formattedWeight}`
                        );

                        const price = getTotalPrice(this.params.carts);
                        await this.orderTotal.getPriceValue()
                            .should.eventually.be.equal(
                                price,
                                `В саммари отображается итого: ${price}`
                            );
                    }
                );

                await this.browser.allure.runStep(
                    'Убрать галочку из первого сниппета',
                    async () => {
                        await this.firstCheckbox.toggle();
                        await this.browser.yaScenario(this, waitForCartActualization);

                        await this.firstCheckbox.isChecked().should.eventually.be.equal(
                            false,
                            'В первом сниппете галочка не отображается'
                        );

                        await this.secondCheckbox.isChecked().should.eventually.be.equal(
                            true,
                            'Во втором сниппете галочка отображается'
                        );

                        const itemsCount = getTotalItemsCount(this.params.carts, 0);
                        await this.orderTotal.getItemsCount().should.eventually.be.equal(
                            itemsCount,
                            `В саммари отображается количество товаров: ${itemsCount}`
                        );

                        const weight = getTotalWeight(this.params.carts, 0);
                        const formattedWeight = buildFormattedRoundedWeightWithoutSpaces(weight);
                        await this.orderTotal.getItemsWeightValueText().should.eventually.be.equal(
                            formattedWeight,
                            `В саммари отображается вес только выбранного товара: ${formattedWeight}`
                        );

                        const price = getTotalPrice(this.params.carts, 0);
                        await this.orderTotal.getPriceValue()
                            .should.eventually.be.equal(
                                price,
                                `В саммари отображается стоимость только выбранного товара: ${price}`
                            );
                    }
                );


                await this.browser.allure.runStep(
                    'Установить галочку в первом сниппете',
                    async () => {
                        await this.firstCheckbox.toggle();
                        await this.browser.yaScenario(this, waitForCartActualization);

                        await this.firstCheckbox.isChecked().should.eventually.be.equal(
                            true,
                            'В первом сниппете галочка отображается'
                        );

                        await this.secondCheckbox.isChecked().should.eventually.be.equal(
                            true,
                            'Во втором сниппете галочка отображается'
                        );

                        const itemsCount = getTotalItemsCount(this.params.carts);
                        await this.orderTotal.getItemsCount().should.eventually.be.equal(
                            itemsCount,
                            `В саммари отображается количество товаров: ${itemsCount}`
                        );

                        const weight = getTotalWeight(this.params.carts);
                        const formattedWeight = buildFormattedRoundedWeightWithoutSpaces(weight);
                        await this.orderTotal.getItemsWeightValueText().should.eventually.be.equal(
                            formattedWeight,
                            `В саммари отображается вес заказа: ${formattedWeight}`
                        );

                        const price = getTotalPrice(this.params.carts);
                        await this.orderTotal.getPriceValue()
                            .should.eventually.be.equal(
                                price,
                                `В саммари отображается итого: ${price}`
                            );
                    }
                );
            },
        }),

    },
});
