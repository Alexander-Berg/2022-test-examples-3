import {makeSuite, makeCase} from 'ginny';
import CartHeader from '@self/root/src/widgets/content/cart/CartHeader/components/View/__pageObject';
import Checkbox from '@self/root/src/uikit/components/Checkbox/__pageObject';
import Clickable from '@self/root/src/components/Clickable/__pageObject';

export default makeSuite('Автоматическое изменение состояния чекбокса "Выбрать всё" при изменении состояний чекбоксов в сниппетах и отображение кнопки "Удалить выбранное".', {
    feature: 'Автоматическое изменение состояния чекбокса "Выбрать всё" при изменении состояний чекбоксов в сниппетах и отображение кнопки "Удалить выбранное"',
    environment: 'kadavr',
    story: {
        async beforeEach() {
            this.setPageObjects({
                selectAll: () => this.createPageObject(Checkbox, {
                    root: `${CartHeader.root} ${Checkbox.root}`,
                }),
                removeSelected: () => this.createPageObject(Clickable, {
                    root: `${CartHeader.root} ${Clickable.root}`,
                }),
            });
        },
        'В чекбоксе "Выбрать всё" автоматически. Кнопка "Удалить выбранное" должна отображаться при условии, что в сниппете установлен чекбокс': makeCase({
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

                        await this.selectAll.isChecked().should.eventually.be.equal(
                            true,
                            'В чекбоксе "Выбрать всё" галочка установлена'
                        );

                        await this.removeSelected.isVisible().should.eventually.be.equal(
                            true,
                            'Отображается кнопка "Удалить выбранное"'
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Убрать галочку у первого сниппета',
                    async () => {
                        await this.firstCheckbox.toggle();

                        await this.selectAll.isChecked().should.eventually.be.equal(
                            false,
                            'В чекбоксе "Выбрать всё" галочка не установлена'
                        );

                        await this.removeSelected.isVisible().should.eventually.be.equal(
                            true,
                            'Отображается кнопка "Удалить выбранное"'
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Перезагрузить страницу',
                    async () => {
                        await this.browser.refresh();

                        await this.selectAll.isChecked().should.eventually.be.equal(
                            false,
                            'В чекбоксе "Выбрать всё" галочка не установлена'
                        );

                        await this.removeSelected.isVisible().should.eventually.be.equal(
                            true,
                            'Отображается кнопка "Удалить выбранное"'
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'В первом сниппете установить галочку',
                    async () => {
                        await this.firstCheckbox.toggle();

                        await this.firstCheckbox.isChecked().should.eventually.be.equal(
                            true,
                            'В первом сниппете установлена галочка'
                        );

                        await this.selectAll.isChecked().should.eventually.be.equal(
                            true,
                            'В чекбоксе "Выбрать всё" галочка установлена'
                        );

                        await this.removeSelected.isVisible().should.eventually.be.equal(
                            true,
                            'Отображается кнопка "Удалить выбранное"'
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Перезагрузить страницу',
                    async () => {
                        await this.browser.refresh();

                        await this.selectAll.isChecked().should.eventually.be.equal(
                            true,
                            'В чекбоксе "Выбрать всё" галочка установлена'
                        );

                        await this.removeSelected.isVisible().should.eventually.be.equal(
                            true,
                            'Отображается кнопка "Удалить выбранное"'
                        );
                    }
                );
            },
        }),

    },
});
