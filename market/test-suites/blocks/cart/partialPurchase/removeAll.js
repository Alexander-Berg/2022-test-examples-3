import {makeSuite, makeCase} from 'ginny';
import CartHeader from '@self/root/src/widgets/content/cart/CartHeader/components/View/__pageObject';
import Checkbox from '@self/root/src/uikit/components/Checkbox/__pageObject';
import {Button} from '@self/root/src/uikit/components/Button/__pageObject';
import Clickable from '@self/root/src/components/Clickable/__pageObject';

export default makeSuite('Удаление всего списка товаров по нажатию на кнопку "Удалить выбранное" при условии, что во всех сниппетах была установлена галочка.', {
    feature: 'Удаление всего списка товаров по нажатию на кнопку "Удалить выбранное" при условии, что во всех сниппетах была установлена галочка',
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
                confirmRemoveButton: () => this.createPageObject(Button, {
                    root: `[data-auto="confirm-remove-popup"] ${Button.root}:nth-child(2)`,
                }),
            });
        },
        'По нажатию на кнопку "Удалить выбранное" должен удалиться весь список товаров при условии, что во всех сниппетах была установлена галочка': makeCase({
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
                    }
                );

                await this.browser.allure.runStep(
                    'Нажать на кнопку "Удалить выбранное"',
                    async () => {
                        await this.removeSelected.click();
                        await this.confirmRemoveButton.click();

                        await this.selectAll.isVisible().should.eventually.be.equal(
                            false,
                            'Чекбокс "Выбрать всё" не отображается'
                        );

                        await this.removeSelected.isExisting().should.eventually.be.equal(
                            false,
                            'Кнопка "Удалить выбранное" не отображается'
                        );
                    }
                );
            },
        }),

    },
});
