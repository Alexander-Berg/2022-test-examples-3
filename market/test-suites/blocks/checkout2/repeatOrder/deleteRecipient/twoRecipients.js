import {
    makeSuite,
    makeCase,
} from 'ginny';

// scenarios
import {
    ACTUALIZATION_TIMEOUT,
    addPresetForRepeatOrder,
    prepareCheckouterPageWithCartsForRepeatOrder,
} from '@self/root/src/spec/hermione/scenarios/checkout';

import {region} from '@self/root/src/spec/hermione/configs/geo';

import {ADDRESSES, CONTACTS} from '../../constants';

const firstContact = CONTACTS.DEFAULT_CONTACT;
const secondContact = CONTACTS.HSCH_CONTACT;
export default makeSuite('Указаны данные двух пользователей.', {
    feature: 'Указаны данные двух пользователей.',
    params: {
        carts: 'Корзины',
        isAuthWithPlugin: 'Авторизован ли пользователь',
        region: 'Регион',
    },
    defaultParams: {
        region: region['Москва'],
        isAuthWithPlugin: true,
    },
    environment: 'kadavr',
    story: {
        async beforeEach() {
            await this.browser.yaScenario(
                this,
                addPresetForRepeatOrder,
                {
                    address: ADDRESSES.MOSCOW_ADDRESS,
                    contact: CONTACTS.DEFAULT_CONTACT,
                }
            );
            await this.browser.setState(`persAddress.contact.${secondContact.id}`, {
                ...secondContact,
                ...secondContact.recipient,
                recipient: secondContact.recipient.name,
                phoneNum: secondContact.phone,
            });

            await this.browser.yaScenario(
                this,
                prepareCheckouterPageWithCartsForRepeatOrder,
                {
                    carts: this.params.carts,
                    options: {
                        region: this.params.region,
                        checkout2: true,
                    },
                }
            );
        },
        'Открыть главную страницу чекаута.': makeCase({
            async test() {
                const firstRecipient =
                    `${firstContact.recipient}\n${firstContact.email}, ${firstContact.phone}`;
                const secondRecipient =
                    `${secondContact.recipient.name}\n${secondContact.email}, ${secondContact.phone}`;

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
                                        firstRecipient,
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
                        await this.recipientList.clickOnEditButtonByRecipient(firstRecipient);
                        await this.browser.allure.runStep(
                            'Открывается форма редактирования данных пользователя "Изменить получателя".',
                            async () => {
                                await this.recepientForm.waitForVisible();
                            }
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Нажать на кнопку "Удалить".',
                    async () => {
                        await this.recepientForm.deleteButtonClick();
                        await this.allure.runStep('Появляется попап "И правда хотите удалить получателя?".', async () => {
                            await this.deleteForm.waitForVisible();
                        });

                        await this.browser.allure.runStep(
                            'В попапе отображаются данные получателя.',
                            async () => {
                                await this.editableRecipientCard.getContactText()
                                    .should.eventually.to.be.equal(
                                        firstRecipient,
                                        'В попапе должны отображаться данные получателя.'
                                    );
                            }
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Нажать на кнопку "Удалить" в попапе подтверждения удаления.',
                    async () => {
                        await this.deleteForm.deleteButtonClick();
                        await this.recipientList.waitForVisible();

                        await this.browser.allure.runStep(
                            'Активным отображается второй пресет получателя.',
                            async () => {
                                await this.recipientList.getActiveItemText()
                                    .should.eventually.to.be.equal(
                                        secondRecipient,
                                        'Активным должен отображаться второй пресет получателя.'
                                    );
                            }
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Закрыть попап "Получатели" по кнопке "Х".',
                    async () => {
                        await this.popupBase.clickOnCrossButton();
                        await this.browser.allure.runStep(
                            'В блоке "Получатель" отображаются данные получателя из второго пресета.',
                            async () => {
                                await this.recipientBlock.getContactText()
                                    .should.eventually.to.be.equal(
                                        secondRecipient,
                                        'На карточке получателя должны быть данные из второго пресета.'
                                    );
                            }
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Перезагрузить страницу.',
                    async () => {
                        await this.browser.refresh();
                    }
                );

                await this.confirmationPage.waitForVisible();
                if (await this.preloader.waitForVisible(1000)) {
                    await this.preloader.waitForHidden(ACTUALIZATION_TIMEOUT);
                }

                await this.browser.allure.runStep(
                    'В блоке "Получатель" отображаются данные получателя из второго пресета.',
                    async () => {
                        await this.recipientBlock.getContactText()
                            .should.eventually.to.be.equal(
                                secondRecipient,
                                'На карточке получателя должны быть данные из второго пресета.'
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
                    'Активным отображается второй пресет получателя.',
                    async () => {
                        await this.recipientList.getActiveItemText()
                            .should.eventually.to.be.equal(
                                secondRecipient,
                                'Активным должен отображаться второй пресет получателя.'
                            );
                    }
                );

                await this.browser.allure.runStep(
                    'В списке отсутствует удаленный пресет.',
                    async () => {
                        await this.recipientList.getCardIndexByRecipient(firstContact)
                            .should.eventually.to.be.equal(
                                -1,
                                'В списке не должен отображаться удаленный пресет.'
                            );
                    }
                );
            },
        }),
    },
});
