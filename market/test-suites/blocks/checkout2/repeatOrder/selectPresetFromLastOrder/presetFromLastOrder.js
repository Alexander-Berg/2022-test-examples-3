import {makeCase, makeSuite} from 'ginny';

import {prepareCheckoutPage} from '@self/root/src/spec/hermione/scenarios/checkout';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {region} from '@self/root/src/spec/hermione/configs/geo';

import {DELIVERY_TYPES} from '@self/root/src/constants/delivery';
import {ORDER_STATUS} from '@self/root/src/entities/order';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

function makeDeliveredOrder() {
    let orderIndex = 0;
    return async delivery => {
        const deliveryType = this.params.deliveryType;

        await this.browser.setState(`persAddress.address.${delivery.id}`, delivery);
        await this.browser.setState(`Checkouter.collections.order.${orderIndex++}`, {
            id: orderIndex,
            status: ORDER_STATUS.DELIVERED,
            region: region['Москва'],
            delivery: {
                regionId: delivery.regionId,
                buyerAddress: delivery,
                type: deliveryType,
            },
        });
    };
}

export default makeSuite('', {
    params: {
        region: 'Регион',
        deliveryType: 'Тип доставки',
        carts: 'Корзина',
        delivery: 'Адреса доставки',
    },
    defaultParams: {
        region: region['Москва'],
        isAuthWithPlugin: true,
        deliveryType: DELIVERY_TYPES.DELIVERY,
    },
    environment: 'kadavr',
    story: {
        async beforeEach() {
            const addDeliveredOrderWithAddresses = makeDeliveredOrder.call(this);
            this.browser.allure.runStep('Плагин Auth: логин', async () => {
                const retpathPageId = PAGE_IDS_COMMON.ORDER_CONDITIONS;
                const retpathParams = {
                    lr: this.params.region,
                };

                const fullRetpath = await this.browser.yaBuildFullUrl(retpathPageId, retpathParams);
                return this.browser.yaMdaTestLogin(null, null, fullRetpath);
            });
            const testState = await this.browser.yaScenario(
                this,
                prepareMultiCartState,
                this.params.carts
            );

            await addDeliveredOrderWithAddresses(this.params.delivery[0]);
            await addDeliveredOrderWithAddresses(this.params.delivery[1]);
            await this.browser.yaScenario(
                this,
                prepareCheckoutPage,
                {
                    region: this.params.region,
                    checkout2: true,
                    items: testState.checkoutItems,
                    reportSkus: testState.reportSkus,
                }
            );
        },
        async afterEach() {
            await this.browser.yaLogout();
        },
        ['При повторном заказе обычного товара пользователь может выбрать пресет адреса,' +
        ' который использовался для одного из предыдущих заказов.']: makeCase({
            async test() {
                const delivery = this.params.delivery;
                const fullFirstAddress = (this.params.deliveryType === DELIVERY_TYPES.POST) ?
                    `${delivery[0].zip}, ${delivery[0].address}`
                    : delivery[0].address;

                const fullLastAddress = (this.params.deliveryType === DELIVERY_TYPES.POST) ?
                    `${delivery[1].zip}, ${delivery[1].address}`
                    : delivery[1].address;

                await this.allure.runStep(
                    'Перейти в чекаут.', async () => {
                        await this.confirmationPage.waitForVisible();
                        await this.deliveryInfo.waitForVisible();
                    }
                );

                await this.allure.runStep(
                    'В данные о доставке заказа подставился последний использованный пресет в данном регионе.',
                    async () => {
                        const lastAddress = delivery[1].address;

                        await this.addressCard.getText()
                            .should.eventually.to.be.equal(
                                lastAddress,
                                `Текст в поле адрес должен быть "${lastAddress}".`
                            );
                    }
                );

                await this.allure.runStep(
                    'Переход к попапу со списком пресетов по нажатию на кнопку "Изменить" в блоке заказа.', async () => {
                        await this.addressEditableCard.isChangeButtonDisabled()
                            .should.eventually.to.be.equal(
                                false,
                                'На карточке блока доставки должна отображатся кнопка "Изменить" и быть активной.'
                            );

                        await this.addressEditableCard.changeButtonClick();
                        await this.editPopup.waitForVisibleRoot();

                        if (this.params.deliveryTypes === DELIVERY_TYPES.DELIVERY) {
                            await this.allure.runStep(
                                'По умолчанию выбран способ доставки "Курьером".', async () => {
                                    await this.popupDeliveryTypes.isCheckedDeliveryTypeDelivery()
                                        .should.eventually.to.be.equal(true, 'Должна отображаться доставка "Курьером."');
                                }
                            );
                        }

                        if (this.params.deliveryTypes === DELIVERY_TYPES.POST) {
                            await this.allure.runStep(
                                'По умолчанию выбран способ доставки "Почтой".', async () => {
                                    await this.popupDeliveryTypes.isCheckedDeliveryTypePost()
                                        .should.eventually.to.be.equal(true, 'Должна отображаться доставка "Почтой."');
                                }
                            );
                        }

                        await this.allure.runStep(
                            'В списке адресов должны присутствовать адреса из предыдущих заказов.', async () => {
                                await this.allure.runStep(
                                    'Активным должен быть адрес из последнего заказа.', async () => {
                                        await this.addressList.getActiveItemText()
                                            .should.eventually.include(
                                                fullLastAddress,
                                                `Текст карточки должен быть "${fullLastAddress}".`
                                            );
                                    }
                                );

                                await this.allure.runStep(
                                    'Должен присутствовать адрес из первого заказа.', async () => {
                                        await this.addressList.isCardWithAddressExisting(fullFirstAddress)
                                            .should.eventually.to.be.equal(
                                                true,
                                                `Карточка с адресом "${fullFirstAddress}" должна быть видна.`
                                            );
                                    }
                                );
                            }
                        );
                    }
                );

                await this.allure.runStep(
                    'Выбрать пресет из первого заказа.', async () => {
                        await this.addressList.clickItemByAddress(fullFirstAddress);
                        await this.allure.runStep(
                            'Активным должен стать пресет из первого заказа.', async () => {
                                await this.addressList.getActiveItemText()
                                    .should.eventually.include(
                                        fullFirstAddress,
                                        `Текст карточки должен быть "${fullFirstAddress}".`
                                    );
                            }
                        );
                    }
                );

                await this.allure.runStep(
                    'Нажать на кнопку "Выбрать".', async () => {
                        const firstAddress = delivery[0].address;
                        await this.editPopup.waitForChooseButtonEnabled();
                        await this.editPopup.chooseButtonClick();

                        await this.deliveryInfo.waitForVisible();

                        await this.allure.runStep(
                            'В поле данных о доставке указан выбранный адрес.', () =>
                                this.addressCard.getText()
                                    .should.eventually.to.be.equal(
                                        firstAddress,
                                        `Текст в поле адрес должен быть "${firstAddress}".`
                                    )
                        );
                    }
                );
            },
        }),
    },
});
