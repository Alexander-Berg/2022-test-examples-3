import {makeCase, makeSuite} from 'ginny';

import RecipientForm from '@self/root/src/components/RecipientForm/__pageObject';
import RecipientPopupContainer
    from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/Popup/__pageObject/index.touch';
import RecipientListTouch
    from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/Popup/components/RecipientList/__pageObject/index.touch';
import * as pharma from '@self/root/src/spec/hermione/kadavr-mock/report/pharma';
import {
    goToConfirmationPageAfterMedicalPickupDelivery,
} from '@self/root/src/spec/hermione/scenarios/checkout/touch/goToConfirmationPageAfterMedical';

export default makeSuite('Оформление повторного заказа. Шаг 3.', {
    id: 'marketfront-5900',
    issue: 'MARKETFRONT-81908',
    feature: 'Оформление повторного заказа. Шаг 3',
    environment: 'kadavr',
    story: {
        async beforeEach() {
            this.setPageObjects({
                recipientForm: () => this.createPageObject(RecipientForm),
                recipientPopup: () => this.createPageObject(RecipientPopupContainer),
                recipientList: () => this.createPageObject(RecipientListTouch),
            });

            await this.browser.yaScenario(
                this,
                goToConfirmationPageAfterMedicalPickupDelivery
            );

            await this.browser.allure.runStep(
                'Нажать на кнопку "Выбрать пункт выдачи" в блоке заказа.',
                async () => {
                    await this.addressBlocks.clickChangeButtonByIndex(0);
                    await this.editPopup.waitForVisibleRoot(5000);
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
                'Для C&C выбрать доступный ПВЗ.', async () => {
                    await this.placemarkMap.waitForVisible(2000);
                    await this.placemarkMap.waitForReady(4000);

                    await this.placemarkMap.clickOnPlacemark([
                        pharma.outletMock.gpsCoord.latitude,
                        pharma.outletMock.gpsCoord.longitude,
                    ], 20);
                }
            );

            await this.allure.runStep(
                'Нажать кнопку "Выбрать".', async () => {
                    await this.deliveryEditor.chooseButtonClick();
                }
            );

            await this.allure.runStep(
                'Открыть страницу чекаута', async () => {
                    await this.confirmationPage.waitForVisible();
                    await this.preloader.waitForHidden(5000);
                }
            );
        },

        'Проверка заказа фешена и переход к оплате".': makeCase({
            async test() {
                await this.browser.allure.runStep(
                    'Блок "Самовывоз" фешн заказа - адрес пункта выдачи и часы работы.',
                    async () => {
                        const outletInfo = ['Магазин Retest Full 1\n'] +
                            ['Москва, Сходненская, д. 11, стр. 1\n'] +
                            ['Ежедневно\n'] +
                            ['10:00 – 22:00'];

                        await this.addressBlocks.getInfoTitleByCardIndex(1)
                            .should.eventually.include(
                                outletInfo,
                                `Текст в поле адрес должен быть "${outletInfo}".`
                            );
                    }
                );

                await this.browser.allure.runStep(
                    'Кнопка "Перейти к оплате" не активна.',
                    async () => {
                        await this.checkoutOrderButton
                            .isButtonDisabled()
                            .should.eventually.to.be.equal(
                                false,
                                'Кнопка "Перейти к оплате" должна быть активна.'
                            );
                    }
                );

                await this.browser.allure.runStep(
                    'Ожидаем изменения урла на: "/my/orders/payment".',
                    async () => {
                        await this.browser.setState('Checkouter.options', {isCheckoutSuccessful: true});

                        await this.browser.yaWaitForChangeUrl(
                            async () => {
                                await this.checkoutOrderButton.click();
                            },
                            5000
                        );

                        await this.browser.getUrl()
                            .should.eventually.to.be.link({
                                query: {
                                    orderId: /\d+/,
                                },
                                pathname: '/my/orders/payment',
                            }, {
                                mode: 'match',
                                skipProtocol: true,
                                skipHostname: true,
                            });
                    }
                );
            },
        }),
    },
});
