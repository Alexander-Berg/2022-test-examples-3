import {
    makeCase,
    makeSuite,
    mergeSuites,
} from 'ginny';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {
    marketBrandedCarts,
} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/carts';
import {fillRecipientForm, prepareCheckoutPage} from '@self/root/src/spec/hermione/scenarios/checkout';
import PlacemarkMap from '@self/root/src/widgets/content/checkout/common/CheckoutTouchSimpleDeliveryEditor/components/PlacemarkMap/__pageObject';
import {OutletDeliveryInformation} from '@self/root/src/components/OutletDeliveryInformation/__pageObject';
import {yandexMarketPickupPoint} from '@self/root/src/spec/hermione/kadavr-mock/returns/reportMoscowReturnOutlets';
import userFormData from '@self/root/src/spec/hermione/configs/checkout/formData/user-card-postpaid';
import RecipientForm from '@self/root/src/components/RecipientForm/__pageObject';
import GroupedParcel
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/GroupedParcel/__pageObject';

export default makeSuite('Время прибытия заказа на точку самовывоза', {
    feature: 'Время прибытия заказа на точку самовывоза',
    environment: 'kadavr',
    issue: 'MARKETFRONT-53152',
    story: mergeSuites(
        {
            async beforeEach() {
                const testState = await this.browser.yaScenario(
                    this,
                    prepareMultiCartState,
                    marketBrandedCarts
                );

                await this.browser.yaScenario(
                    this,
                    prepareCheckoutPage,
                    {
                        items: testState.checkoutItems,
                        reportSkus: testState.reportSkus,
                        checkout2: true,
                    }
                );

                this.setPageObjects({
                    placemarkMap: () => this.createPageObject(PlacemarkMap, {
                        parent: this.deliveryEditor,
                    }),
                    recipientForm: () => this.createPageObject(RecipientForm, {
                        parent: this.recipientWizard,
                    }),
                    outletDeliveryInformation: () => this.createPageObject(OutletDeliveryInformation, {
                        parent: this.deliveryEditor,
                    }),
                    checkoutCard: () => this.createPageObject(GroupedParcel, {
                        parent: this.confirmationPage,
                    }),
                });

                await this.deliveryEditor.waitForSubmitButtonEnabled();
                await this.deliveryEditor.submitButtonClick();

                await this.deliveryEditor.waitForVisible();

                await this.placemarkMap.waitForVisible(4000);
                await this.placemarkMap.waitForReady(4000);

                await this.browser.allure.runStep(
                    'Кликнуть по доступному ПВЗ.',
                    async () => {
                        await this.placemarkMap.clickOnOutlet([
                            yandexMarketPickupPoint.gpsCoord.latitude,
                            yandexMarketPickupPoint.gpsCoord.longitude,
                        ]);
                    }
                );
            },
        },
        {
            'Экран ПВЗ': {
                'Должно корректно отображаться': makeCase({
                    id: 'bluemarket-4116',
                    async test() {
                        await this.outletDeliveryInformation.waitForVisible();

                        const deliveryDateString = 'в пятницу, 23 февраля';

                        await this.outletDeliveryInformation.getDeliveryInfo()
                            .should.eventually.to.be.contain(
                                deliveryDateString,
                                `Текст должен содержать ${deliveryDateString}`
                            );
                    },
                }),
            },
        },
        {
            'Чекаут': {
                'Должно корректно отображаться': makeCase({
                    id: 'bluemarket-4117',
                    async test() {
                        await this.allure.runStep(
                            'Нажать кнопку "Выбрать".', async () => {
                                await this.deliveryEditor.chooseButtonClick();
                            }
                        );

                        await this.browser.yaScenario(
                            this,
                            fillRecipientForm,
                            {
                                formName: 'user-card-postpaid',
                                formData: userFormData,
                                recipientForm: this.recipientForm,
                            }
                        );

                        await this.browser.allure.runStep(
                            'Нажать кнопку "Продолжить".',
                            async () => {
                                await this.deliveryEditor.submitButtonClick();
                                await this.confirmationPage.waitForVisible();
                            }
                        );

                        const expectedText = 'Доставка в пункт выдачи 23 февраля к 12:00';

                        await this.checkoutCard.getTitleText()
                            .should.eventually.to.be.contain(
                                expectedText,
                                `Текст должен содержать ${expectedText}`
                            );
                    },
                }),
            },
        }
    ),
});
