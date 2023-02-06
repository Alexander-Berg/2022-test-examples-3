import {
    makeSuite,
    makeCase,
} from 'ginny';

// scenarios
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {
    addPresetForRepeatOrder,
    prepareCheckouterPageWithCartsForRepeatOrder,
    userFieldChecker,
} from '@self/root/src/spec/hermione/scenarios/checkout';

// pageObjects
import RecipientList from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/Popup/components/RecipientList/__pageObject';
import RecipientForm from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/Popup/components/RecipientForm/__pageObject';
import RecipientFormFields from '@self/root/src/components/RecipientForm/__pageObject';

// mocks
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';

import {region} from '@self/root/src/spec/hermione/configs/geo';
import {DELIVERY_TYPES} from '@self/root/src/constants/delivery';

import {ADDRESSES, CONTACTS} from '../../constants';

const simpleCarts = [
    buildCheckouterBucket({
        items: [{
            skuMock: kettle.skuMock,
            offerMock: kettle.offerMock,
            count: 1,
        }],
    }),
];

const orders = [
    {
        id: 777,
        delivery: {
            type: DELIVERY_TYPES.DELIVERY,
            regionId: region['Москва'],
            buyerAddress: ADDRESSES.MOSCOW_ADDRESS,
        },
    },
];

export default makeSuite('Редактирование данных получателя в попапе "Изменить получателя".', {
    id: 'marketfront-4430',
    issue: 'MARKETFRONT-36080',
    feature: 'Редактирование данных получателя в попапе "Изменить получателя".',
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
                recipientList: () => this.createPageObject(RecipientList),
                recepientForm: () => this.createPageObject(RecipientForm),
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

            await this.browser.setState('Checkouter.collections.order', orders);

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
        'Открыть главную страницу чекаута.': makeCase({
            async test() {
                const recipient =
                    `${CONTACTS.DEFAULT_CONTACT.recipient}\n${CONTACTS.DEFAULT_CONTACT.email}, ${CONTACTS.DEFAULT_CONTACT.phone}`;
                const editedRecipient = 'Проверка Редактирования\ntesting@testing.com, 89876543210';

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

                await this.browser.allure.runStep(
                    'Нажать на кнопку "Карандаш".',
                    async () => {
                        await this.recipientList.clickOnEditButtonByRecipient(recipient);
                        await this.browser.allure.runStep(
                            'Открывается форма редактирования данных пользователя "Изменить получателя".',
                            async () => {
                                await this.recepientForm.waitForVisible();
                            }
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Поле "Имя и фамилия".',
                    async () => {
                        const nameTextFieldSelector = `${RecipientFormFields.name}`;
                        await this.browser.yaScenario(
                            this,
                            userFieldChecker,
                            {
                                selector: nameTextFieldSelector,
                                inputText: 'Проверка Редактирования',
                            }
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Поле "Электронная почта".',
                    async () => {
                        const emailTextFieldSelector = `${RecipientFormFields.email}`;
                        await this.browser.yaScenario(
                            this,
                            userFieldChecker,
                            {
                                selector: emailTextFieldSelector,
                                inputText: 'testing@testing.com',
                            }
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Поле "Телефон".',
                    async () => {
                        const phoneTextFieldSelector = `${RecipientFormFields.phone}`;
                        await this.browser.yaScenario(
                            this,
                            userFieldChecker,
                            {
                                selector: phoneTextFieldSelector,
                                inputText: '89876543210',
                            }
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Нажать на кнопку "Сохранить".',
                    async () => {
                        await this.recepientForm.saveButtonClick();

                        await this.browser.allure.runStep(
                            'На экране появляется попап "Получатель".',
                            async () => {
                                await this.recipientList.waitForVisible();
                            }
                        );

                        await this.browser.allure.runStep(
                            'В пресете получателя отображаются введенные данные.',
                            async () => {
                                await this.recipientList.getCardIndexByRecipient(editedRecipient)
                                    .should.not.be.equal(
                                        -1,
                                        'В пресете должны отображаться введенные данные.'
                                    );
                            }
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Закрыть попап "Получатель".',
                    async () => {
                        await this.popupBase.clickOnCrossButton();

                        await this.browser.allure.runStep(
                            'В блоке "Получатель" отображаются новые данные получателя.',
                            async () => {
                                await this.recipientBlock.getContactText()
                                    .should.eventually.to.be.equal(
                                        editedRecipient,
                                        'На карточке получателя должны быть указанные пользователем данные'
                                    );
                            }
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Перезагрузить страницу.',
                    async () => {
                        await this.browser.refresh();
                        await this.browser.allure.runStep(
                            'Отображается главный экран чекаута.',
                            async () => {
                                await this.confirmationPage.waitForVisible();
                                await this.deliveryInfo.waitForVisible();
                            }
                        );

                        await this.browser.allure.runStep(
                            'В блоке "Получатель" отображаются новые данные получателя.',
                            async () => {
                                await this.recipientBlock.getContactText()
                                    .should.eventually.to.be.equal(
                                        editedRecipient,
                                        'На карточке получателя должны быть указанные пользователем данные'
                                    );
                            }
                        );
                    }
                );
            },
        }),
    },
});
