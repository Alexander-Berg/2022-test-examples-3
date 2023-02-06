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

export default makeSuite('Указаны данные одного пользователя.', {
    feature: 'Указаны данные одного пользователя.',
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
                const recipient =
                    `${CONTACTS.DEFAULT_CONTACT.recipient}\n${CONTACTS.DEFAULT_CONTACT.email}, ${CONTACTS.DEFAULT_CONTACT.phone}`;

                await this.browser.allure.runStep(
                    'Отображается главный экран чекаута.',
                    async () => {
                        await this.confirmationPage.waitForVisible();

                        await this.browser.allure.runStep(
                            'В блоке "Получатель" отображается информация о получателе.',
                            async () => {
                                await this.recipientBlock.getContactText()
                                    .should.eventually.to.be.equal(
                                        recipient,
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
                        await this.recipientList.clickEditButtonByRecipient(recipient);
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
                                        recipient,
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
                                await this.recipientFormFields.waitForVisible();
                                await this.recipientPopup.getRecipientTitle()
                                    .should.eventually.to.be.equal(
                                        title,
                                        `Заголовок попапа должен быть "${title}".`
                                    );
                            }
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Закрыть попап "Получатель".',
                    async () => {
                        await this.recipientPopup.backRecipientButtonClick();

                        await this.browser.allure.runStep(
                            'В блоке "Получатель" отображаться кнопка "Выбрать получателя".',
                            async () => {
                                await this.recipientBlock.isChooseRecipientButtonVisible()
                                    .should.eventually.to.be.equal(
                                        true,
                                        'На карточке получателя должна отображаться кнопка "Выбрать получателя".'
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
                    'В блоке "Получатель" отображаться кнопка "Выбрать получателя".',
                    async () => {
                        await this.recipientBlock.isChooseRecipientButtonVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'На карточке получателя должна отображаться кнопка "Выбрать получателя".'
                            );
                    }
                );

                await this.browser.allure.runStep(
                    'Нажать на кнопку "Выбрать получателя".',
                    async () => {
                        const title = 'Получатель';

                        await this.recipientBlock.chooseRecipientButtonClick();
                        await this.browser.allure.runStep(
                            `Появляется попап "${title}".`,
                            async () => {
                                await this.recipientFormFields.waitForVisible();
                                await this.recipientPopup.getRecipientTitle()
                                    .should.eventually.to.be.equal(
                                        title,
                                        `Заголовок попапа должен быть "${title}".`
                                    );
                            }
                        );
                    }
                );
            },
        }),
    },
});
