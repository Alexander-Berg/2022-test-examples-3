import {makeCase, makeSuite} from 'ginny';

import DeliveryTypeOptions from '@self/root/src/components/DeliveryTypeOptions/__pageObject/index.touch.js';
import FullAddressForm from '@self/root/src/components/FullAddressForm/__pageObject';
import GeoSuggest from '@self/root/src/components/GeoSuggest/__pageObject';
import RecipientForm from '@self/root/src/components/RecipientForm/__pageObject';
import CheckoutLayoutConfirmation from
    '@self/root/src/widgets/content/checkout/layout/CheckoutLayoutConfirmationPage/view/__pageObject';
import GroupedParcel
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/GroupedParcel/__pageObject';
import EditableCard from '@self/root/src/components/EditableCard/__pageObject/index.touch.js';
import AddressCard from '@self/root/src/components/AddressCard/__pageObject';
import ContactCard from '@self/root/src/components/Checkout/ContactCard/__pageObject';
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
import {SelectButton, SelectPopover} from '@self/root/src/components/Select/__pageObject';
import EditPaymentOption
    from '@self/root/src/components/EditPaymentOption/__pageObject';
import Modal from '@self/root/src/components/PopupBase/__pageObject';
import PaymentOptionsList
    from '@self/root/src/components/PaymentOptionsList/__pageObject';

import {fillDeliveryType, fillAddressForm} from '@self/platform/spec/hermione/scenarios/checkout';


const RECIPIENT_DATA = {
    name: 'Тест Тестовый',
    email: 'test@test.test',
    phone: '79999992919',
};

const RECIPIENT_DATA_INLINE = `${RECIPIENT_DATA.name}\n${RECIPIENT_DATA.email}, ${RECIPIENT_DATA.phone}`;

const ADDRESS_DATA = {
    city: 'Москва',
    street: 'Усачёва улица',
    house: '62',
    apartment: '12',
    floor: '15',
    entrance: '1',
    intercom: '12test',
    comment: 'Тестирование',
};

const {
    city,
    street,
    house,
    apartment,
    floor,
    entrance,
    intercom,
    comment,
} = ADDRESS_DATA;

const ADDRESS_DATA_INLINE =
    `${city}, ${street}, д. ${house}, ${apartment}\n${entrance} подъезд,` +
    ` ${floor} этаж, домофон ${intercom}, "${comment}"`;

export default makeSuite('', {
    environment: 'kadavr',
    params: {
        isAuthWithPlugin: 'Авторизован ли пользователь',
    },
    defaultParams: {
        isAuthWithPlugin: false,
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                deliveryTypeOptions: () => this.createPageObject(DeliveryTypeOptions),
                fullAddressForm: () => this.createPageObject(FullAddressForm),
                citySuggest: () => this.createPageObject(GeoSuggest, {
                    parent: this.fullAddressForm,
                }),
                streetSuggest: () => this.createPageObject(GeoSuggest, {
                    parent: FullAddressForm.street,
                }),
                recipientForm: () => this.createPageObject(RecipientForm),
                confirmationPage: () => this.createPageObject(CheckoutLayoutConfirmation),
                addressBlock: () => this.createPageObject(GroupedParcel, {
                    parent: this.confirmationPage,
                }),
                addressEditableCard: () => this.createPageObject(EditableCard, {
                    parent: this.addressBlock,
                }),
                addressCard: () => this.createPageObject(AddressCard, {
                    parent: this.addressBlock,
                }),
                contactCard: () => this.createPageObject(ContactCard, {
                    parent: this.confirmationPage,
                }),
                dateSelect: () => this.createPageObject(DateSelect, {
                    parent: this.addressBlock,
                }),
                timeSelect: () => this.createPageObject(TimeSelect, {
                    parent: this.addressBlock,
                }),
                dateSelectButton: () => this.createPageObject(SelectButton, {
                    parent: this.dateSelect,
                }),
                selectPopover: () => this.createPageObject(SelectPopover),
                paymentOptionsBlock: () => this.createPageObject(EditPaymentOption, {
                    parent: this.confirmationPage,
                }),
                paymentOptionsEditableCard: () => this.createPageObject(EditableCard, {
                    parent: this.paymentOptionsBlock,
                }),
                paymentOptionsModal: () => this.createPageObject(Modal, {
                    root: `${Modal.root} [data-auto="editableCardPopup"]`,
                }),
                paymentOptionsPopupContent: () => this.createPageObject(PaymentOptionsList),
            });
        },
        'Заполняем адрес и данные получателя': {
            async beforeEach() {
                await this.browser.yaScenario(
                    this,
                    fillDeliveryType,
                    {type: 'DELIVERY'}
                );

                await this.deliveryEditor.submitButtonClick();

                await this.browser.yaScenario(
                    this,
                    fillAddressForm,
                    ADDRESS_DATA
                );

                await this.deliveryEditor.submitButtonClick();

                await this.recipientForm.setRecipientData(RECIPIENT_DATA, 0);

                await this.deliveryEditor.submitButtonClick();
            },
            'Отображается главный экран чекаута': makeCase({
                async test() {
                    await this.confirmationPage.waitForVisible();
                },
            }),
            'В поле "Адрес доставки" указан полный адрес доставки, который был указан в форме "Адрес доставки"': makeCase({
                async test() {
                    await this.addressEditableCard.getTitle()
                        .should.eventually.include(
                            'Доставка курьером',
                            'Текст заголовка должен содержать "Доставка курьером".'
                        );

                    await this.addressCard.getText()
                        .should.eventually.to.be.equal(
                            ADDRESS_DATA_INLINE,
                            'На карточке адреса доставки должны отображаться указанные пользователем данные адреса'
                        );
                },
            }),
            'В блоке "Получатель" отображаются данные получателя, которые были указаны в форме "Получатель"': makeCase({
                async test() {
                    await this.contactCard.getContactText()
                        .should.eventually.to.be.equal(
                            RECIPIENT_DATA_INLINE,
                            'На карточке получателя должны отображаться данные указанные пользователем'
                        );
                },
            }),
            'Открываем список с датами доставки': {
                async beforeEach() {
                    await this.dateSelectButton.click();
                },
                'Отображается список доступных дат доставки': makeCase({
                    async test() {
                        await this.selectPopover.waitForListIsVisible();
                    },
                }),
                'Выбираем последнюю дату доставки': {
                    async beforeEach() {
                        await this.selectPopover.clickOptionByText('с 5 по 6 марта, 250 ₽');
                        await this.selectPopover.waitForListIsInvisible();
                    },
                    'На экране отображается выбранная дата доставки': {
                        'в селекторе': makeCase({
                            async test() {
                                await this.dateSelectButton.getText()
                                    .should.eventually.to.be.include(
                                        'с 5 по 6 марта',
                                        'должна отображаться выбранная дата доставки'
                                    );
                            },
                        }),
                    },
                },
            },
            'Открываем список с интервалами доставки': {
                'Выбираем последний интервал времени доставки': {
                    async beforeEach() {
                        await this.timeSelect.clickByText('16:00-20:00');
                    },
                    'На экране отображается выбранное время доставки': {
                        'в селекторе': makeCase({
                            async test() {
                                await this.timeSelect.getText()
                                    .should.eventually.to.be.equal(
                                        '16:00-20:00',
                                        'должна отображаться выбранная дата доставки'
                                    );
                            },
                        }),
                    },
                },
            },
            'Открываем список с способами оплаты': {
                async beforeEach() {
                    await this.paymentOptionsEditableCard.changeButtonClick();
                    await this.paymentOptionsModal.waitForVisible();
                },
                'Выбираем способ оплаты "Наличными при получении"': {
                    async beforeEach() {
                        await this.paymentOptionsPopupContent.setPaymentTypeCashOnDelivery();
                        await this.paymentOptionsPopupContent.submitButtonClick();
                    },
                    'Список закрывается': makeCase({
                        async test() {
                            await this.paymentOptionsModal.waitForNonexisting();
                        },
                    }),
                    'В блоке "Способ оплаты" отображается "Наличными при получении"': makeCase({
                        async test() {
                            await this.paymentOptionsBlock.getText()
                                .should.eventually.include(
                                    'Наличными при получении',
                                    'На карточке способа оплаты должен отображатся способ оплаты "Наличными при получении".'
                                );
                        },
                    }),
                },
            },
            'Выбираем способ оплаты, дату и интервал доставки, и перезагружаем страницу': {
                async beforeEach() {
                    await this.dateSelectButton.click();
                    await this.selectPopover.waitForListIsVisible();
                    await this.selectPopover.clickOptionByText('с 5 по 6 марта, 250 ₽');

                    await this.timeSelect.clickByText('16:00-20:00');

                    await this.paymentOptionsEditableCard.changeButtonClick();
                    await this.paymentOptionsModal.waitForVisible();
                    await this.paymentOptionsPopupContent.setPaymentTypeCashOnDelivery();
                    await this.paymentOptionsPopupContent.submitButtonClick();
                    await this.paymentOptionsModal.waitForNonexisting();

                    await this.browser.refresh();
                },
                'После перезагрузки отображаются измененные данные': makeCase({
                    async test() {
                        await this.confirmationPage.waitForVisible();

                        await this.addressEditableCard.getTitle()
                            .should.eventually.include(
                                'Доставка курьером',
                                'Текст заголовка должен содержать "Доставка курьером".'
                            );

                        await this.addressCard.getText()
                            .should.eventually.to.be.equal(
                                ADDRESS_DATA_INLINE,
                                'На карточке адреса доставки должены быть указанные пользователем данные.'
                            );

                        await this.dateSelectButton.getText()
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
                                RECIPIENT_DATA_INLINE,
                                'На карточке получателя должны быть указанные пользователем данные'
                            );

                        await this.paymentOptionsBlock.getText()
                            .should.eventually.include(
                                'Наличными при получении',
                                'На карточке способа оплаты должен отображатся способ оплаты "Наличными при получении".'
                            );
                    },
                }),
            },
        },
    },
});
