import {makeCase, makeSuite} from 'ginny';

import {
    prepareCheckouterPageWithCartsForRepeatOrder,
    addPresetForRepeatOrder,
} from '@self/root/src/spec/hermione/scenarios/checkout';

import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import {region} from '@self/root/src/spec/hermione/configs/geo';

import CheckoutRecipient
    from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/CheckoutRecipient/__pageObject';
import RecipientList
    from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/Popup/components/RecipientList/__pageObject';
import RecipientForm
    from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/Popup/components/RecipientForm/__pageObject';

import {ADDRESSES, CONTACTS} from '../../constants';

export default makeSuite('Новый получатель.', {
    id: 'marketfront-4915',
    issue: 'MARKETFRONT-54582',
    feature: 'Новый получатель.',
    params: {
        region: 'Регион',
        isAuthWithPlugin: 'Авторизован ли пользователь',
    },
    defaultParams: {
        region: region['Москва'],
        isAuth: false,
    },
    environment: 'kadavr',
    story: {
        async beforeEach() {
            this.setPageObjects({
                recipientBlock: () => this.createPageObject(CheckoutRecipient, {
                    parent: this.confirmationPage,
                }),
                recipientList: () => this.createPageObject(RecipientList),
                recipientForm: () => this.createPageObject(RecipientForm),
            });

            const carts = [
                buildCheckouterBucket({
                    items: [{
                        skuMock: kettle.skuMock,
                        offerMock: kettle.offerMock,
                        count: 1,
                    }],
                }),
            ];

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
                    carts,
                    options: {
                        region: this.params.region,
                        checkout2: true,
                    },
                }
            );
        },
        'Открыть страницу чекаута': {
            async beforeEach() {
                await this.confirmationPage.waitForVisible();
                await this.deliveryInfo.waitForVisible();
            },
            'В блоке "Получатель"': makeCase({
                async test() {
                    const newRecipient = {
                        name: 'Кто Я',
                        email: 'asdasdasd@yandex.ru',
                        phone: '+78889990011',
                    };

                    await this.allure.runStep(
                        'Отображается информация о получателе', async () => {
                            const recipient = CONTACTS.DEFAULT_CONTACT;
                            await this.recipientBlock.waitForVisible();
                            await this.recipientBlock.getContactText().should.eventually.to.be.equal(
                                `${recipient.recipient}\n${recipient.email}, ${recipient.phone}`,
                                'На карточке должны быть указаны данные получателя первого заказа'
                            );
                        }
                    );

                    await this.allure.runStep(
                        'Нажать на кнопку "Изменить"', async () => {
                            await this.recipientBlock.editRecipientButtonClick();
                            await this.allure.runStep(
                                'Открывается попап "Получатели" со списком получателей', async () => {
                                    await this.recipientList.waitForVisible();
                                }
                            );
                            await this.allure.runStep(
                                'Нажать на кнопку "Добавить получателя"', async () => {
                                    await this.recipientList.addRecipientButtonClick();
                                    await this.allure.runStep('Открывается форма указания данных получателя', async () => {
                                        await this.recipientForm.waitForVisible();
                                    });
                                }
                            );

                            await this.allure.runStep(
                                'Заполнить поля валидными значениями', async () => {
                                    await this.recipientForm.setTextForm(newRecipient);
                                }
                            );

                            await this.allure.runStep(
                                'Нажать на кнопку "Сохранить"', async () => {
                                    await this.recipientForm.saveButtonClick();

                                    await this.allure.runStep(
                                        'Отображается список пресетов', async () => {
                                            await this.recipientList.waitForVisible();
                                        }
                                    );

                                    await this.allure.runStep(
                                        'Добавленный пресет отображается активным', async () => {
                                            await this.recipientList.getActiveItemText().should.eventually.to.be.equal(
                                                `${newRecipient.name}\n${newRecipient.email}, ${newRecipient.phone}`,
                                                'В пресеты должны быть указаны данные нового получателя.'
                                            );
                                        }
                                    );
                                }
                            );

                            await this.allure.runStep(
                                'Нажать на кнопку "Выбрать".', async () => {
                                    await this.recipientList.chooseRecipientButtonClick();
                                    await this.recipientList.isExisting().should.eventually.to.be.equal(
                                        false,
                                        'Попап "Получатели" закрывается.'
                                    );
                                    await this.allure.runStep(
                                        'В блоке "Получатель"', async () => {
                                            await this.recipientBlock.waitForVisible();
                                            await this.recipientBlock.getContactText().should.eventually.to.be.equal(
                                                `${newRecipient.name}\n${newRecipient.email}, ${newRecipient.phone}`,
                                                'На карточке должны быть указаны данные нового получателя'
                                            );
                                        }
                                    );
                                }
                            );
                        }
                    );
                    await this.allure.runStep(
                        'Перезагрузить страницу.', async () => {
                            await this.browser.yaPageReload(5000, ['state']);
                        }
                    );

                    await this.allure.runStep(
                        'Отображается информация о получателе', async () => {
                            await this.recipientBlock.waitForVisible();
                            await this.recipientBlock.getContactText().should.eventually.to.be.equal(
                                `${newRecipient.name}\n${newRecipient.email}, ${newRecipient.phone}`,
                                'На карточке должны быть указаны данные нового получателя'
                            );
                        }
                    );
                },
            }),
        },
    },
});
