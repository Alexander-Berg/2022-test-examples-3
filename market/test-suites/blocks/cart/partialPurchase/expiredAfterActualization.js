import {makeSuite, makeCase} from 'ginny';
import CartHeader from '@self/root/src/widgets/content/cart/CartHeader/components/View/__pageObject';
import Checkbox from '@self/root/src/uikit/components/Checkbox/__pageObject';
import Clickable from '@self/root/src/components/Clickable/__pageObject';
import ExpiredCartOffer from '@self/root/src/widgets/content/cart/CartList/components/ExpiredCartOffer/__pageObject';
import COOKIE_NAME from '@self/root/src/constants/cookie';
import {waitForCartActualization} from '@self/root/src/spec/hermione/scenarios/cart';

export default makeSuite('Выставление чекбокса в сниппете при условии, что выбранный товар был разобран.', {
    feature: 'Выставление чекбокса в сниппете при условии, что выбранный товар был разобран',
    environment: 'kadavr',
    defaultParams: {
        cookie: {
            [COOKIE_NAME.USER_UNCHECKED_CART_ITEM_IDS]: {
                name: COOKIE_NAME.USER_UNCHECKED_CART_ITEM_IDS,
                value: JSON.stringify([1]),
            },
        },
    },
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

            await this.firstCartItem.isVisible().should.eventually.to.be.equal(
                true,
                'На странице отображается сниппет первого добавленного товара'
            );

            await this.firstCheckbox.isChecked().should.eventually.be.equal(
                false,
                'В сниппете галочка не установлена'
            );

            await this.noCheckedItemsInfo.isExisting().should.eventually.be.equal(
                true,
                'Отображается сообщение "Выберите товары чтобы перейти к оформлению заказа"'
            );

            await this.cartCheckoutButton.isEnabled().should.eventually.be.equal(
                false,
                'Заблокирована кнопка "Перейти к оформлению"'
            );
        },
        'Установить галочку в сниппете': makeCase({
            async test() {
                await this.firstCheckbox.toggle();
                await this.browser.yaScenario(this, waitForCartActualization);

                await this.expiredCartOffer.isVisible().should.eventually.to.be.equal(
                    true,
                    'В сниппете отображается сообщение "Разобрано"'
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

                await this.orderTotal.isVisible().should.eventually.be.equal(
                    false,
                    'Саммари не отображается'
                );
            },
        }),
        'Установить чекбокс "Выбрать все"': makeCase({
            async test() {
                await this.selectAll.toggle();
                await this.browser.yaScenario(this, waitForCartActualization);

                await this.expiredCartOffer.isVisible().should.eventually.to.be.equal(
                    true,
                    'В сниппете отображается сообщение "Разобрано"'
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

                await this.orderTotal.isVisible().should.eventually.be.equal(
                    false,
                    'Саммари не отображается'
                );
            },
        }),
    },
});
