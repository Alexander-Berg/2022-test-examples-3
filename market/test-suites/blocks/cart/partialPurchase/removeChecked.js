import {makeSuite, makeCase} from 'ginny';

import {waitForCartActualization} from '@self/root/src/spec/hermione/scenarios/cart';


export default makeSuite('Удаление из списка товара, у которого установлен чек-бокс при условии, что такой товар единственный в списке.', {
    feature: 'Удаление из списка товара, у которого установлен чек-бокс при условии, что такой товар единственный в списке',
    environment: 'kadavr',
    story: {
        'В саммари должно отображаться сообщение "Выберите товары чтобы перейти к оформлению заказа" и кнопка "Перейти к оформлению" должна быть заблокирована': makeCase({
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
                        await this.firstCheckbox.isChecked().should.eventually.be.equal(
                            true,
                            'Во втором сниппете установлена галочка'
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Убрать галочку из первого сниппета',
                    async () => {
                        await this.firstCheckbox.toggle();

                        await this.firstCheckbox.isChecked().should.eventually.be.equal(
                            false,
                            'В первом сниппете галочка не отображается'
                        );
                        await this.secondCheckbox.isChecked().should.eventually.be.equal(
                            true,
                            'Во втором сниппете галочка отображается'
                        );
                    }
                );


                await this.browser.allure.runStep(
                    'Удалить из списка второй сниппет',
                    async () => {
                        await this.secondCartItemRemoveButton.waitForVisible();
                        await this.secondCartItemRemoveButton.click();
                        await this.browser.yaScenario(this, waitForCartActualization);

                        await this.firstCartItem.isExisting().should.eventually.to.be.equal(
                            true,
                            'На странице отображается сниппет без галочки'
                        );
                        await this.secondCartItem.isVisible().should.eventually.to.be.equal(
                            false,
                            'На странице не отображается удаленный сниппет с галочкой'
                        );

                        await this.orderTotal.isVisible().should.eventually.be.equal(
                            false,
                            'Саммари не отображается'
                        );

                        await this.noCheckedItemsInfo.isExisting().should.eventually.be.equal(
                            true,
                            'Отображается сообщение "Выберите товары чтобы перейти к оформлению заказа"'
                        );

                        await this.cartCheckoutButton.isEnabled().should.eventually.be.equal(
                            false,
                            'Заблокирована кнопка "Перейти к оформлению"'
                        );
                    }
                );
            },
        }),

    },
});
