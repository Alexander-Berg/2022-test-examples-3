import {
    makeCase,
    makeSuite,
    mergeSuites,
    prepareSuite,
} from 'ginny';

import Promocode from '@self/root/src/components/Promocode/__pageObject';
import CheckoutButton
    from '@self/root/src/widgets/content/cart/CartCheckoutControl/components/CartCheckoutButton/__pageObject';
import JawerlyCostError
    from '@self/root/src/widgets/content/cart/CartCheckoutControl/components/JawerlyCostError/__pageObject';
import TooltipWithOpener from '@self/root/src/components/TooltipWithOpener/__pageObject';
import Tooltip from '@self/root/src/uikit/components/AbstractTooltip/__pageObject';
import OrderTotal from '@self/root/src/components/OrderTotalV2/__pageObject';

import TotalInfoA11ySuite from './totalInfo.a11y';

/**
 * Проверяет информацию в виджете CartTotalInformation
 * Требует задания PageObject - this.cartTotalInformation
 */
module.exports = makeSuite('Общая информация о корзине.', {
    environment: 'kadavr',

    params: {
        bonus: 'Информация о бонусе',
        totalPrice: 'Общая сумма заказа',
        checkoutBtnDisabled: 'Должна ли быть не доступна кнопка перехода в чекаут',
        checkoutBtnErrorText: 'Текст об ошибке под кнопкой',
        errorOpenerText: 'Текст в тултипе над ошибкой',
    },

    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    promocode: () => this.createPageObject(Promocode, {
                        parent: this.cartTotalInformation,
                    }),
                    checkoutButton: () => this.createPageObject(CheckoutButton, {
                        parent: this.cartTotalInformation,
                    }),
                    jawerlyCostError: () => this.createPageObject(JawerlyCostError, {
                        parent: this.cartTotalInformation,
                    }),
                    errorOpener: () => this.createPageObject(TooltipWithOpener, {
                        parent: this.jawerlyCostError,
                    }),
                    tooltip: () => this.createPageObject(Tooltip),
                    orderTotal: () => this.createPageObject(OrderTotal),
                });
            },

            'Содержит ожидаемые данные': makeCase({
                async test() {
                    const {
                        totalPrice,
                    } = this.params;

                    await this.cartTotalInformation.isExisting()
                        .should.eventually.be.equal(
                            true,
                            'Общая информация должна быть видна'
                        );

                    await this.orderTotal.getPriceValue()
                        .should.eventually.be.equal(
                            totalPrice,
                            `Общая стоимость товаров должна быть ${totalPrice}`
                        );

                    await this.promocode.isVisible()
                        .should.eventually.be.equal(
                            true,
                            'Поле ввода промокода должно быть видно'
                        );
                },
            }),

            'Кнопка перехода в чекаут': makeCase({
                async test() {
                    const {
                        checkoutBtnDisabled,
                        checkoutBtnErrorText,
                        errorOpenerText,
                    } = this.params;

                    await this.browser.moveToObject(CheckoutButton.root);
                    await this.checkoutButton.isEnabled()
                        .should.eventually.to.be.equal(
                            !checkoutBtnDisabled,
                            `Кнопка перейти к оформлению должна быть ${checkoutBtnDisabled ? 'не ' : ''}доступна`
                        );

                    if (checkoutBtnErrorText) {
                        await this.jawerlyCostError.getText()
                            .should.eventually.to.contain(
                                checkoutBtnErrorText,
                                'Под кнопкой перехода в чекаут должна быть написана причина блокировки кнопки'
                            );
                    }

                    if (errorOpenerText) {
                        await this.browser.allure.runStep(
                            'Вызываем окошко с информацией о блокировки кнопки',
                            () => this.errorOpener.show()
                        );

                        await this.tooltip.waitForTooltipContentIsVisible();

                        await this.browser.getText(JawerlyCostError.tooltipContent)
                            .should.eventually.to.contain(
                                errorOpenerText,
                                'Сообщение должно иметь верный текст'
                            );
                    }
                },
            }),
        },

        prepareSuite(TotalInfoA11ySuite)
    ),
});
