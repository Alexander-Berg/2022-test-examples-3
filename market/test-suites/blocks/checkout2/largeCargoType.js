import {makeCase, makeSuite} from 'ginny';

import {
    skuMock as largeCargoTypeSkuMock,
    offerMock as largeCargoTypeOfferMock,
} from '@self/root/src/spec/hermione/kadavr-mock/report/largeCargoType';
import DeliveryTypeList from '@self/root/src/components/DeliveryTypes/__pageObject';
import CourierSuggest from
    '@self/root/src/widgets/content/checkout/common/CheckoutDeliveryEditor/components/CourierSuggest/__pageObject';

import {prepareCheckoutPage} from '@self/root/src/spec/hermione/scenarios/checkout';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';

const largeCargoCarts = [
    buildCheckouterBucket({
        items: [{
            skuMock: largeCargoTypeSkuMock,
            offerMock: largeCargoTypeOfferMock,
            cargoTypes: largeCargoTypeOfferMock.cargoTypes,
            count: 1,
        }],
    }),
];

export default makeSuite('Доставка КГТ', {
    environment: 'kadavr',
    id: 'marketfront-4437',
    issue: 'MARKETFRONT-36930',
    feature: 'Доставка КГТ',
    params: {
        skuId: 'Идентификатор SKU',
        slug: 'Транслитерированное название sku',
        region: 'Регион проверки',
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                deliveryTypes: () => this.createPageObject(DeliveryTypeList, {
                    parent: this.deliveryEditorCheckoutWizard,
                }),
                courierSuggest: () => this.createPageObject(CourierSuggest, {
                    parent: this.deliveryEditorCheckoutWizard,
                }),
            });

            const testState = await this.browser.yaScenario(
                this,
                prepareMultiCartState,
                largeCargoCarts
            );

            await this.browser.yaScenario(
                this,
                prepareCheckoutPage,
                {
                    items: testState.checkoutItems,
                    reportSkus: testState.reportSkus,
                }
            );

            await this.deliveryTypes.waitForVisible();
        },
        'Открыть страницу чекаута.': {
            'Отсутствие доступности способов доставки для КГТ, кроме курьерской и самовывоза.': makeCase({
                async test() {
                    const titleText = 'Как доставить заказ?';

                    await this.browser.allure.runStep(
                        `Отображается экран "${titleText}".`,
                        async () => {
                            await this.deliveryEditorCheckoutWizard.getTitleText()
                                .should.eventually.be.equal(
                                    titleText,
                                    `Текст заголовка блока с оформлением заказа должен быть "${titleText}".`
                                );
                        }
                    );

                    await this.browser.allure.runStep(
                        'По дефолту отображается способ доставки "Курьером" с формой ввода адреса.',
                        async () => {
                            await this.deliveryTypes.isCheckedDeliveryTypeDelivery()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Должна отображаться доставка "Курьером."'
                                );

                            await this.courierSuggest.waitForVisible();
                        }
                    );

                    await this.browser.allure.runStep(
                        'Присутствует способ доставки "Самовывоз".',
                        async () => {
                            await this.deliveryTypes.deliveryTypePickupIsExisting()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Способ доставки "Самовывоз" должен отображаться.'
                                );
                        }
                    );

                    await this.browser.allure.runStep(
                        'Способ доставки "Почта" не отображается.',
                        async () => {
                            await this.deliveryTypes.deliveryTypePostIsExisting()
                                .should.eventually.to.be.equal(
                                    false,
                                    'Способ доставки "Почта" не должен отображаться.'
                                );
                        }
                    );

                    await this.browser.allure.runStep(
                        'Кнопка "Продолжить" заблокирована.',
                        async () => {
                            await this.deliveryEditorCheckoutWizard.isSubmitButtonDisabled()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Кнопка "Продолжить" должна быть заблокирована.'
                                );
                        }
                    );
                },
            }),
        },
    },
});
