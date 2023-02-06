import {makeCase, makeSuite} from 'ginny';

import ParcelTitle from '@self/root/src/widgets/content/cart/CartList/components/ParcelTitle/__pageObject';
import CartErrors from '@self/root/src/widgets/content/cart/CartList/components/CartErrors/__pageObject';
import CartListPopup from '@self/root/src/widgets/content/cart/CartList/components/CartListPopup/__pageObject';

import {getDeliveryTitle} from './constants';

/**
 * Проверяет посылку в корзине
 * Требует задания PageObject - this.cart
 */
module.exports = makeSuite('Информация о посылке.', {
    environment: 'kadavr',

    params: {
        date: 'Дата доставки',
        supplier: 'Доставщик',
        count: 'Количество оферов в посылке',
        errorText: 'Текст об ошибке в корзине',
        errorPopupTitle: 'Заголовок в попапе с информацией об ошибке',
        errorPopupText: 'Текст в попапе с информацией об ошибке',
        isExpress: 'Экспресс-доставка',
    },

    story: {
        async beforeEach() {
            this.setPageObjects({
                title: () => this.createPageObject(ParcelTitle, {
                    parent: this.cart,
                }),
                сartError: () => this.createPageObject(CartErrors, {
                    parent: this.cart,
                }),
                errorPopup: () => this.createPageObject(CartListPopup),
            });
        },

        'Содержит ожидаемые данные': makeCase({
            async test() {
                const {date, supplier, count, errorText, isExpress} = this.params;
                await this.title.waitForVisible();

                if (isExpress) {
                    await this.title.getContent()
                        .should.eventually.to.match(
                            /^Экспресс-доставка Яндекса/,
                            'Заголовок должен начинаться с "Экспресс-доставка Яндекса"'
                        );
                } else {
                    await this.title.getContent()
                        .should.eventually.to.match(
                            new RegExp(`^${getDeliveryTitle(supplier, date)}`),
                            'Заголовок должен содержать стоимость и доставщика'
                        );
                }

                await this.cart.getItemsCount()
                    .should.eventually.be.equal(
                        count,
                        `Количество оферов в посылке должно быть: ${count}`
                    );

                if (errorText) {
                    await this.сartError.getText()
                        .should.eventually.to.contain(
                            errorText,
                            'У посылки должно быть ожидаемое предупреждение'
                        );
                }
            },
        }),

        'При клике на ошибку, открывает всплывашку': makeCase({
            async test() {
                const {errorPopupText, errorPopupTitle} = this.params;

                if (!errorPopupText) {
                    return this.skip('Скипаем тест если не собираемся проверять текст всплывашки');
                }
                await this.сartError.waitForVisible();
                await this.сartError.click();

                await this.errorPopup.waitForVisible();

                await this.errorPopup.getTitleText()
                    .should.eventually.to.contain(
                        errorPopupTitle,
                        'У попапа предупреждения должен быть соответсвующий заголовок'
                    );

                await this.errorPopup.getContentText()
                    .should.eventually.to.contain(
                        errorPopupText,
                        'У попапа предупреждения должен быть соответсвующий текст'
                    );
            },
        }),
    },
});
