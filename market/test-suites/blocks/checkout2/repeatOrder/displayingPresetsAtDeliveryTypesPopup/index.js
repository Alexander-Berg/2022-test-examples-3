import {makeCase, makeSuite} from 'ginny';

import {
    prepareCheckouterPageWithCartsForRepeatOrder,
    addPresetForRepeatOrder,
} from '@self/root/src/spec/hermione/scenarios/checkout';

import {region} from '@self/root/src/spec/hermione/configs/geo';

import CheckoutWizard from '@self/root/src/widgets/content/checkout/layout/components/wizard/__pageObject';
import EditPopup
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/__pageObject';
import DeliveryTypeList from '@self/root/src/components/DeliveryTypes/__pageObject';

import {ADDRESSES, CONTACTS} from '../../constants';
import {getExpressCart} from './helpers';

export default makeSuite(
    'Отсутстивие отображения пресетов доставки в попапе "Способ доставки" и на экране "Как доставить заказ".', {
        id: 'marketfront-5040',
        issue: 'MARKETFRONT-54577',
        feature: 'Редактирование адреса в форме экрана "Как доставить заказ?"',
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
                    deliveryEditorCheckoutWizard: () => this.createPageObject(CheckoutWizard),
                    editPopup: () => this.createPageObject(EditPopup),
                    popupDeliveryTypes: () => this.createPageObject(DeliveryTypeList, {
                        parent: this.editPopup,
                    }),
                });

                const carts = [
                    getExpressCart(),
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
            'В блоке "Доставки", нажать на кнопку "Изменить".': {
                async beforeEach() {
                    await this.allure.runStep(
                        'Открывается главный экран чекаута.', async () => {
                            await this.confirmationPage.waitForVisible();
                            await this.deliveryInfo.waitForVisible();
                        }
                    );

                    await this.allure.runStep(
                        'В блоке доставки нажать кнопку "Изменить".', async () => {
                            await this.addressEditableCard.changeButtonClick();
                            await this.editPopup.waitForVisibleRoot();
                        }
                    );
                },
                'Открывается попап "Способ доставки"': makeCase({
                    async test() {
                        await this.allure.runStep(
                            'В попапе отображается заголовок "Мои способы доставки"', async () => {
                                const title = 'Мои способы доставки';

                                await this.editPopup.getTitleText().should.eventually.to.be.equal(
                                    title,
                                    `Текст заголовка блока должен быть ${title}.`
                                );
                            }
                        );

                        await this.allure.runStep(
                            'В попапе отображается только пресет доставки "Курьером".', async () => {
                                await this.popupDeliveryTypes.isExisting()
                                    .should.eventually.to.be.equal(
                                        true,
                                        'Блока типов доставки должен отображаться.'
                                    );

                                await this.popupDeliveryTypes.deliveryTypeDeliveryIsExisting()
                                    .should.eventually.to.be.equal(
                                        true,
                                        'Должен отображаться способ доставки "Курьер".'
                                    );

                                await this.popupDeliveryTypes.deliveryTypePickupIsExisting()
                                    .should.eventually.to.be.equal(
                                        false,
                                        'Способ доставки "Самовывоз" не должен отображаться.'
                                    );

                                await this.popupDeliveryTypes.deliveryTypePostIsExisting()
                                    .should.eventually.to.be.equal(
                                        false,
                                        'Способ доставки "Почта" не должен отображаться.'
                                    );
                            }
                        );

                        await this.allure.runStep(
                            'Нажать на кнопку "Добавить новый адрес"', async () => {
                                await this.editPopup.addButtonClick();

                                await this.allure.runStep(
                                    'Открывается экран "Как доставить заказ?"', async () => {
                                        const title = 'Как доставить заказ?';

                                        await this.deliveryEditorCheckoutWizard.waitForVisible();
                                        await this.deliveryEditorCheckoutWizard.getTitleText()
                                            .should.eventually.to.be.equal(
                                                title,
                                                `Текст заголовка блока должен быть ${title}.`
                                            );
                                    }
                                );

                                await this.allure.runStep(
                                    'На экране отображается только пресет доставки "Курьером".', async () => {
                                        await this.deliveryTypes.isExisting()
                                            .should.eventually.to.be.equal(
                                                true,
                                                'Блока типов доставки должен отображаться.'
                                            );

                                        await this.deliveryTypes.deliveryTypeDeliveryIsExisting()
                                            .should.eventually.to.be.equal(
                                                true,
                                                'Должен отображаться способ доставки "Курьер".'
                                            );

                                        await this.deliveryTypes.deliveryTypePickupIsExisting()
                                            .should.eventually.to.be.equal(
                                                false,
                                                'Способ доставки "Самовывоз" не должен отображаться.'
                                            );

                                        await this.deliveryTypes.deliveryTypePostIsExisting()
                                            .should.eventually.to.be.equal(
                                                false,
                                                'Способ доставки "Почта" не должен отображаться.'
                                            );
                                    }
                                );
                            }
                        );
                    },
                }),
            },
        },
    });
