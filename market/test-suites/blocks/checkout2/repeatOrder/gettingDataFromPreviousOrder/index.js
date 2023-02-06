import {makeCase, makeSuite} from 'ginny';

// scenarios
import {
    addPresetForRepeatOrder,
    prepareCheckouterPageWithCartsForRepeatOrder,
    ACTUALIZATION_TIMEOUT,
} from '@self/root/src/spec/hermione/scenarios/checkout';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';

// mocks
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import {
    skuMock as largeCargoTypeSkuMock,
    offerMock as largeCargoTypeOfferMock,
} from '@self/root/src/spec/hermione/kadavr-mock/report/largeCargoType';
import {region} from '@self/root/src/spec/hermione/configs/geo';

// pageObjects
import AddressCard from '@self/root/src/components/AddressCard/__pageObject';
import CartCheckoutButton
    from '@self/root/src/widgets/content/cart/CartCheckoutControl/components/CartCheckoutButton/__pageObject';
import EditPaymentOption from '@self/root/src/components/EditPaymentOption/__pageObject';
import OrderTotal from '@self/root/src/components/OrderTotalV2/__pageObject';

import {ADDRESSES, CONTACTS} from '../../constants';

const address = ADDRESSES.MOSCOW_ADDRESS;
const contact = CONTACTS.DEFAULT_CONTACT;
const fullContactInfo = `${contact.recipient}\n${contact.email}, ${contact.phone}`;
const cartsWithLargeCargoType = [
    buildCheckouterBucket({
        items: [{
            skuMock: kettle.skuMock,
            offerMock: kettle.offerMock,
            count: 1,
        }, {
            skuMock: largeCargoTypeSkuMock,
            offerMock: largeCargoTypeOfferMock,
            cargoTypes: largeCargoTypeOfferMock.cargoTypes,
            count: 1,
        }],
    }),
];

export default makeSuite('Оформление заказа ОТ в офис и КГТ.', {
    feature: 'Оформление заказа ОТ в офис и КГТ.',
    id: 'marketfront-4427',
    issue: 'MARKETFRONT-36076',
    defaultParams: {
        region: region['Москва'],
        isAuthWithPlugin: true,
    },
    environment: 'kadavr',
    story: {
        async beforeEach() {
            this.setPageObjects({
                addressCard: () => this.createPageObject(AddressCard, {
                    parent: this.deliveryInfo,
                }),
                editPaymentContent: () => this.createPageObject(EditPaymentOption, {
                    root: EditPaymentOption.content,
                    parent: this.confirmationPage,
                }),
                orderTotal: () => this.createPageObject(OrderTotal, {
                    parent: this.summary,
                }),
                cartCheckoutButton: () => this.createPageObject(CartCheckoutButton),
            });
            await this.browser.setState('Checkouter.options', {isCheckoutSuccessful: true});
            await this.browser.yaScenario(
                this,
                addPresetForRepeatOrder,
                {
                    address,
                    contact,
                }
            );
            await this.browser.yaScenario(
                this,
                prepareCheckouterPageWithCartsForRepeatOrder,
                {
                    carts: cartsWithLargeCargoType,
                    options: {
                        region: this.params.region,
                        checkout2: true,
                    },
                }
            );
        },
        'Открыть страницу чекаута.': makeCase({
            async test() {
                await this.browser.allure.runStep(
                    'Отображается главный экран чекаута.',
                    async () => {
                        await this.confirmationPage.waitForVisible();
                        await this.deliveryInfo.waitForVisible();
                    }
                );

                await this.allure.runStep(
                    'Блок информации о доставке.', async () => {
                        await this.allure.runStep(
                            'В блоке информации о доставки отображается заголовок "Доставка курьером".',
                            () =>
                                this.addressEditableCard.getTitle()
                                    .should.eventually.to.be.include(
                                        'Доставка курьером',
                                        'Текст заголовка блока доставки должен быть "Доставка курьером".'
                                    )
                        );
                        await this.allure.runStep(
                            `В блоке информации о доставки отображается адрес "${address.address}".`, () =>
                                this.addressCard.getText()
                                    .should.eventually.to.be.equal(
                                        address.address,
                                        `Текст в поле адрес должен быть "${address.address}".`
                                    )
                        );
                    }
                );

                await this.allure.runStep(
                    'Блок "Получатель".', async () => {
                        await this.allure.runStep(
                            'В блоке информации о получателе подставились данные из предыдущего заказа.',
                            () =>
                                this.recipientBlock.getContactText()
                                    .should.eventually.to.be.equal(
                                        fullContactInfo,
                                        'На карточке получателя должны быть данные из предыдущего заказа.'
                                    )
                        );
                    }
                );

                await this.allure.runStep(
                    'Блок "Способ оплаты".', async () => {
                        await this.allure.runStep(
                            'В блоке оплаты выбран способ оплаты "Картой онлайн".',
                            () =>
                                this.editPaymentContent.getText()
                                    .should.eventually.to.be.match(
                                        new RegExp('(Новой картой|••••\\s\\s\\d{4})$'),
                                        'Должен быть выбран способ оплаты картой'
                                    )
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'В саммари отображается информация о стоимости доставки.',
                    async () => {
                        await this.orderTotal.getTotalDeliveryPriceValue()
                            .should.eventually.be.equal(
                                250,
                                'В саммари должна отображается информация о стоимости доставки.'
                            );
                    }
                );

                await this.allure.runStep(
                    'Нажать на кнопку "Подтвердить заказ".', async () => {
                        await this.browser.allure.runStep(
                            'Кнопка "Подтвердить заказ" отображается активной.',
                            async () => {
                                await this.checkoutOrderButton.waitForEnabledButton();
                                await this.checkoutOrderButton.isButtonDisabled()
                                    .should.eventually.to.be.equal(
                                        false,
                                        'Кнопка "Подтвердить заказ" дожна быть активна'
                                    );
                            }
                        );

                        await this.browser.allure.runStep(
                            'Ожидаем изменения урла на: "/my/orders/payment".',
                            async () => {
                                await this.browser.yaWaitForChangeUrl(
                                    async () => {
                                        await this.checkoutOrderButton.click();
                                    },
                                    ACTUALIZATION_TIMEOUT
                                );
                                await this.browser.getUrl()
                                    .should.eventually.to.be.link({
                                        query: {
                                            orderId: /\d+/,
                                        },
                                        pathname: '/my/orders/payment',
                                    }, {
                                        mode: 'match',
                                        skipProtocol: true,
                                        skipHostname: true,
                                    });
                            }
                        );
                    }
                );
            },
        }),
    },
});
