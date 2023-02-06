import {makeSuite, makeCase} from 'ginny';
import CartHeader from '@self/root/src/widgets/content/cart/CartHeader/components/View/__pageObject';
import Checkbox from '@self/root/src/uikit/components/Checkbox/__pageObject';
import Clickable from '@self/root/src/components/Clickable/__pageObject';
import ExpiredCartOffer from '@self/root/src/widgets/content/cart/CartList/components/ExpiredCartOffer/__pageObject';

export default makeSuite('Отсутствие чекбоксов и кнопки "Удалить выбранное" при условии, что в корзине только товары, которые не доставляется в текущий регион.', {
    feature: 'Отсутствие чекбоксов и кнопки "Удалить выбранное" при условии, что в корзине только товары, которые не доставляется в текущий регион',
    environment: 'kadavr',
    story: {
        async beforeEach() {
            this.setPageObjects({
                expiredCartOffer: () => this.createPageObject(ExpiredCartOffer),
                selectAll: () => this.createPageObject(Checkbox, {
                    root: `${CartHeader.root} ${Checkbox.root}`,
                }),
                removeSelected: () => this.createPageObject(Clickable, {
                    root: `${CartHeader.root} ${Clickable.root}`,
                }),
            });
        },
        'На странице корзины должны отсутствовать отображение элементов частичной покупки при условии, что в корзине только товары, которые не доставляется в текущий регион': makeCase({
            async test() {
                await this.browser.allure.runStep(
                    'Открыть страницу корзины',
                    async () => {
                        await this.firstCartItem.isVisible().should.eventually.to.be.equal(
                            true,
                            'На странице отображается сниппет первого добавленного товара'
                        );
                        await this.expiredCartOffer.isVisible().should.eventually.to.be.equal(
                            true,
                            'На странице отображается состояние недоступности товара'
                        );

                        await this.firstCheckbox.isVisible().should.eventually.be.equal(
                            false,
                            'На странице отсутствует отображение чекбокса в сниппете'
                        );

                        await this.selectAll.isVisible().should.eventually.be.equal(
                            false,
                            'На странице отсутствует отображение чекбокса "Выбрать все"'
                        );
                        await this.removeSelected.isExisting().should.eventually.be.equal(
                            false,
                            'На странице отсутствует отображение кнопки "Удалить выбранное"'
                        );
                    }
                );
            },
        }),

    },
});
