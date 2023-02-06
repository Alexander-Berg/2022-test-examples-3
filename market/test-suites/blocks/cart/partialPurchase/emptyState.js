import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Изменение блока саммари, когда ни у одного сниппета не выставлена галочка.', {
    feature: 'Изменение блока саммари, когда ни у одного сниппета не выставлена галочка',
    environment: 'kadavr',
    story: {
        'В саммари должно отображаться сообщение "Выберите товары чтобы перейти к оформлению заказа" и кнопка "Перейти к оформлению" должна быть заблокирована': makeCase({
            async test() {
                await this.browser.allure.runStep(
                    'Отрыть страницу корзины',
                    async () => {
                        await this.firstCartItem.isVisible().should.eventually.to.be.equal(
                            true,
                            'На странице отображается сниппет добавленного товара'
                        );

                        await this.firstCheckbox.isChecked().should.eventually.be.equal(
                            true,
                            'В сниппете установлена галочка'
                        );

                        await this.orderTotal.isItemsValueVisible().should.eventually.be.equal(
                            true,
                            'В саммари отображается стоимость товара'
                        );

                        await this.orderTotal.isItemsValueVisible().should.eventually.be.equal(
                            true,
                            'В саммари отображается стоимость товара'
                        );

                        await this.orderTotal.isPriceValueVisible().should.eventually.be.equal(
                            true,
                            'В саммари отображается итоговая стоимость'
                        );

                        await this.cartCheckoutButton.isEnabled().should.eventually.be.equal(
                            true,
                            'Активна кнопка "Перейти к оформлению"'
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Убрать галочку из сниппета',
                    async () => {
                        await this.firstCheckbox.toggle();
                        await this.firstCheckbox.isChecked().should.eventually.be.equal(
                            false,
                            'В сниппете галочка не отображается'
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
                await this.browser.allure.runStep(
                    'Установить галочку в сниппете',
                    async () => {
                        await this.firstCheckbox.toggle();
                        await this.firstCheckbox.isChecked().should.eventually.be.equal(
                            true,
                            'В сниппете отображается галочка'
                        );

                        await this.noCheckedItemsInfo.isExisting().should.eventually.be.equal(
                            false,
                            'В саммари отсутствует отображение сообщения "Выберите товары чтобы перейти к оформлению заказа"'
                        );

                        await this.orderTotal.isItemsValueVisible().should.eventually.be.equal(
                            true,
                            'В саммари отображается стоимость товара'
                        );

                        await this.orderTotal.isItemsValueVisible().should.eventually.be.equal(
                            true,
                            'В саммари отображается стоимость товара'
                        );

                        await this.orderTotal.isPriceValueVisible().should.eventually.be.equal(
                            true,
                            'В саммари отображается итоговая стоимость'
                        );

                        await this.cartCheckoutButton.isEnabled().should.eventually.be.equal(
                            true,
                            'Активна кнопка "Перейти к оформлению"'
                        );
                    }
                );
            },
        }),
    },
});
