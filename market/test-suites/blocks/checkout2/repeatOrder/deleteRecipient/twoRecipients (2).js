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

import {ADDRESSES, CONTACTS} from '../constants';

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
                    contact: firstContact,
                }
            );
            await this.browser.setState(`persAddress.contact.${secondContact.id}`, secondContact);

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
                    `${secondContact.recipient}\n${secondContact.email}, ${secondContact.phone}`;

                await this.browser.allure.runStep(
                    'Отображается главный экран чекаута.',
                    async () => {
                        await this.confirmationPage.waitForVisible();

                        await this.browser.allure.runStep(
                            'В блоке "Получатель" отображается информация о получателе.',
                            async () => {
                                await this.recipientBlock.getContactText()
                                    .should.eventually.to.be.equal(
                                        firstRecipient,
                                        'На карточке получателя должны отображаться данные указанные пользователем'
                                    );
                            }
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Тапнуть на блок "Получатель".',
                    async () => {
                        await this.recipientBlock.waitForVisible();
                        await this.recipientBlock.onClick();

                        await this.browser.allure.runStep(
                            'Открывается попап "Получатели" со списком получателей.',
                            async () => {
                                await this.recipientPopup.waitForVisible();
                            }
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Нажать на кнопку "Карандаш".',
                    async () => {
                        await this.recipientList.clickEditButtonByRecipient(firstRecipient);
                        await this.browser.allure.runStep(
                            'Открывается форма редактирования данных пользователя "Изменить получателя".',
                            async () => {
                                await this.recipientFormFields.waitForVisible();
                            }
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Нажать на кнопку "Удалить".',
                    async () => {
                        const title = 'И правда хотите удалить получателя?';
                        await this.recipientPopup.deleteRecipientButtonClick();

                        await this.browser.allure.runStep(
                            `Появляется попап "${title}".`,
                            async () => {
                                await this.deleteForm.waitRootForVisible();
                                await this.recipientPopup.getDeletingTitle()
                                    .should.eventually.to.be.equal(
                                        title,
                                        `Заголовок попапа должен быть "${title}".`
                                    );
                            }
                        );

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
                        const title = 'Получатель';

                        await this.recipientPopup.deletingButtonClick();
                        await this.browser.allure.runStep(
                            `Появляется попап "${title}".`,
                            async () => {
                                await this.recipientList.waitForVisible();
                                await this.recipientPopup.getNewRecipientTitle()
                                    .should.eventually.to.be.equal(
                                        title,
                                        `Заголовок попапа должен быть "${title}".`
                                    );
                            }
                        );

                        await this.browser.allure.runStep(
                            'В блоке "Получатель" отображаются данные получателя из второго пресета.',
                            async () => {
                                /**
                                 * @ifSuccess удалить след. строку
                                 * @ticket MARKETFRONT-59614
                                 * next-line
                                 */
                                await this.recipientList.clickRecipientListItemByRecipient(secondRecipient);
                                await this.recipientList.isItemWithRecipientChecked(secondRecipient)
                                    .should.eventually.to.be.equal(
                                        true,
                                        'В блоке "Получатель" должны отображаться данные получателя из второго пресета.'
                                    );
                            }
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Закрыть попап "Получатель".',
                    async () => {
                        await this.recipientPopup.crossRecipientButtonClick();

                        await this.browser.allure.runStep(
                            'В блоке "Получатель" отображаются данные второго получателя.',
                            async () => {
                                await this.recipientBlock.getContactText()
                                    .should.eventually.to.be.equal(
                                        secondRecipient,
                                        'На карточке получателя должны быть данные второго получателя.'
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
                if (await this.summaryPlaceholder.waitForVisible(1000)) {
                    await this.summaryPlaceholder.waitForHidden(ACTUALIZATION_TIMEOUT);
                }

                await this.browser.allure.runStep(
                    'В блоке "Получатель" отображаются данные второго получателя.',
                    async () => {
                        await this.recipientBlock.getContactText()
                            .should.eventually.to.be.equal(
                                secondRecipient,
                                'На карточке получателя должны быть данные второго получателя.'
                            );
                    }
                );

                await this.browser.allure.runStep(
                    'Тапнуть на блок "Получатель".',
                    async () => {
                        await this.recipientBlock.waitForVisible();
                        await this.recipientBlock.onClick();

                        await this.browser.allure.runStep(
                            'Открывается попап "Получатели" со списком получателей.',
                            async () => {
                                await this.recipientPopup.waitForVisible();
                            }
                        );

                        await this.browser.allure.runStep(
                            'Активным отображается пресет с данными второго пользователя.',
                            async () => {
                                await this.recipientList.isItemWithRecipientChecked(secondRecipient)
                                    .should.eventually.to.be.equal(
                                        true,
                                        'В блоке "Получатель" должны отображаться данные получателя из второго пресета.'
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
                    }
                );
            },
        }),
    },
});
