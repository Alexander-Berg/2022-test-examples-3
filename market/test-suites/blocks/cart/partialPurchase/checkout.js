import {makeSuite, makeCase} from 'ginny';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import ConfirmationPage
    from '@self/root/src/widgets/content/checkout/layout/CheckoutLayoutConfirmationPage/view/__pageObject';
import CheckoutSummary
    from '@self/root/src/components/CheckoutSummary/__pageObject';
import OrderTotal from '@self/root/src/components/OrderTotal/__pageObject';
import {goToConfirmationPage} from '@self/root/src/spec/hermione/scenarios/checkout/goToConfirmationPage';
import userFormData from '@self/root/src/spec/hermione/configs/checkout/formData/user-prepaid';
import {moscowAddress} from '@self/platform/spec/hermione/test-suites/blocks/checkout2/hsch/saveStateBetweenPages/mocks';
import GroupedParcel
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/GroupedParcel/__pageObject';
import EditableCard from '@self/root/src/components/EditableCard/__pageObject';
import AddressCard from '@self/root/src/components/AddressCard/__pageObject';
/**
 * @ifLose заменить на старые импорты из .../components/DeliveryIntervals/__pageObject
 * @expFlag all_checkout_new_intervals [обратный эксперимент]
 * @ticket MARKETFRONT-58113
 * @start
 */
import {
    DateSelect,
    // eslint-disable-next-line max-len
} from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/DateIntervalSelector/__pageObject';

import {
    TimeSelect,
    // eslint-disable-next-line max-len
} from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/TimeIntervalSelector/__pageObject';
/**
 * @expFlag all_checkout_new_intervals [обратный эксперимент]
 * @ticket MARKETFRONT-58113
 * @end
 */
import {SelectPopover} from '@self/root/src/components/Select/__pageObject';
import {prepareUserLastState} from '@self/root/src/spec/hermione/scenarios/persAddressResource';
import {SummaryPlaceholder} from '@self/root/src/components/OrderTotalV2/components/SummaryPlaceholder/__pageObject';
import {ACTUALIZATION_TIMEOUT} from '@self/root/src/spec/hermione/scenarios/checkout';
import InnerPayment
    from '@self/root/src/components/InnerPayment/__pageObject';
import CheckoutOrderButton
    from '@self/root/src/widgets/content/checkout/common/CheckoutOrderButton/components/View/__pageObject';
import EditPaymentOption from '@self/root/src/components/EditPaymentOption/__pageObject';
import Modal from '@self/root/src/components/PopupBase/__pageObject';
import PaymentOptionsList from '@self/root/src/components/PaymentOptionsList/__pageObject';
import OrderConfirmation from '@self/root/src/spec/page-objects/OrderConfirmation';
import OrderDelivery from '@self/root/src/widgets/parts/OrderConfirmation/components/OrderDelivery/__pageObject';
import {waitForCartActualization} from '@self/root/src/spec/hermione/scenarios/cart';
import {
    getTotalItemsCount,
    getTotalPrice,
} from './helpres';

export default makeSuite('Оформление заказа с выбранными товарами с последующим отображением в корзине не выбранных товаров.', {
    feature: 'Оформление заказа с выбранными товарами с последующим отображением в корзине не выбранных товаров',
    environment: 'kadavr',
    story: {
        async beforeEach() {
            this.setPageObjects({
                groupedParcel: () => this.createPageObject(GroupedParcel),
                editGroupedParcelCard: () => this.createPageObject(
                    EditableCard,
                    {parent: this.groupedParcel}
                ),
                addressCard: () => this.createPageObject(AddressCard, {
                    parent: this.editGroupedParcelCard,
                }),
                dateSelect: () => this.createPageObject(DateSelect),
                timeSelect: () => this.createPageObject(TimeSelect),
                selectPopover: () => this.createPageObject(SelectPopover),
                confirmationPage: () => this.createPageObject(ConfirmationPage),
                summary: () => this.createPageObject(CheckoutSummary, {
                    parent: this.confirmationPage,
                }),
                orderTotal: () => this.createPageObject(OrderTotal, {
                    parent: this.summary,
                }),
                summaryPlaceholder: () => this.createPageObject(SummaryPlaceholder),
                innerPayment: () => this.createPageObject(InnerPayment),
                checkoutOrderButton: () => this.createPageObject(CheckoutOrderButton, {
                    parent: this.confirmationPage,
                }),


                paymentOptionsBlock: () => this.createPageObject(EditPaymentOption, {
                    parent: this.confirmationPage,
                }),
                paymentOptionsEditableCard: () => this.createPageObject(EditableCard, {
                    parent: this.paymentOptionsBlock,
                }),
                paymentOptionsModal: () => this.createPageObject(Modal, {
                    root: `${Modal.root} [data-auto="editableCardPopup"]`,
                }),
                paymentOptionsPopUpContent: () => this.createPageObject(PaymentOptionsList),

                orderConfirmation: () => this.createPageObject(OrderConfirmation),
                firstOrderDelivery: () => this.createPageObject(OrderDelivery, {
                    root: `${OrderConfirmation.firstDelivery} ${OrderDelivery.root}`,
                }),
                secondOrderDelivery: () => this.createPageObject(OrderDelivery, {
                    root: `${OrderConfirmation.secondDelivery} ${OrderDelivery.root}`,
                }),
            });

            await this.browser.yaScenario(this, prepareUserLastState);
        },
        'У пользователя должны быть возможно оформить заказ с только выбранными товарами': makeCase({
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
                    'Оставить галочку только у первого сниппета в списке. Перейти к оформлению',
                    async () => {
                        await this.secondCheckbox.toggle();
                        await this.browser.yaScenario(this, waitForCartActualization);

                        await this.cartCheckoutButton.waitForButtonEnabled();
                        await this.cartCheckoutButton.goToCheckout();

                        const [openedUrl, expectedPath] = await Promise.all([
                            this.browser.getUrl(),
                            this.browser.yaBuildURL(PAGE_IDS_COMMON.CHECKOUT2),
                        ]);

                        await this.expect(openedUrl).to.be.link({pathname: expectedPath}, {
                            skipProtocol: true,
                            skipHostname: true,
                        });

                        await this.browser.yaScenario(this, goToConfirmationPage, {
                            userFormData,
                            addressFormData: moscowAddress,
                        });

                        const itemsCount = getTotalItemsCount(this.params.carts, 1);
                        await this.orderTotal.getItemsCount().should.eventually.be.equal(
                            itemsCount,
                            `В саммари чекаута отображается количество выбранных товаров: ${itemsCount}`
                        );

                        const price = getTotalPrice(this.params.carts, 1);
                        await this.orderTotal.getItemsValue()
                            .should.eventually.be.equal(
                                price,
                                `В саммари чекаута отображается стоимость только выбранного товара: ${price}`
                            );
                    }
                );

                await this.browser.allure.runStep(
                    'Перезагрузить страницу',
                    async () => {
                        await this.browser.refresh();
                        await this.confirmationPage.waitForVisible();
                        if (await this.summaryPlaceholder.waitForVisible(1000)) {
                            await this.summaryPlaceholder.waitForHidden(ACTUALIZATION_TIMEOUT);
                        }

                        const itemsCount = getTotalItemsCount(this.params.carts, 1);
                        await this.orderTotal.getItemsCount().should.eventually.be.equal(
                            itemsCount,
                            `В саммари чекаута отображается количество выбранных товаров: ${itemsCount}`
                        );

                        const price = getTotalPrice(this.params.carts, 1);
                        await this.orderTotal.getItemsValue()
                            .should.eventually.be.equal(
                                price,
                                `В саммари чекаута отображается стоимость только выбранного товара: ${price}`
                            );
                    }
                );

                await this.browser.allure.runStep('Нажать на кнопку "Перейти к оплате"',
                    async () => {
                        await this.paymentOptionsEditableCard.changeButtonClick();
                        await this.paymentOptionsModal.waitForVisible();

                        await this.paymentOptionsPopUpContent.setPaymentTypeCashOnDelivery();
                        await this.paymentOptionsPopUpContent.submitButtonClick();
                        await this.paymentOptionsModal.waitForInvisible();

                        if (await this.summaryPlaceholder.waitForVisible(1000)) {
                            await this.summaryPlaceholder.waitForHidden(ACTUALIZATION_TIMEOUT);
                        }
                        await this.checkoutOrderButton.waitForEnabledButton();
                        await this.checkoutOrderButton.click();
                    });

                await this.browser.allure.runStep('Открывается страница спасибки',
                    async () => {
                        await this.browser.yaWaitForChangeUrl(null, 60000);
                        await this.browser.getUrl()
                            .should.eventually.to.be.link({
                                query: {
                                    orderId: /\d+/,
                                },
                                pathname: '/my/orders/confirmation',
                            }, {
                                mode: 'match',
                                skipProtocol: true,
                                skipHostname: true,
                            });

                        await this.orderConfirmation.waitForCheckoutThankyouIsVisible();

                        await this.firstOrderDelivery.isVisible().should.eventually.to.be.equal(
                            true,
                            'На странице отображается информация о доставке оформленного товара'
                        );
                        await this.secondOrderDelivery.isVisible().should.eventually.to.be.equal(
                            false,
                            'На странице не отображается информация о доставке не оформленного товара'
                        );
                        await this.orderConfirmation.nextOrder.isVisible().should.eventually.to.be.equal(
                            true,
                            'На странице отображается блок "Продолжить покупки"'
                        );
                    }
                );
            },
        }),

    },
});
