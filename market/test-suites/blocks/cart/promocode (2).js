import {find} from 'ambar';
import {
    makeSuite,
    makeCase,
    mergeSuites,
} from 'ginny';
import {applyPromocode, removePromocode} from '@self/root/src/spec/hermione/scenarios/checkout';
import {fillCheckoutCartPage, fillCheckoutPaymentPage} from '@self/platform/spec/hermione/scenarios/checkout';
import promocodes from '@self/root/src/spec/hermione/configs/checkout/promocodes';
import OrderTotal from '@self/root/src/components/OrderTotalV2/__pageObject';
import Promocode from '@self/root/src/components/Promocode/__pageObject';
import SubmitField from '@self/root/src/components/SubmitField/__pageObject';
import CartCheckoutButton from '@self/root/src/widgets/content/cart/CartCheckoutControl/components/CartCheckoutButton/__pageObject';
// import CheckoutCartPage from '@self/project/src/widgets/content/checkout/common/CheckoutCartPage/components/View/__pageObject';
// import CheckoutPaymentPage
//     from '@self/project/src/widgets/content/checkout/common/CheckoutPaymentPage/components/View/__pageObject';
// import CheckoutSummaryPage
//     from '@self/project/src/widgets/content/checkout/common/CheckoutSummaryPage/components/View/__pageObject';
import CheckoutAuth
    from '@self/root/src/widgets/content/cart/CartCheckoutControl/components/CartCheckoutAuthRequirement/__pageObject';
import Modal from '@self/root/src/components/PopupBase/__pageObject';
import DeliveryTypeOptions from '@self/root/src/components/DeliveryTypeOptions/__pageObject/index.desktop.js';
// eslint-disable-next-line max-len
// import AddAddress from '@self/project/src/widgets/content/checkout/common/CheckoutUserAddresses/components/Presets/components/AddPresetButton/__pageObject';
// import UserAddressForm from
//     '@self/project/src/widgets/content/checkout/common/CheckoutUserAddresses/components/AddressForm/__pageObject';
import RecipientForm from '@self/root/src/components/RecipientForm/__pageObject';
import {Button} from '@self/root/src/uikit/components/Button/__pageObject';
// import CheckoutProcessNextStep
//     from '@self/project/src/widgets/content/checkout/common/CheckoutProcessNextStep/components/View/__pageObject';
import PaymentOptions from '@self/root/src/components/PaymentOptions/BasePaymentOptions/__pageObject';
import {
    offerMock,
    skuMock,
} from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {prepareCartPageBySkuId} from '@self/platform/spec/hermione/scenarios/cart';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';

export default makeSuite('Промокоды.', {
    feature: 'Промокоды',
    environment: 'kadavr',
    params: {
        cart: 'Корзина',
    },
    defaultParams: {
        cart: {
            items: [{
                skuMock,
                offerMock,
            }],
        },
    },
    story: {
        beforeEach() {
            this.setPageObjects({
                orderTotal: () => this.createPageObject(OrderTotal),
                promocodeWrapper: () => this.createPageObject(Promocode, {parent: this.orderInfo}),
                promocodeInput: () => this.createPageObject(SubmitField, {parent: this.promocodeWrapper}),

                checkoutCartButton: () => this.createPageObject(CartCheckoutButton, {parent: this.orderInfo}),
                // checkoutCartPage: () => this.createPageObject(CheckoutCartPage),
                // checkoutPaymentPage: () => this.createPageObject(CheckoutPaymentPage),
                // checkoutSummaryPage: () => this.createPageObject(CheckoutSummaryPage),
                checkoutAuth: () => this.createPageObject(CheckoutAuth),
                checkoutPromocodeInput: () => this.createPageObject(SubmitField, {
                    parent: this.checkoutSummaryPage,
                }),
                checkoutOrderTotal: () => this.createPageObject(OrderTotal),
                modal: () => this.createPageObject(Modal),
                deliveryTypeOptions: () => this.createPageObject(DeliveryTypeOptions, {
                    parent: this.checkoutCartPage,
                }),
                recipientForm: () => this.createPageObject(RecipientForm, {
                    parent: this.checkoutCartPage,
                }),
                modalRecipientForm: () => this.createPageObject(RecipientForm, {
                    parent: this.modal,
                }),
                // addressForm: () => this.createPageObject(UserAddressForm, {
                //     parent: this.checkoutCartPage,
                // }),
                // modalAddressForm: () => this.createPageObject(
                //     UserAddressForm,
                //     {parent: this.modal}
                // ),
                // addAddress: () => this.createPageObject(AddAddress, {
                //     parent: this.checkoutCartPage,
                // }),
                paymentOptions: () => this.createPageObject(PaymentOptions, {
                    parent: this.checkoutPaymentPage,
                }),
                presetSaveButton: () => this.createPageObject(Button, {
                    parent: this.modal,
                }),
                // nextButtonCartPage: () => this.createPageObject(CheckoutProcessNextStep, {
                //     parent: this.checkoutCartPage,
                // }),
                // nextButtonPaymentPage: () => this.createPageObject(CheckoutProcessNextStep, {
                //     parent: this.checkoutPaymentPage,
                // }),
            });
        },
        'Применение промокодов.': mergeSuites(
            makeSuite('Активный промокод', {
                params: {
                    promocode: 'Активный промокод.',
                    discount: 'Скидка по промокоду',
                },
                defaultParams: {
                    promocode: find(promocode => promocode.status === 'ACTIVE', promocodes).code,
                    discount: 10,
                },
                story: {
                    async beforeEach() {
                        const testState = await this.browser.yaScenario(
                            this,
                            prepareMultiCartState,
                            [buildCheckouterBucket(this.params.cart)]
                        );
                        await this.browser.yaScenario(
                            this,
                            prepareCartPageBySkuId,
                            {
                                items: testState.checkoutItems,
                                reportSkus: testState.reportSkus,
                                region: this.params.region,
                            }
                        );

                        await this.browser.setState('Loyalty.collections.promocodes', [
                            {
                                code: this.params.promocode,
                                discount: this.params.discount,
                            },
                        ]);

                        await this.browser.yaScenario(this, removePromocode);

                        return this.browser.yaScenario(this, applyPromocode, this.params.promocode, {withPause: true});
                    },

                    afterEach() {
                        return this.browser.yaScenario(this, removePromocode);
                    },

                    'При применении промокода отображаются корректные данные': {
                        'в чекауте': makeCase({
                            id: 'bluemarket-3270',
                            issue: 'BLUEMARKET-9977',

                            async test() {
                                if (this.params.isAuth) {
                                    await this.browser.yaWaitForChangeUrl(
                                        () => this.checkoutCartButton.goToCheckout(),
                                        1000
                                    );
                                } else {
                                    await this.checkoutCartButton.goToCheckout();

                                    await this.browser.yaWaitForChangeUrl(
                                        () => this.checkoutAuth.dismissLogin(),
                                        5000
                                    );
                                }

                                await this.checkoutCartPage.waitForVisible();

                                if (this.params.isAuth) {
                                    await this.browser.yaScenario(this, fillCheckoutCartPage, {
                                        deliveryType: 'DELIVERY',
                                        formName: 'user-prepaid',
                                        addNewAddress: true,
                                    });
                                } else {
                                    await this.browser.yaScenario(this, fillCheckoutCartPage, {
                                        deliveryType: 'DELIVERY',
                                        formName: 'user-prepaid',
                                    });
                                }

                                await this.browser.yaScenario(this, fillCheckoutPaymentPage, {
                                    paymentOption: 'CASH_ON_DELIVERY',
                                });

                                await checkPromocode({
                                    discount: this.params.discount,
                                    promocodeInput: this.checkoutPromocodeInput,
                                    orderTotal: this.checkoutOrderTotal,
                                });
                            },
                        }),
                    },
                },
            })
        ),
    },
});

async function checkPromocode({
    discount,
    promocodeInput,
    orderTotal,
}) {
    await promocodeInput.getMessage()
        .should.eventually.be.equal(
            `Теперь ваш заказ стоит на ${discount} ₽ дешевле`,
            `Под текстовым полем промокода отображается надпись "Теперь ваш заказ стоит на ${discount} ₽ дешевле"`
        );

    let value = await orderTotal.getItemsValue();
    const deliveryIsActive = await orderTotal.deliveryValue.isExisting();
    if (deliveryIsActive) {
        value += await orderTotal.getDeliveryValue();
    }
    return orderTotal.getPriceValue()
        .should.eventually.be.equal(
            value - discount,
            'Итоговая сумма рассчитана с учетом скидки'
        );
}
