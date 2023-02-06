import {
    makeSuite,
    makeCase,
} from 'ginny';

import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import {region} from '@self/root/src/spec/hermione/configs/geo';
import {
    ACTUALIZATION_TIMEOUT,
    prepareCheckouterPageWithCartsForRepeatOrder,
} from '@self/root/src/spec/hermione/scenarios/checkout';
import {deliveryDeliveryMock} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {fillAddressForm} from '@self/platform/spec/hermione/scenarios/checkout';

import DeliveryTypeOptions from '@self/root/src/components/DeliveryTypeOptions/__pageObject/index.touch.js';
import FullAddressForm from '@self/root/src/components/FullAddressForm/__pageObject';
import GeoSuggest from '@self/root/src/components/GeoSuggest/__pageObject';
import AddressCard from '@self/root/src/components/AddressCard/__pageObject';
import CheckoutRecipient
    from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/CheckoutRecipient/__pageObject';
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
import PaymentOptionsList
    from '@self/root/src/components/PaymentOptionsList/__pageObject';
import RecipientPopupContainer from
    '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/Popup/__pageObject/index.touch';
import CartCheckoutButton from '@self/root/src/widgets/content/cart/CartCheckoutControl/components/CartCheckoutButton/__pageObject';
import RecipientList from
    '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/Popup/components/RecipientList/__pageObject/index.touch.js';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {ADDRESSES, CONTACTS} from '../constants';

export default makeSuite('ХСЧ: сохранение данных получателя при оформлении обычного товара и возврате на страницу корзины.', {
    id: 'm-touch-3645',
    issue: 'MARKETFRONT-50733',
    feature: 'ХСЧ: сохранение данных получателя при оформлении обычного товара и возврате на страницу корзины.',
    params: {
        region: 'Регион',
        isAuthWithPlugin: 'Авторизован ли пользователь',
    },
    defaultParams: {
        region: region['Москва'],
        isAuthWithPlugin: true,
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                deliveryTypeOptions: () => this.createPageObject(DeliveryTypeOptions),
                recipientPopupContainer: () => this.createPageObject(RecipientPopupContainer),
                recipientList: () => this.createPageObject(RecipientList),
                fullAddressForm: () => this.createPageObject(FullAddressForm),
                citySuggest: () => this.createPageObject(GeoSuggest, {
                    parent: this.fullAddressForm,
                }),
                streetSuggest: () => this.createPageObject(GeoSuggest, {
                    parent: FullAddressForm.street,
                }),
                addressCard: () => this.createPageObject(AddressCard, {
                    parent: this.addressBlock,
                }),
                contactCard: () => this.createPageObject(CheckoutRecipient, {
                    parent: this.confirmationPage,
                }),
                dateSelect: () => this.createPageObject(DateSelect, {
                    parent: this.addressBlock,
                }),
                timeSelect: () => this.createPageObject(TimeSelect, {
                    parent: this.addressBlock,
                }),
                selectPopover: () => this.createPageObject(SelectPopover),
                paymentOptionsPopupContent: () => this.createPageObject(PaymentOptionsList, {
                    parent: this.paymentOptionsModal,
                }),
                cartCheckoutButton: () => this.createPageObject(CartCheckoutButton),
            });
            const carts = [
                buildCheckouterBucket({
                    cartIndex: 0,
                    items: [{
                        skuMock: kettle.skuMock,
                        offerMock: kettle.offerMock,
                        count: 1,
                    }],
                    deliveryOptions: [
                        deliveryDeliveryMock,
                        {
                            ...deliveryDeliveryMock,
                            id: 'mockedId1',
                            hash: 'mockedHash1',
                            dates: {
                                fromDate: '05-03-2024',
                                toDate: '06-03-2024',
                            },
                            deliveryOptionId: 1003938,
                        },
                    ],
                }),
            ];

            await this.browser.yaScenario(
                this,
                prepareCheckouterPageWithCartsForRepeatOrder,
                {
                    carts,
                    options: {
                        region: this.params.region,
                        checkout2: true,
                    },
                }
            );
        },
        'Сохранение данных получателя при оформлении обычного товара.': makeCase({
            async test() {
                await this.allure.runStep(
                    'Открыть страницу чекаута', async () => {
                        await this.confirmationPage.waitForVisible();
                    }
                );

                await this.allure.runStep(
                    'В блоке адреса доставки нажать кнопку "Изменить способ доставки".', async () => {
                        await this.allure.runStep(
                            'Открыть попап "Новый адрес".', async () => {
                                await this.addressBlock.changeButtonClick();
                                await this.editPopup.waitForVisibleRoot(5000);
                            }
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Нажать кнопку "Добавить новый".',
                    async () => {
                        await this.browser.allure.runStep(
                            'Над чипсами присутствует кнопка "Добавить новый".',
                            async () => {
                                await this.editPopup.isAddNewButtonVisible()
                                    .should.eventually.to.be.equal(
                                        true,
                                        'Над чипсами должна быть кнопка "Добавить новый".'
                                    );
                            }
                        );

                        await this.browser.allure.runStep(
                            'Нажать кнопку "Добавить новый".',
                            async () => {
                                await this.editPopup.clickAddNewButton();
                            }
                        );
                    }
                );

                await this.allure.runStep(
                    'В форме заполнить поля нового адреса.', async () => {
                        await this.allure.runStep(
                            'Заполняем форму нового адреса.', () =>
                                this.browser.yaScenario(
                                    this,
                                    fillAddressForm,
                                    ADDRESSES.MOSCOW_HSCH_ADDRESS
                                )
                        );
                        await this.allure.runStep(
                            'Нажать кнопку "Сохранить".', async () => {
                                await this.editPopup.waitForChooseButtonEnabled();
                                await this.editPopup.chooseButtonClick();
                            }
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Нажать на кнопку "Привезти сюда".',
                    async () => {
                        await this.editPopup.waitForChooseButtonEnabled();
                        await this.editPopup.chooseButtonClick();
                    }
                );

                await this.allure.runStep(
                    'Открыть страницу чекаута', async () => {
                        await this.confirmationPage.waitForVisible();
                        await this.preloader.waitForHidden(5000);
                    }
                );

                await this.allure.runStep(
                    'В поле "Адрес доставки" указан полный адрес доставки, который был указан в форме "Новый адрес".', async () => {
                        await this.addressEditableCard.getTitle()
                            .should.eventually.include(
                                'Доставка курьером',
                                'Текст заголовка должен содержать "Доставка курьером".'
                            );

                        await this.addressCard.getText()
                            .should.eventually.to.be.equal(
                                ADDRESSES.MOSCOW_HSCH_ADDRESS.fullDeliveryInfo,
                                'На карточке адреса доставки должны отображаться указанные пользователем данные адреса'
                            );
                    }
                );

                await this.allure.runStep(
                    'Открываем список с датами доставки', async () => {
                        await this.dateSelect.click();
                        await this.selectPopover.waitForListIsVisible();

                        await this.allure.runStep(
                            'Выбираем последнюю дату доставки', async () => {
                                await this.selectPopover.clickOptionByText('с 5 по 6 марта, 250 ₽');
                            }
                        );

                        await this.allure.runStep(
                            'На экране отображается выбранная дата доставки', async () => {
                                await this.dateSelect.getText()
                                    .should.eventually.to.be.include(
                                        'с 5 по 6 марта',
                                        'должна отображаться выбранная дата доставки'
                                    );
                            }
                        );
                    }
                );

                await this.allure.runStep(
                    'Открываем список с интервалами доставки', async () => {
                        await this.allure.runStep(
                            'Выбираем последний интервал времени доставки', async () => {
                                await this.timeSelect.clickByText('16:00-20:00');
                            }
                        );

                        await this.allure.runStep(
                            'На экране отображается выбранное время доставки', async () => {
                                await this.timeSelect.getText()
                                    .should.eventually.to.be.equal(
                                        '16:00-20:00',
                                        'должна отображаться выбранная дата доставки'
                                    );
                            }
                        );
                    }
                );

                await this.allure.runStep(
                    'В блоке "Получатель".', async () => {
                        await this.allure.runStep(
                            'Нажать кнопку "Выбрать получателя".', async () => {
                                await this.recipientBlock.chooseRecipientButtonClick();
                            }
                        );

                        await this.allure.runStep(
                            'Заполняем поля формы получателя.', async () => {
                                await this.recipientForm.setRecipientData(CONTACTS.HSCH_CONTACT, 0);
                            }
                        );

                        await this.recipientPopupContainer.submitButtonClick();

                        await this.allure.runStep(
                            'Выбираем пользователя.', async () => {
                                await this.recipientList.waitForVisible();
                                await this.recipientList.clickRecipientListItemByRecipient(CONTACTS.HSCH_CONTACT.recipientFullInfo);
                            }
                        );

                        await this.recipientPopupContainer.chooseButtonClick();

                        await this.confirmationPage.waitForVisible();
                        if (await this.preloader.waitForVisible(1000)) {
                            await this.preloader.waitForHidden(ACTUALIZATION_TIMEOUT);
                        }

                        await this.contactCard.getContactText()
                            .should.eventually.to.be.equal(
                                CONTACTS.HSCH_CONTACT.recipientFullInfo,
                                'На карточке получателя должны отображаться данные указанные пользователем'
                            );
                    }
                );

                await this.allure.runStep(
                    'В блоке способа оплаты.', async () => {
                        await this.allure.runStep(
                            'Нажать кнопку изменения способа оплаты.', async () => {
                                await this.paymentOptionsEditableCard.changeButtonClick();
                            }
                        );

                        await this.allure.runStep(
                            'Выбрать способ оплаты "Наличными при получении".', async () => {
                                await this.paymentOptionsModal.waitForVisible();
                                await this.paymentOptionsPopupContent.setPaymentTypeCashOnDelivery();
                            }
                        );

                        await this.allure.runStep(
                            'Нажать кнопку подтверждения.', async () => {
                                await this.paymentOptionsPopupContent.submitButtonClick();
                                await this.paymentOptionsModal.waitForInvisible(3000);
                            }
                        );

                        await this.browser.allure.runStep(
                            'Ожидаем когда способ оплаты сменится на "Наличными при получении".',
                            () => this.browser.waitUntil(
                                () => this.paymentOptionsBlock.getText()
                                    .should.eventually.include(
                                        'Наличными при получении',
                                        'На карточке способа оплаты должен отображатся способ оплаты "Наличными при получении".'
                                    ),
                                3000,
                                'Текст способа оплаты должен стать "Наличными при получении".'
                            )
                        );
                    }
                );

                if (await this.preloader.waitForVisible(2000)) {
                    await this.preloader.waitForHidden(ACTUALIZATION_TIMEOUT);
                }

                await this.allure.runStep(
                    'Вернуться на страницу корзины.', () =>
                        this.browser.yaOpenPage(PAGE_IDS_COMMON.CART)
                );

                await this.allure.runStep(
                    'На странице корзины нажать на кнопку "Перейти к оформлению"', async () => {
                        await this.cartCheckoutButton.waitForButtonEnabled();
                        await this.cartCheckoutButton.goToCheckout();
                    }
                );

                await this.allure.runStep(
                    'После перезагрузки отображаются измененные данные', async () => {
                        await this.confirmationPage.waitForVisible();
                        if (await this.preloader.waitForVisible(1000)) {
                            await this.preloader.waitForHidden(ACTUALIZATION_TIMEOUT);
                        }

                        await this.addressEditableCard.getTitle()
                            .should.eventually.include(
                                'Доставка курьером',
                                'Текст заголовка должен содержать "Доставка курьером".'
                            );

                        await this.addressCard.getText()
                            .should.eventually.to.be.equal(
                                ADDRESSES.MOSCOW_HSCH_ADDRESS.fullDeliveryInfo,
                                'На карточке адреса доставки должны отображаться указанные пользователем данные адреса'
                            );

                        await this.dateSelect.getText()
                            .should.eventually.to.be.include(
                                'с 5 по 6 марта',
                                'должна отображаться выбранная дата доставки'
                            );

                        await this.timeSelect.getText()
                            .should.eventually.to.be.equal(
                                '16:00-20:00',
                                'должна отображаться выбранная дата доставки'
                            );

                        await this.contactCard.getContactText()
                            .should.eventually.to.be.equal(
                                CONTACTS.HSCH_CONTACT.recipientFullInfo,
                                'На карточке получателя должны отображаться данные указанные пользователем'
                            );

                        await this.paymentOptionsBlock.getText()
                            .should.eventually.include(
                                'Наличными при получении',
                                'На карточке способа оплаты должен отображатся способ оплаты "Наличными при получении".'
                            );
                    }
                );
            },
        }),
    },
});
