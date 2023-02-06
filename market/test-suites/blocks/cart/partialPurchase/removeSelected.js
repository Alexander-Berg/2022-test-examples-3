import {makeSuite, makeCase} from 'ginny';
import CartHeader from '@self/root/src/widgets/content/cart/CartHeader/components/View/__pageObject';
import Checkbox from '@self/root/src/uikit/components/Checkbox/__pageObject';
import {Button} from '@self/root/src/uikit/components/Button/__pageObject';
import Clickable from '@self/root/src/components/Clickable/__pageObject';
import {waitForCartActualization} from '@self/root/src/spec/hermione/scenarios/cart';

export default makeSuite('Удаление сниппетов с галочками по нажатию на кнопку "Удалить выбранное" при условии, что в части сниппетах была установлена галочка.', {
    feature: 'Удаление сниппетов с галочками по нажатию на кнопку "Удалить выбранное" при условии, что в части сниппетах была установлена галочка',
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
        'По нажатию на кнопку "Удалить выбранное" должны удалиться только те сниппеты, в которых установлена галочка': makeCase({
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
                    'Убрать галочку из второго сниппета. Нажать на кнопку "Удалить выбранное"',
                    async () => {
                        await this.secondCheckbox.toggle();
                        await this.removeSelected.click();
                        await this.confirmRemoveButton.click();
                        await this.browser.yaScenario(this, waitForCartActualization);

                        await this.firstCartItem.isVisible().should.eventually.to.be.equal(
                            true,
                            'На странице отображается сниппет оставшегося товара'
                        );
                        await this.secondCartItem.isVisible().should.eventually.to.be.equal(
                            false,
                            'На странице не отображается сниппет удаленного товара'
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Обновить страницу',
                    async () => {
                        await this.browser.refresh();

                        await this.secondCartItem.isVisible().should.eventually.to.be.equal(
                            false,
                            'После обновления на странице не отображается сниппет удаленного товара'
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
