import {makeCase, makeSuite} from 'ginny';

// scenarios
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {
    ACTUALIZATION_TIMEOUT,
    prepareCheckouterPageWithCartsForRepeatOrder,
} from '@self/root/src/spec/hermione/scenarios/checkout';

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
    x5outlet,
} from '@self/root/src/spec/hermione/kadavr-mock/report/outlets';

// pageObjects
import DeliveryInfo from '@self/root/src/components/Checkout/DeliveryInfo/__pageObject/index.touch';
import PickupList
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/components/PickupList/__pageObject/index.touch';
import DeliveryTypeList from '@self/root/src/components/DeliveryTypes/__pageObject';
import SubpageHeader from '@self/root/src/components/Checkout/CheckoutSubpageHeader/__pageObject';
import OutletAddress from '@self/root/src/containers/Outlet/OutletAddressContainer/__pageObject';
import PlacemarkMap from '@self/root/src/widgets/content/checkout/common/CheckoutTouchSimpleDeliveryEditor/components/PlacemarkMap/__pageObject';

import {ADDRESSES} from '../constants';

const DSBS_SAGGESTS_COUNT = 3;
const PVZ_COUNT = 4;
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
                    {id: x5outlet.id, regionId: 0},
                    {id: sdek.id, regionId: 0},
                    {id: boxberry.id, regionId: 0},
                    {id: pickpoint1.id, regionId: 0},
                ],
            },
        ],
        outlets: [
            x5outlet,
            sdek,
            boxberry,
            pickpoint1,
        ],
    }),
];

export default makeSuite('Выбор ПВЗ для DSBS товара.', {
    feature: 'Способ доставки "Самовывоз".',
    defaultParams: {
        region: region['Москва'],
        isAuthwithPlugin: true,
    },
    environment: 'kadavr',
    story: {
        async beforeEach() {
            this.setPageObjects({
                deliveryInfo: () => this.createPageObject(DeliveryInfo, {
                    parent: this.confirmationPage,
                }),
                popupDeliveryTypes: () => this.createPageObject(DeliveryTypeList, {
                    parent: this.editPopup,
                }),
                pickupList: () => this.createPageObject(PickupList, {
                    parent: this.editPopup,
                }),
                placemarkMap: () => this.createPageObject(PlacemarkMap, {
                    parent: this.deliveryEditorCheckoutWizard,
                }),
                subpageHeader: () => this.createPageObject(SubpageHeader),
                outletAddress: () => this.createPageObject(OutletAddress, {
                    parent: this.deliveryInfo,
                }),
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
                        await this.deliveryInfo.click();
                    }
                );

                await this.browser.allure.runStep(
                    'Открывается попап "Способ доставки"',
                    async () => {
                        await this.editPopup.waitForVisibleRoot();
                        await this.popupDeliveryTypes.waitForVisible();

                        await this.browser.allure.runStep(
                            'Текст заголовока попапа "Мои способы доставки".',
                            async () => {
                                await this.editPopup.getTitleText()
                                    .should.eventually.to.be.equal(
                                        'Мои способы доставки',
                                        'Заголовок попапа должен быть "Мои способы доставки".'
                                    );
                            }
                        );
                    }
                );
            },
            'Присутствуют чипсы способов доставки и кнопка "Добавить новый".': makeCase({
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
                },
            }),
            'Отсуствуют пресеты пользователя.': makeCase({
                async test() {
                    await this.browser.allure.runStep(
                        'Отображается подзаголовок "Среди ваших пунктов самовывоза нет подходящих.".',
                        async () => {
                            await this.editPopup.getDrawerSubtitleText()
                                .should.eventually.to.be.include(
                                    'Среди ваших пунктов самовывоза нет подходящих.',
                                    'Должен отображаться подзаголовок "Среди ваших пунктов самовывоза нет подходящих.".'
                                );
                        }
                    );

                    await this.browser.allure.runStep(
                        `Отображается список из ${DSBS_SAGGESTS_COUNT} ПВЗ для DSBS товара.`,
                        async () => {
                            await this.pickupList.getPickupCardsCount()
                                .should.eventually.to.be.equal(
                                    DSBS_SAGGESTS_COUNT,
                                    `Должены отображаться ${DSBS_SAGGESTS_COUNT} саджеста.`
                                );
                        }
                    );

                    await this.browser.allure.runStep(
                        'Под саджестами кнопка "Смотреть все на карте".',
                        async () => {
                            await this.pickupList.isGoToMapButtonVisible()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Должна отображаться кнопка "Смотреть все на карте".'
                                );
                        }
                    );

                    await this.browser.allure.runStep(
                        'Кнопка "Привезти сюда" не активна.',
                        async () => {
                            await this.editPopup.isChooseButtonDisabled()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Кнопка "Привезти сюда" должна быть не активна.'
                                );
                        }
                    );
                },
            }),
            'Выбрать первый ПВЗ из саджестов.': {
                async beforeEach() {
                    await this.browser.allure.runStep(
                        'Присутствуют 3 саджеста ПВЗ партнера.',
                        async () => {
                            await this.browser.allure.runStep(
                                'Отображается список с доступными ПВЗ.',
                                async () => {
                                    await this.pickupList.isRootVisible();
                                }
                            );

                            await this.browser.allure.runStep(
                                `Отображается список из ${DSBS_SAGGESTS_COUNT} ПВЗ для DSBS товара.`,
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
                            await this.pickupList.clickPickupListItemByAddress(x5outlet.address.fullAddress);
                        }
                    );

                    await this.browser.allure.runStep(
                        'Ожидаем пока кнопка "Привезти сюда" станет активной.',
                        async () => {
                            await this.editPopup.waitForChooseButtonEnabled();
                        }
                    );

                    await this.browser.allure.runStep(
                        'Нажать кнопку "Смотреть все на карте".',
                        async () => {
                            await this.pickupList.clickGoToMapButton();
                        }
                    );

                    await this.browser.allure.runStep(
                        'Переход на карту.',
                        async () => {
                            await this.deliveryEditor.waitForVisible();

                            await this.placemarkMap.waitForVisible(2000);
                            await this.placemarkMap.waitForReady(4000);
                        }
                    );
                },
                'На карте присутствуют ПВЗ для DSBS товара.': makeCase({
                    async test() {
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
                                await this.subpageHeader.clickBackButton();

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
                            'Выбрать первый ПВЗ из саджестов.',
                            async () => {
                                await this.browser.allure.runStep(
                                    'Выбрать первый саджест.',
                                    async () => {
                                        await this.pickupList.clickPickupListItemByAddress(x5outlet.address.fullAddress);
                                    }
                                );

                                await this.browser.allure.runStep(
                                    'Ожидаем пока кнопка "Привезти сюда" станет активной.',
                                    async () => {
                                        await this.editPopup.waitForChooseButtonEnabled();
                                    }
                                );
                            }
                        );

                        await this.browser.allure.runStep(
                            'Нажать кнопку "Привезти сюда".',
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
                                        if (await this.preloader.waitForVisible(1000)) {
                                            await this.preloader.waitForHidden(ACTUALIZATION_TIMEOUT);
                                        }

                                        await this.outletAddress.getText()
                                            .should.eventually.to.be.equal(
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
