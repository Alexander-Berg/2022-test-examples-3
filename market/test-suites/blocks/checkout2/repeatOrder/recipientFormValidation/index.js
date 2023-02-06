import {
    makeSuite,
    prepareSuite,
    mergeSuites,
} from 'ginny';

// scenarios
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {
    addPresetForRepeatOrder,
    prepareCheckouterPageWithCartsForRepeatOrder,
} from '@self/root/src/spec/hermione/scenarios/checkout';

// pageObjects
import DeliveryInfo from '@self/root/src/components/Checkout/DeliveryInfo/__pageObject/index.touch';
import RecipientPopupContainer
    from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/Popup/__pageObject/index.touch';
import RecipientList
    from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/Popup/components/RecipientList/__pageObject/index.touch';
import RecipientFormFields from '@self/root/src/components/RecipientForm/__pageObject';
import RecipientPopup
    from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/Popup/__pageObject/index.touch.js';

// mocks
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';

import fieldValidator from './fieldValidator';

import {ADDRESSES, CONTACTS} from '../constants';

const nameTextFieldSelector = `${RecipientFormFields.name}`;
const emailTextFieldSelector = `${RecipientFormFields.email}`;
const phoneTextFieldSelector = `${RecipientFormFields.phone}`;

const address = ADDRESSES.MOSCOW_ADDRESS;
const contact = CONTACTS.DEFAULT_CONTACT;
const recipientFullInfo = `${contact.recipient}\n${contact.email}, ${contact.phone}`;
const newRecipient = 'Тест Кейс';
const newEmail = 'test@testik.ru';
const newPhone = '89876543210';
const editedWithPhone = `${contact.recipient}\n${contact.email}, ${newPhone}`;
const editedWithEmail = `${contact.recipient}\n${newEmail}, ${contact.phone}`;
const editedWithRecipient = `${newRecipient}\n${contact.email}, ${contact.phone}`;

const simpleCarts = [
    buildCheckouterBucket({
        items: [{
            skuMock: kettle.skuMock,
            offerMock: kettle.offerMock,
            count: 1,
        }],
    }),
];

export default makeSuite('Редактирование получателя в попапе "Получатель".', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-57070',
    story: mergeSuites({
        async beforeEach() {
            this.setPageObjects({
                deliveryInfo: () => this.createPageObject(DeliveryInfo, {
                    parent: this.confirmationPage,
                }),
                recipientPopupContainer: () => this.createPageObject(RecipientPopupContainer),
                recipientPopup: () => this.createPageObject(RecipientPopup),
                recipientList: () => this.createPageObject(RecipientList),
                recipientFormFields: () => this.createPageObject(RecipientFormFields),
            });

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
                    carts: simpleCarts,
                    options: {
                        region: this.params.region,
                        checkout2: true,
                    },
                }
            );
        },
        'Поле "Имя и фамилия".': prepareSuite(fieldValidator, {
            meta: {
                id: 'm-touch-3736',
            },
            params: {
                selector: nameTextFieldSelector,
                fieldName: 'Имя и фамилия',
                recipientBeforeEdit: recipientFullInfo,
                editedRecipient: editedWithRecipient,
                validText: {
                    text: newRecipient,
                },
                additionalText: {
                    text: `${contact.recipient}test`,
                    expectedError: 'Фамилия и имя должны быть написаны на кириллице',
                },
                emptyText: {
                    expectedError: 'Напишите имя и фамилию как в паспорте',
                },
                invalidText: {
                    text: 'Имя',
                    expectedError: 'Напишите имя и фамилию как в паспорте',
                },
            },
        }),
        'Поле "Электронная почта".': prepareSuite(fieldValidator, {
            meta: {
                id: 'm-touch-3738',
            },
            params: {
                selector: emailTextFieldSelector,
                fieldName: 'Электронная почта',
                recipientBeforeEdit: recipientFullInfo,
                editedRecipient: editedWithEmail,
                validText: {
                    text: newEmail,
                },
                additionalText: {
                    text: `тест${contact.email}`,
                    expectedError: 'Неверный формат почты',
                },
                emptyText: {
                    expectedError: 'Напишите электронную почту',
                },
                invalidText: {
                    text: 'test.ru',
                    expectedError: 'Неверный формат почты',
                },
            },
        }),
        'Поле "Телефон".': prepareSuite(fieldValidator, {
            meta: {
                id: 'm-touch-3743',
            },
            params: {
                selector: phoneTextFieldSelector,
                fieldName: 'Телефон',
                recipientBeforeEdit: recipientFullInfo,
                editedRecipient: editedWithPhone,
                validText: {
                    text: newPhone,
                },
                additionalText: {
                    text: `${contact.phone}9879879`,
                    expectedError: 'Пример: +7 495 414-30-00',
                },
                emptyText: {
                    expectedError: 'Напишите номер телефона',
                },
            },
        }),
    }),
});
