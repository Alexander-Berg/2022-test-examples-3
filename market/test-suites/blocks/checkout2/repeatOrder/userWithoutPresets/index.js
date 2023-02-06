import {makeCase, makeSuite} from 'ginny';

import {prepareCheckouterPageWithCartsForRepeatOrder} from '@self/root/src/spec/hermione/scenarios/checkout';

import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import {region} from '@self/root/src/spec/hermione/configs/geo';

import EditPopup from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/__pageObject';
import DeliveryTypeList from '@self/root/src/components/DeliveryTypes/__pageObject';

export default makeSuite('Пользователь без пресетов.', {
    id: 'marketfront-4629',
    issue: 'MARKETFRONT-45593',
    feature: 'Пользователь без пресетов.',
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
                editPopup: () => this.createPageObject(EditPopup),
                deliveryTypes: () => this.createPageObject(DeliveryTypeList, {
                    parent: this.editPopup,
                }),
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
        'Блок "Доставка"': {
            async beforeEach() {
                await this.deliveryActionButton.waitForVisible();
            },
            'В блоке доставки присутствуют кнопки "Изменить" и "Выбрать адрес доставки"': makeCase({
                async test() {
                    await this.addressEditableCard.isChangeButtonDisabled()
                        .should.eventually.to.be.equal(
                            false,
                            'На карточке блока доставки должна отображатся кнопка "Изменить" и быть активной.'
                        );

                    return this.deliveryActionButton.isButtonVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'На карточке блока доставки должна отображатся кнопка "Выбрать адрес доставки".'
                        );
                },
            }),
            'Нажать на кнопку "Выбрать адрес доставки".': {
                async beforeEach() {
                    await this.deliveryActionButton.isButtonVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'На карточке блока доставки должна отображатся кнопка "Выбрать адрес доставки".'
                        );

                    await this.deliveryActionButton.click();
                },
                'Откроется попап выбора адресов доставки.': {
                    async beforeEach() {
                        await this.editPopup.waitForVisibleRoot();
                        await this.deliveryTypes.waitForVisible();
                    },
                    'Для всех способов доставки нет сохраненных пресетов': {
                        'По умолчанию выбран способ доставки "Курьером".': makeCase({
                            async test() {
                                const emptyTitleText = 'Нет сохраненных адресов';

                                await this.deliveryTypes.isCheckedDeliveryTypeDelivery()
                                    .should.eventually.to.be.equal(true, 'Должна отображаться доставка "Курьером."');

                                await this.editPopup.isAddressEmptyTitleVisible()
                                    .should.eventually.to.be.equal(true, 'Должен отображаться пустой заголовок.');

                                return this.editPopup.getAddressEmptyText()
                                    .should.eventually.to.be.equal(
                                        emptyTitleText,
                                        `Текст заголовка блока доставки должен быть "${emptyTitleText}".`
                                    );
                            },
                        }),
                        'Кликнуть на таб способа доставки "Самовывозом".': makeCase({
                            async test() {
                                const emptyTitleText = 'Нет сохраненных адресов';

                                await this.deliveryTypes.setDeliveryTypePickup();
                                await this.editPopup.isAddressEmptyTitleVisible()
                                    .should.eventually.to.be.equal(true, 'Должен отображаться пустой заголовок.');

                                return this.editPopup.getAddressEmptyText()
                                    .should.eventually.to.be.equal(
                                        emptyTitleText,
                                        `Текст заголовка блока доставки должен быть "${emptyTitleText}".`
                                    );
                            },
                        }),
                    },
                },
            },
        },
    },
});
