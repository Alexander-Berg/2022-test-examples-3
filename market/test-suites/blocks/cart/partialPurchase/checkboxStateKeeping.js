import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Сохранение состояний чекбоксов в сниппетах при перезагрузки страницы.', {
    feature: 'Сохранение состояний чекбоксов в сниппетах при перезагрузки страницы',
    environment: 'kadavr',
    story: {
        'При перезагрузки страницы должны сохранятся состояние чекбоксов': makeCase({
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
                    'Перезагрузить страницу',
                    async () => {
                        await this.browser.refresh();

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
                    'В первом сниппете установить галочку',
                    async () => {
                        await this.firstCheckbox.toggle();

                        await this.firstCheckbox.isChecked().should.eventually.be.equal(
                            true,
                            'В первом сниппете галочка отображается'
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Во втором сниппете убрать галочку',
                    async () => {
                        await this.secondCheckbox.toggle();

                        await this.secondCheckbox.isChecked().should.eventually.be.equal(
                            false,
                            'Во втором сниппете галочка не отображается'
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Перезагрузить страницу',
                    async () => {
                        await this.browser.refresh();

                        await this.firstCheckbox.isChecked().should.eventually.be.equal(
                            true,
                            'В первом сниппете галочка отображается'
                        );

                        await this.secondCheckbox.isChecked().should.eventually.be.equal(
                            false,
                            'Во втором сниппете галочка не отображается'
                        );
                    }
                );
            },
        }),

    },
});
