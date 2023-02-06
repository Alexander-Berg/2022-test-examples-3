import {makeCase, makeSuite} from 'ginny';

// scenarios
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {prepareCheckouterPageWithCartsForRepeatOrder} from '@self/root/src/spec/hermione/scenarios/checkout';

// mocks

import * as dsbs from '@self/root/src/spec/hermione/kadavr-mock/report/dsbs';
import {region} from '@self/root/src/spec/hermione/configs/geo';
import {
    deliveryOptionsMock,
    deliveryPickupMock,
} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';
import {
    sdek,
    boxberry,
    pickpoint1,
    pickpoint2,
    x5outlet,
} from '@self/root/src/spec/hermione/kadavr-mock/report/outlets';

// pageObjects
import AddressList from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/components/AddressList/__pageObject';
import PickupList from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/components/PickupList/__pageObject';
import EditPopup from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/__pageObject';
import EditAddressPopup from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditAddressPopup/__pageObject';
import PopupBase from '@self/root/src/components/PopupBase/__pageObject';
import DeliveryTypeList from '@self/root/src/components/DeliveryTypes/__pageObject';
import InformationPanel from '@self/root/src/components/InformationPanel/__pageObject';
import PlacemarkMap
    from '@self/root/src/widgets/content/checkout/common/CheckoutVectorPlacemarkMap/components/VectorPlacemarkMap/__pageObject';
// eslint-disable-next-line max-len
import DeliveryActionButton from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/DeliveryActionButton/__pageObject';

import {ADDRESSES} from '../../constants';

const DSBS_SAGGESTS_COUNT = 4;
const PVZ_COUNT = 5;
const carts = [
    buildCheckouterBucket({
        items: [{
            skuMock: dsbs.skuPhoneMock,
            offerMock: dsbs.offerPhoneMock,
            count: 1,
        }],
        deliveryOptions: [
            ...deliveryOptionsMock,
            {
                ...deliveryPickupMock,
                outlets: [
                    {id: x5outlet.id, regionId: region['Москва']},
                    {id: sdek.id, regionId: region['Москва']},
                    {id: boxberry.id, regionId: region['Москва']},
                    {id: pickpoint1.id, regionId: region['Москва']},
                    {id: pickpoint2.id, regionId: region['Москва']},
                ],
            },
        ],
        outlets: [
            x5outlet,
            sdek,
            boxberry,
            pickpoint1,
            pickpoint2,
        ],
    }),
];

export default makeSuite('Выбор ПВЗ для DSBS товара.', {
    feature: 'Способ доставки "Самовывоз".',
    id: 'marketfront-5093',
    issue: 'MARKETFRONT-45598',
    defaultParams: {
        region: region['Москва'],
        isAuthwithPlugin: true,
    },
    environment: 'kadavr',
    story: {
        async beforeEach() {
            this.setPageObjects({
                popupBase: () => this.createPageObject(PopupBase),
                editPopup: () => this.createPageObject(EditPopup),
                editAddressPopup: () => this.createPageObject(EditAddressPopup, {
                    parent: this.popupBase,
                }),
                popupDeliveryTypes: () => this.createPageObject(DeliveryTypeList, {
                    parent: this.editPopup,
                }),
                addressList: () => this.createPageObject(AddressList, {
                    parent: this.editPopup,
                }),
                pickupList: () => this.createPageObject(PickupList, {
                    parent: this.editPopup,
                }),
                pickupCard: () => this.createPageObject(InformationPanel, {
                    parent: this.deliveryInfo,
                }),
                placemarkMap: () => this.createPageObject(PlacemarkMap, {
                    parent: this.deliveryEditorCheckoutWizard,
                }),
                deliveryActionButton: () => this.createPageObject(DeliveryActionButton),
            });
            await this.browser.setState(`persAddress.address.${ADDRESSES.MOSCOW_ADDRESS.id}`, ADDRESSES.MOSCOW_ADDRESS);
            await this.browser.yaScenario(
                this,
                prepareCheckouterPageWithCartsForRepeatOrder,
                {
                    carts: carts,
                    options: {
                        region: this.params.region,
                        checkout2: true,
                    },
                }
            );
        },
        'Открыть главную страницу чекаута.': {
            async beforeEach() {
                await this.browser.allure.runStep(
                    'Отображается главный экран чекаута.',
                    async () => {
                        await this.confirmationPage.waitForVisible();
                    }
                );

                await this.browser.allure.runStep(
                    'Нажать на кнопку "Выбрать пункт выдачи" в блоке заказа.',
                    async () => {
                        await this.deliveryActionButton.isButtonVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'В блоке доставки должна отображатся кнопка "Выбрать пункт выдачи".'
                            );

                        await this.deliveryActionButton.click();
                    }
                );

                await this.browser.allure.runStep(
                    'Открывается попап "Способ доставки"',
                    async () => {
                        await this.editPopup.waitForVisibleRoot();
                        await this.popupDeliveryTypes.waitForVisible();
                    }
                );
            },
            'Присутствуют чипсы способов доставки "Самовывоз", "Курьер".': makeCase({
                async test() {
                    await this.browser.allure.runStep(
                        'Выбран способ доставки "Самовывоз".',
                        async () => {
                            await this.popupDeliveryTypes.isCheckedDeliveryTypePickup()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Должен быть выбран способ доставки "Самовывоз".'
                                );
                        }
                    );

                    await this.browser.allure.runStep(
                        'Присутствует чипс "Курьер".',
                        async () => {
                            await this.popupDeliveryTypes.deliveryTypeDeliveryIsExisting()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Должен отображаться способ доставки "Курьер".'
                                );
                        }
                    );
                },
            }),
            'Отображаются кнопки "Выбрать", "Добавить новый" и "Смотреть все на карте".': makeCase({
                async test() {
                    await this.browser.allure.runStep(
                        'Отображается кнопка "Выбрать".',
                        async () => {
                            await this.editPopup.isChooseButtonVisible()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Должна отображаться кнопка "Выбрать".'
                                );
                        }
                    );

                    await this.browser.allure.runStep(
                        'Отображается кнопка "Добавить новый".',
                        async () => {
                            await this.editPopup.isAddButtonVisible()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Должна отображаться кнопка "Добавить новый".'
                                );
                        }
                    );

                    await this.browser.allure.runStep(
                        'Отображается кнопка "Смотреть все на карте".',
                        async () => {
                            await this.editPopup.isGoToMapButtonVisible()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Должна отображаться кнопка "Смотреть все на карте".'
                                );
                        }
                    );
                },
            }),
            'Выбрать первый ПВЗ из саджестов.': {
                async beforeEach() {
                    await this.browser.allure.runStep(
                        'Присутствуют 4 саджеста ПВЗ партнера.',
                        async () => {
                            await this.browser.allure.runStep(
                                'Отображается список с доступными ПВЗ.',
                                async () => {
                                    await this.pickupList.isRootVisible();
                                }
                            );

                            await this.browser.allure.runStep(
                                `Отображается ${DSBS_SAGGESTS_COUNT} ПВЗ партнера.`,
                                async () => {
                                    await this.pickupList.getPickupCardsCount()
                                        .should.eventually.to.be.equal(
                                            DSBS_SAGGESTS_COUNT,
                                            `Должены отображаться ${DSBS_SAGGESTS_COUNT} саджеста.`
                                        );
                                }
                            );
                        }
                    );

                    await this.browser.allure.runStep(
                        'Выбрать первый саджест.',
                        async () => {
                            await this.pickupList.clickItemByAddress(x5outlet.address.fullAddress);
                        }
                    );

                    await this.browser.allure.runStep(
                        'Ожидаем пока кнопка "Выбрать" станет активной.',
                        async () => {
                            await this.editPopup.waitForChooseButtonEnabled();
                        }
                    );

                    await this.browser.allure.runStep(
                        'Нажать кнопку "Смотреть все на карте".',
                        async () => {
                            // скроллим чтобы срабатывал клик
                            const selector = await this.editPopup.getSelector(EditPopup.content);
                            await this.browser.yaSetElemScrollToBottom(selector);

                            await this.editPopup.goToMapButtonClick();
                        }
                    );

                    await this.browser.allure.runStep(
                        'Переход на карту.',
                        async () => {
                            await this.deliveryEditorCheckoutWizard.waitForVisible(5000);

                            await this.placemarkMap.waitForVisible(5000);
                            await this.placemarkMap.waitForReady(8000);
                        }
                    );
                },
                'Отображается способ доставки "Самовывоз", на карте отображаются доступные ПВЗ.': makeCase({
                    async test() {
                        await this.allure.runStep(
                            'Выбран способ доставки "Самовывоз".', async () => {
                                await this.deliveryTypes.waitForVisibleDeliveryTypePickup(5000);
                                await this.deliveryTypes.isCheckedDeliveryTypePickup()
                                    .should.eventually.to.be.equal(
                                        true,
                                        'Должен быть выбран способ доставки "Самовывоз".'
                                    );
                            }
                        );

                        await this.allure.runStep(
                            'На карте присутствуют ПВЗ для DSBS товара.', async () => {
                                await this.placemarkMap.getPlacemarkCount()
                                    .should.eventually.to.be.equal(
                                        PVZ_COUNT,
                                        `Должены отображаться ${PVZ_COUNT} ПВЗ.`
                                    );
                            }
                        );
                    },
                }),
                'Возврат к попапу выбора способа доставки по нажатию кнопки "Назад".': makeCase({
                    async test() {
                        await this.browser.allure.runStep(
                            'Нажать кнопку "Назад".',
                            async () => {
                                await this.deliveryEditorCheckoutWizard.backButtonClick();

                                await this.browser.allure.runStep(
                                    'Возврат к попапу выбора способа доставки',
                                    async () => {
                                        await this.editPopup.waitForVisibleRoot();
                                        await this.popupDeliveryTypes.waitForVisible();
                                    }
                                );
                            }
                        );

                        await this.browser.allure.runStep(
                            'Выбран первый ПВЗ из саджестов.',
                            async () => {
                                await this.pickupList.getActiveItemText()
                                    .should.eventually.to.be.equal(
                                        x5outlet.address.fullAddress,
                                        `Должен отображаться активным пресет с адресом ${x5outlet.address.fullAddress}.`
                                    );

                                await this.browser.allure.runStep(
                                    'Ожидаем пока кнопка "Выбрать" станет активной.',
                                    async () => {
                                        await this.editPopup.waitForChooseButtonEnabled();
                                    }
                                );
                            }
                        );

                        await this.browser.allure.runStep(
                            'Нажать кнопку "Выбрать".',
                            async () => {
                                await this.editPopup.chooseButtonClick();

                                await this.browser.allure.runStep(
                                    'Попап "Способ доставки" закрывается.',
                                    async () => {
                                        await this.editPopup.waitForRootInvisible();
                                    }
                                );

                                await this.browser.allure.runStep(
                                    'В блоке доставки отображается выбранный ПВЗ.',
                                    async () => {
                                        await this.pickupCard.getContentText()
                                            .should.eventually.to.be.include(
                                                x5outlet.address.fullAddress,
                                                'Текст в блоке адреса должен соответствовать адресу выбранного ПВЗ.'
                                            );
                                    }
                                );
                            }
                        );
                    },
                }),
            },
        },
    },
});
