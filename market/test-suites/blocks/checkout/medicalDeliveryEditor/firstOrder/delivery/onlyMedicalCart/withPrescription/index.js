import {
    makeSuite,
    makeCase,
} from 'ginny';

import * as pharma from '@self/root/src/spec/hermione/kadavr-mock/report/pharma';
import {
    deliveryDeliveryMock,
    deliveryPickupMock,
    paymentOptions,
} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';
import x5outletMock from '@self/root/src/spec/hermione/kadavr-mock/report/outlets/x5outlet';
import {prepareCheckoutCartStateWithPharma} from '@self/root/src/spec/hermione/fixtures/cart/pharmaCart';
import {fillDeliveryTypeCheckoutForm} from '@self/root/src/spec/hermione/scenarios/checkout';
import MedicalParcelContent
    from '@self/root/src/widgets/content/checkout/common/CheckoutMedicalCartDeliveryEditor/components/MedicalParcelContent/__pageObject';
import SimpleMedicalShopOffer
    from '@self/root/src/widgets/content/checkout/common/CheckoutMedicalCartDeliveryEditor/components/MedicalParcelContent/components/SimpleMedicalShopOffer/__pageObject';
import GeoSuggest from '@self/root/src/components/GeoSuggest/__pageObject';
import OrderTotal from '@self/root/src/components/OrderTotalV2/__pageObject';

export default makeSuite('Доставка курьером.', {
    id: 'marketfront-5862',
    issue: 'MARKETFRONT-91160',
    feature: 'Покупка списком. Чекаут. Флоу повторного заказа',
    environment: 'kadavr',
    story: {
        async beforeEach() {
            this.setPageObjects({
                medicalParcelContent: () => this.createPageObject(MedicalParcelContent, {
                    parent: this.medicalCartDeliveryEditorCheckoutWizard,
                }),
                prescriptionSimpleMedicalShopOffer: () => this.createPageObject(SimpleMedicalShopOffer, {
                    root: `${SimpleMedicalShopOffer.root}:last-child`,
                    parent: this.medicalParcelContent,
                }),
                geoSuggest: () => this.createPageObject(GeoSuggest, {
                    parent: this.medicalCartDeliveryEditorCheckoutWizard,
                }),
                courierSuggestInput: () => this.createPageObject(GeoSuggest, {
                    parent: this.medicalCartDeliveryEditorCheckoutWizard,
                }),
                orderTotal: () => this.createPageObject(OrderTotal),
            });

            const medicalDelivery = {
                deliveryOptions: [
                    {
                        ...deliveryPickupMock,
                        paymentOptions: [
                            paymentOptions.yandex,
                            paymentOptions.cashOnDelivery,
                        ],
                        outlets: [
                            {id: x5outletMock.id, regionId: 0},
                            {id: pharma.outletMock.id, regionId: 0},
                        ],
                    },
                    deliveryDeliveryMock,
                ],
                outlets: [
                    x5outletMock,
                    pharma.outletMock,
                ],
            };

            await prepareCheckoutCartStateWithPharma.call(this, {delivery: medicalDelivery, withPrescriptionCart: true});
        },
        'В корзине присутствуют рецептурные лекарственные товары': makeCase({
            async test() {
                const titleText = 'Доставка лекарств';

                await this.medicalCartDeliveryEditorCheckoutWizard
                    .getTitleText()
                    .should.eventually.to.be.equal(
                        titleText,
                        `Текст заголовка блока с оформлением заказа должен быть "${titleText}".`
                    );

                await this.browser.allure.runStep(
                    'По дефолту отображается способ доставки "Курьером"',
                    async () => {
                        await this.deliveryTypes
                            .isCheckedDeliveryTypeDelivery()
                            .should.eventually.to.be.equal(
                                true,
                                'Должна отображаться доставка "Курьером."'
                            );
                    }
                );

                await this.browser.allure.runStep(
                    'По дефолту отображается бейдж "Самовывоз" у рецептурного товара',
                    async () => {
                        await this.prescriptionSimpleMedicalShopOffer
                            .isOnlyPickupBadgeVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'Должен быть виден бейдж "Самовывоз" у рецептурного товара'
                            );
                    }
                );

                await this.browser.yaScenario(
                    this,
                    fillDeliveryTypeCheckoutForm
                );

                await this.browser.allure.runStep(
                    'Кнопка "Продолжить" активна.',
                    async () => {
                        await this.medicalCartDeliveryEditorCheckoutWizard
                            .isSubmitButtonDisabled()
                            .should.eventually.to.be.equal(
                                false,
                                'Кнопка "Продолжить" должна быть активна.'
                            );
                    }
                );

                await this.medicalCartDeliveryEditorCheckoutWizard.submitButtonClick();

                await this.confirmationPage.waitForVisible();

                await this.browser.allure.runStep(
                    'Блок "Доставка" с лекарственными товарами.',
                    async () => {
                        await this.groupedParcels
                            .getAddressTitleByCardIndex(0)
                            .should.eventually.include(
                                'Доставка курьером 23 февраля – 8 марта•250₽',
                                'Текст заголовка должен содержать "Доставка".'
                            );
                    }
                );

                await this.browser.allure.runStep(
                    'Блок "Доставка" с лекарственными товарами - адрес аптеки.',
                    async () => {
                        await this.groupedParcels
                            .getInfoContentByCardIndex(0)
                            .should.eventually.include(
                                'Курьером\nМосква, Красная площадь, д. 1',
                                'Текст информации о магазине должен содержать адрес доставки.'
                            );
                    }
                );

                await this.browser.allure.runStep(
                    'Блок "Получатель".',
                    async () => {
                        await this.recipientBlock
                            .isChooseRecipientButtonVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'На карточке получателя должна отображается кнопка "Укажите данные получателя".'
                            );
                    }
                );

                await this.browser.allure.runStep(
                    'Кнопка "Перейти к оплате" не активна.',
                    async () => {
                        await this.checkoutOrderButton
                            .isButtonDisabled()
                            .should.eventually.to.be.equal(
                                true,
                                'Кнопка "Перейти к оплате" должна быть не активна.'
                            );
                    }
                );

                await this.browser.allure.runStep(
                    'Количество товаров в блоке Итого соответствует количеству безрецептурных товаров заказа.',
                    async () => {
                        await this.orderTotal
                            .getItemsCount()
                            .should.eventually.to.be.equal(
                                1,
                                'Количество товаров в блоке "Итого" должно быть равно 1.'
                            );
                    }
                );
            },
        }),
    },
});
