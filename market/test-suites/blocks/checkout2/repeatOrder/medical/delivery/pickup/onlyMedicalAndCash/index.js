import {
    makeCase,
    makeSuite,
    mergeSuites,
} from 'ginny';

import {prepareCheckoutPage} from '@self/root/src/spec/hermione/scenarios/checkout';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {
    goToConfirmationPageAfterMedicalPickupDelivery,
} from '@self/root/src/spec/hermione/scenarios/checkout/touch/goToConfirmationPageAfterMedical';

import * as pharma from '@self/root/src/spec/hermione/kadavr-mock/report/pharma';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {
    deliveryDeliveryMock,
    deliveryPickupMock,
    paymentOptions,
} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';
import x5outletMock from '@self/root/src/spec/hermione/kadavr-mock/report/outlets/x5outlet';
import withTrying from '@self/root/src/spec/hermione/kadavr-mock/report/outlets/withTrying';
import CONTACTS from '@self/root/src/spec/hermione/test-suites/blocks/checkout/constants/contacts';

import AddressForm from '@self/root/src/components/AddressForm/__pageObject';
import GeoSuggest from '@self/root/src/components/GeoSuggest/__pageObject';
import RecipientForm from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/Popup/components/RecipientForm/__pageObject';
import RecipientList from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/Popup/components/RecipientList/__pageObject';
import RecipientInfo from '@self/root/src/widgets/content/checkout/layout/components/recipientInfo/__pageObject';
import CheckoutCourierMap
    from '@self/root/src/widgets/content/checkout/common/CheckoutVectorPinMap/components/VectorMap/__pageObject';
import CourierSuggest
    from '@self/root/src/widgets/content/checkout/common/CheckoutDeliveryEditor/components/CourierSuggest/__pageObject';
import PlacemarkMap from '@self/root/src/components/PlacemarkMap/__pageObject';
import FullAddressForm from '@self/root/src/components/FullAddressForm/__pageObject';
import EditPaymentOption from '@self/root/src/components/EditPaymentOption/__pageObject';
import EditableCard from '@self/root/src/components/EditableCard/__pageObject/index.touch';
import Modal from '@self/root/src/components/PopupBase/__pageObject';
import PaymentOptionsList from '@self/root/src/components/PaymentOptionsList/__pageObject';

export default makeSuite('В корзине только фарма. Самовывоз. Оплата наличными.', {
    id: 'marketfront-5900',
    issue: 'MARKETFRONT-81908',
    feature: 'Покупка списком. Чекаут. Флоу повторного заказа',
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                const carts = [
                    buildCheckouterBucket({
                        cartIndex: 0,
                        items: [{
                            skuMock: pharma.skuMock,
                            offerMock: pharma.offerMock,
                            count: 1,
                        }],
                        shopId: pharma.offerMock.shopId,
                        isMedicalParcel: true,
                        deliveryOptions: [
                            {
                                ...deliveryPickupMock,
                                paymentOptions: [
                                    paymentOptions.yandex,
                                    paymentOptions.cashOnDelivery,
                                ],
                                outlets: [
                                    {id: x5outletMock.id, regionId: 0},
                                    {id: pharma.outletMock.id, regionId: 0},
                                    {id: withTrying.id, regionId: 0},
                                ],
                            },
                            deliveryDeliveryMock,
                        ],
                        outlets: [
                            x5outletMock,
                            pharma.outletMock,
                            withTrying,
                        ],
                    }),
                ];

                await this.browser.setState(`persAddress.contact.${CONTACTS.DEFAULT_CONTACT.id}`, CONTACTS.DEFAULT_CONTACT);

                const testState = await this.browser.yaScenario(
                    this,
                    prepareMultiCartState,
                    carts
                );

                const offers = [
                    {
                        entity: 'offer',
                        marketSku: '265149848',
                        wareId: 'zq3kcdGwrZdWHzFxtRMJWA',
                        stockStoreCount: 1,
                        count: 1,
                        price: {currency: 'RUR', value: '234'},
                    },
                ];

                const shopsWithOffers = [
                    {
                        entity: 'shop',
                        id: 10268608,
                        name: 'Яндекс.Маркет',
                        hasBooking: false,
                        offers,
                        offersTotalPrice: {currency: 'RUR', value: '234'},
                    },
                ];

                await this.browser.setState('report.data.search.shops', shopsWithOffers);

                await this.browser.yaScenario(
                    this,
                    prepareCheckoutPage,
                    {
                        items: testState.checkoutItems,
                        reportSkus: testState.reportSkus,
                        checkout2: true,
                        queryParams: {
                            purchaseList: 1,
                        },
                    }
                );

                this.setPageObjects({
                    addressForm: () => this.createPageObject(AddressForm, {
                        parent: this.medicalCartDeliveryEditorCheckoutWizard,
                    }),
                    geoSuggest: () => this.createPageObject(GeoSuggest, {
                        parent: this.medicalCartDeliveryEditorCheckoutWizard,
                    }),
                    courierSuggest: () => this.createPageObject(CourierSuggest),
                    courierSuggestInput: () => this.createPageObject(GeoSuggest, {
                        parent: this.medicalCartDeliveryEditorCheckoutWizard,
                    }),
                    recipientForm: () => this.createPageObject(RecipientForm),
                    recipientList: () => this.createPageObject(RecipientList),
                    recipientInfo: () => this.createPageObject(RecipientInfo),
                    map: () => this.createPageObject(CheckoutCourierMap, {
                        parent: this.medicalCartDeliveryEditorCheckoutWizard,
                    }),
                    medicalCartPlacemarkMap: () => this.createPageObject(PlacemarkMap, {
                        parent: this.medicalCartDeliveryEditorCheckoutWizard,
                    }),

                    fullAddressForm: () => this.createPageObject(FullAddressForm),
                    citySuggest: () => this.createPageObject(GeoSuggest, {
                        parent: this.fullAddressForm,
                    }),
                    streetSuggest: () => this.createPageObject(GeoSuggest, {
                        parent: FullAddressForm.street,
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
                    paymentOptionsPopupContent: () => this.createPageObject(PaymentOptionsList, {
                        parent: this.paymentOptionsModal,
                    }),
                });

                await this.browser.yaScenario(
                    this,
                    goToConfirmationPageAfterMedicalPickupDelivery
                );
            },
            'Выбрать способ оплаты "Наличными при получении"': makeCase({
                async test() {
                    await this.browser.allure.runStep(
                        'Блок "Самовывоз" с лекарственными товарами.',
                        async () => {
                            await this.addressBlocks.getAddressTitleByCardIndex(0)
                                .should.eventually.include(
                                    'Самовывоз из аптеки 23 февраля – 8 марта',
                                    'Текст заголовка должен содержать "Самовывоз".'
                                );
                        }
                    );

                    await this.browser.allure.runStep(
                        'Блок "Самовывоз" с лекарственными товарами - адрес аптеки и часы работы.',
                        async () => {
                            const outletInfo = ['Пункт самовывоза Retest Full 1\n'] +
                                ['Москва, Сходненская, д. 11, стр. 1\n'] +
                                ['Ежедневно\n'] +
                                ['10:00 – 22:00'];

                            await this.addressBlocks.getInfoTitleByCardIndex(0)
                                .should.eventually.include(
                                    outletInfo,
                                    `Текст в поле адрес должен быть "${outletInfo}".`
                                );
                        }
                    );

                    await this.browser.allure.runStep(
                        'Блок "Получатель".',
                        async () => {
                            const MOCK_CONTACT = 'Вася Пупкин\npupochek@yandex.ru, 89876543210';
                            await this.recipientBlock
                                .getContactText()
                                .should.eventually.to.be.equal(
                                    MOCK_CONTACT,
                                    'В блоке "Получатель" отображаются данные, которые были указаны в моках'
                                );
                        }
                    );

                    await this.browser.allure.runStep(
                        'В блоке "Способ оплаты" выбираем "Наличными при получении"',
                        async () => {
                            await this.paymentOptionsEditableCard.changeButtonClick();
                            await this.paymentOptionsModal.waitForVisible();
                            return this.browser.allure.runStep(
                                'Установить способ оплаты "Наличными при получении"',
                                async () => {
                                    await this.paymentOptionsPopupContent.setPaymentTypeCashOnDelivery();
                                    await this.paymentOptionsPopupContent.submitButtonClick();
                                    await this.paymentOptionsModal.waitForInvisible(3000);
                                }
                            );
                        }
                    );

                    await this.browser.allure.runStep(
                        'Ожидаем изменения урла на: "/my/orders/confirmation".',
                        async () => {
                            await this.browser.setState('Checkouter.options', {isCheckoutSuccessful: true});

                            await this.browser.yaWaitForChangeUrl(
                                async () => {
                                    await this.checkoutOrderButton.click();
                                },
                                5000
                            );

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
                        }
                    );
                },
            }),
        }
    ),
});
