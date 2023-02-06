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
import GeoSuggest from '@self/root/src/components/GeoSuggest/__pageObject';
import PlacemarkMap
    from '@self/root/src/widgets/content/checkout/common/CheckoutVectorPlacemarkMap/components/VectorPlacemarkMap/__pageObject';
import OutletInfoCard from '@self/root/src/components/OutletInfoCard/__pageObject';
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
                    street: () => this.createPageObject(GeoSuggest, {
                        parent: this.deliveryEditorCheckoutWizard,
                    }),
                    placemarkMap: () => this.createPageObject(PlacemarkMap, {
                        parent: this.deliveryEditorCheckoutWizard,
                    }),
                    recipientForm: () => this.createPageObject(RecipientForm, {
                        parent: this.recipientWizard,
                    }),
                    outletInfoCard: () => this.createPageObject(OutletInfoCard, {
                        parent: this.deliveryEditorCheckoutWizard,
                    }),
                    checkoutCard: () => this.createPageObject(GroupedParcel, {
                        parent: this.confirmationPage,
                    }),
                });

                const testAddress = 'Москва';

                await this.deliveryEditor.waitForVisible();

                await this.street.setText(testAddress);
                await this.street.selectSuggestion(testAddress);

                await this.placemarkMap.waitForVisible(4000);
                await this.placemarkMap.waitForReady(4000);

                await this.browser.allure.runStep(
                    'Кликнуть по доступному ПВЗ.',
                    async () => {
                        await this.placemarkMap.clickOnOutlet([
                            yandexMarketPickupPoint.gpsCoord.longitude,
                            yandexMarketPickupPoint.gpsCoord.latitude,
                        ]);
                    }
                );
            },
        },
        {
            'Экран ПВЗ': {
                'Должно корректно отображаться': makeCase({
                    id: 'bluemarket-4111',
                    async test() {
                        await this.outletInfoCard.waitForVisible();

                        const deliveryDateString = 'Доставка 23 февраля к 12:00';

                        await this.outletInfoCard.getTitleText()
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
                    id: 'bluemarket-4112',
                    async test() {
                        await this.browser.allure.runStep(
                            'Нажать кнопку "Продолжить" для перехода к экрану "Получатель".',
                            async () => {
                                await this.deliveryEditorCheckoutWizard.submitButtonClick();
                                await this.recipientWizard.waitForVisible();
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
                                await this.recipientWizard.submitButtonClick();

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
