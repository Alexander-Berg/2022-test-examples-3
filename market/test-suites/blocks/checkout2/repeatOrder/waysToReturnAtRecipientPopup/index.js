import {makeSuite} from 'ginny';

// scenarios
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {
    addPresetForRepeatOrder,
    prepareCheckouterPageWithCartsForRepeatOrder,
} from '@self/root/src/spec/hermione/scenarios/checkout';

// pageObjects
import EditPopup
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/__pageObject';
import RecipientFormFields from '@self/root/src/components/RecipientForm/__pageObject';
import PopupBase from '@self/root/src/components/PopupBase/__pageObject';
/* eslint-disable max-len */
import DeleteForm from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/Popup/components/DeleteForm/__pageObject/index.js';
import RecipientForm from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/Popup/components/RecipientForm/__pageObject';
import RecipientList from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/Popup/components/RecipientList/__pageObject';
/* eslint-enable max-len */

// mocks
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import {region} from '@self/root/src/spec/hermione/configs/geo';

// cases
import closeRecipientPopup from './closeRecipientPopup';
import returnToRecipientPopup from './returnToRecipientPopup';
import returnToRecipientFromEditPopup from './returnToRecipientFromEditPopup';
import returnToEditRecipientFromDeletingPopup from './returnToEditRecipientFromDeletingPopup';

import {ADDRESSES, CONTACTS} from '../../constants';

const recipient =
    `${CONTACTS.DEFAULT_CONTACT.recipient}\n${CONTACTS.DEFAULT_CONTACT.email}, ${CONTACTS.DEFAULT_CONTACT.phone}`;
const simpleCarts = [
    buildCheckouterBucket({
        items: [{
            skuMock: kettle.skuMock,
            offerMock: kettle.offerMock,
            count: 1,
        }],
    }),
];

export default makeSuite('Навигации в попапах получателя.', {
    issue: 'MARKETFRONT-54581',
    feature: 'Навигации в попапах получателя.',
    params: {
        region: 'Регион',
        isAuthWithPlugin: 'Авторизован ли пользователь',
    },
    defaultParams: {
        region: region['Москва'],
        isAuthWithPlugin: true,
    },
    environment: 'kadavr',
    story: {
        async beforeEach() {
            this.setPageObjects({
                popupBase: () => this.createPageObject(PopupBase, {
                    root: `${PopupBase.root} [data-auto="editableCardPopup"]`,
                }),
                editPopup: () => this.createPageObject(EditPopup),
                recipientList: () => this.createPageObject(RecipientList),
                recepientForm: () => this.createPageObject(RecipientForm),
                deleteForm: () => this.createPageObject(DeleteForm),
                recipientFormFields: () => this.createPageObject(RecipientFormFields, {
                    parent: this.recipientForm,
                }),
            });

            await this.browser.yaScenario(
                this,
                addPresetForRepeatOrder,
                {
                    address: ADDRESSES.MOSCOW_ADDRESS,
                    contact: CONTACTS.DEFAULT_CONTACT,
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
        'Открыть главную страницу чекаута.': {
            async beforeEach() {
                await this.browser.allure.runStep(
                    'Отображается главный экран чекаута.',
                    async () => {
                        await this.confirmationPage.waitForVisible();
                        await this.deliveryInfo.waitForVisible();

                        await this.browser.allure.runStep(
                            'В блоке "Получатель" отображается информация о получателе.',
                            async () => {
                                await this.recipientBlock.getContactText()
                                    .should.eventually.to.be.equal(
                                        recipient,
                                        'На карточке получателя должны быть данные'
                                    );
                            }
                        );

                        await this.browser.allure.runStep(
                            'Нажать кнопку "Изменить" в блоке "Получатель".',
                            async () => {
                                await this.recipientEditableCard.changeButtonClick();
                                await this.browser.allure.runStep(
                                    'Открывается попап "Получатели" со списком получателей.',
                                    async () => {
                                        await this.editPopup.waitForVisibleRoot();
                                    }
                                );
                            }
                        );
                    }
                );
            },
            'Закрытие попапа "Получатели".': closeRecipientPopup,
            'Возврат к попапу "Получатели" из формы создания получателя.': returnToRecipientPopup,
            'Возврат к списку получателей из попапа "Изменить получателя".': returnToRecipientFromEditPopup,
            'Возврат к попапу "Изменить получателя" из попапа удаления получателя.':
                returnToEditRecipientFromDeletingPopup,
        },
    },
});
