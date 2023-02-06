import {makeSuite, makeCase} from 'ginny';

import {setReportState} from '@self/root/src/spec/hermione/scenarios/kadavr';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {createDeliveryGroup} from '@yandex-market/kadavr/mocks/Report/helpers';

import PickupItem from '@self/root/src/components/deliveryCalculator/tariffs/Items/Pickup/__pageObject';
import DeliveryItem from '@self/root/src/components/deliveryCalculator/tariffs/Items/Delivery/__pageObject';
import PostItem from '@self/root/src/components/deliveryCalculator/tariffs/Items/Post/__pageObject';
import CargoDeliveryItem from '@self/root/src/components/deliveryCalculator/tariffs/Items/CargoDelivery/__pageObject';
import FreeDeliveryItem from '@self/root/src/components/deliveryCalculator/tariffs/Items/FreeDelivery/__pageObject';
import TariffsNotFoundForRegion from
    '@self/root/src/components/deliveryCalculator/tariffs/TariffsNotFoundForRegion/__pageObject';

import {region} from '@self/root/src/spec/hermione/configs/geo';
import {onlyDeliveryDeliveryGroup, onlyPickupDeliveryGroup} from '@self/root/src/spec/hermione/kadavr-mock/report/deliveryGroup';
import orderConditionsPageCmsMarkup
    from '@self/root/src/spec/hermione/kadavr-mock/tarantino/orderConditions/orderConditionsPageCmsMarkup';

export default makeSuite('Калькулятор доставки.', {
    feature: 'Отображение блока "Калькулятор доставки"',
    id: 'bluemarket-2832',
    issue: 'BLUEMARKET-6972',
    defaultParams: {
        region: region['Москва'],
    },
    story: {
        beforeEach() {
            this.setPageObjects({
                pickupItem: () => this.createPageObject(PickupItem, {parent: this.deliveryCalculator}),
                deliveryItem: () => this.createPageObject(DeliveryItem, {parent: this.deliveryCalculator}),
                postItem: () => this.createPageObject(PostItem, {parent: this.deliveryCalculator}),
                cargoDeliveryItem: () => this.createPageObject(
                    CargoDeliveryItem,
                    {parent: this.deliveryCalculator}
                ),
                freeDeliveryItem: () => this.createPageObject(
                    FreeDeliveryItem,
                    {parent: this.deliveryCalculator}
                ),
                tariffsNotFoundForRegion: () => this.createPageObject(
                    TariffsNotFoundForRegion,
                    {parent: this.deliveryCalculator}
                ),
            });
        },

        'По умолчанию': {
            async beforeEach() {
                if (this.getMeta('environment') === 'kadavr') {
                    await this.browser.setState(
                        'Tarantino.data.result',
                        [orderConditionsPageCmsMarkup]
                    );
                }

                await this.browser.yaOpenPage(PAGE_IDS_COMMON.ORDER_CONDITIONS);
            },

            'виджет с калькулятором отображается и имеет заголовок “Доставляем по России”': makeCase({
                async test() {
                    await this.deliveryCalculator.isVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Проверяем, что виджет калькулятора доставки отображается на странице'
                        );

                    const title = 'Доставляем по России';
                    await this.expect(this.deliveryCalculator.getTitle())
                        .to.be.equal(title, `Текст заголовка должен быть "${title}"`);
                },
            }),

            'отображается надпись “Точные сроки и стоимость доставки будут известны при оформлении заказа”': makeCase({
                async test() {
                    await this.deliveryCalculator.disclaimer.isVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Проверяем, что сноска видна на странице'
                        );

                    const text = 'Точные сроки и стоимость доставки будут известны при оформлении заказа';
                    await this.expect(this.deliveryCalculator.getDisclaimerText())
                        .to.be.equal(text,
                            `Текст сноски должен быть "${text}"`
                        );
                },
            }),

            'выбран текущий регион - Москва': makeCase({
                async test() {
                    const regionName = 'Москва';
                    await this.expect(this.deliveryCalculator.getSuggestInputValue())
                        .to.be.equal(regionName, `Должен быть выбран регион "${regionName}"`);
                },
            }),
        },

        'Все возможные блоки информации о доставке.': makeSuite('Интеграционные тесты.', {
            environment: 'testing',
            story: {
                async beforeEach() {
                    await this.browser.yaOpenPage(PAGE_IDS_COMMON.ORDER_CONDITIONS);

                    const regionName = 'Москва';
                    await this.expect(this.deliveryCalculator.getSuggestInputValue())
                        .to.be.equal(regionName, `Должен быть выбран регион "${regionName}"`);
                },

                'Доставка курьером': makeCase({
                    async test() {
                        const deliveryTypeDescriptionRegExp = '^(\\d+)(-(\\d+))* (день|дня|дней), (\\d+) ₽$';

                        await this.deliveryItem.isVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'Проверяем, что вариант доставки курьером отображается на странице'
                            );

                        const titleDelivery = 'Доставка курьером';
                        await this.deliveryItem.getTitle()
                            .should.eventually.to.be.equal(titleDelivery, `Текст заголовка должен быть "${titleDelivery}"`);

                        await this.deliveryItem.getDescription()
                            .should.eventually.to.be.match(
                                new RegExp(deliveryTypeDescriptionRegExp),
                                `Описание варианта доставки курьером
                            должно соответсвовать шаблону ${deliveryTypeDescriptionRegExp}`
                            );
                    },
                }),

                'Самовывоз': makeCase({
                    async test() {
                        const deliveryTypeDescriptionRegExp = '^(\\d+)(-(\\d+))* (день|дня|дней), (\\d+) ₽$';

                        await this.pickupItem.isVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'Проверяем, что вариант доставки "Самовывоз" отображается на странице'
                            );

                        const titlePickup = 'Самовывоз';
                        await this.pickupItem.getTitle()
                            .should.eventually.to.be.equal(titlePickup, `Текст заголовка должен быть "${titlePickup}"`);

                        await this.pickupItem.getDescription()
                            .should.eventually.to.be.match(
                                new RegExp(deliveryTypeDescriptionRegExp),
                                `Описание варианта доставки "Самовывоз"
                                должно соответсвовать шаблону ${deliveryTypeDescriptionRegExp}`
                            );
                    },
                }),

                'Почта': makeCase({
                    async test() {
                        const deliveryTypeDescriptionRegExp = '^(\\d+)(-(\\d+))* (день|дня|дней), (\\d+) ₽$';

                        await this.postItem.isVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'Проверяем, что вариант доставки почтой отображается на странице'
                            );

                        const titlePost = 'Доставка почтой';
                        await this.postItem.getTitle()
                            .should.eventually.to.be.equal(titlePost, `Текст заголовка должен быть "${titlePost}"`);

                        await this.postItem.getDescription()
                            .should.eventually.to.be.match(
                                new RegExp(deliveryTypeDescriptionRegExp),
                                `Описание варианта доставки почтой
                                должно соответсвовать шаблону ${deliveryTypeDescriptionRegExp}`
                            );
                    },
                }),

                'Крупногабаритный заказ': makeCase({
                    async test() {
                        const deliveryTypeDescriptionRegExp = '^(\\d+)(-(\\d+))* (день|дня|дней), (\\d+) ₽$';
                        const cargoDeliveryTypeTitleRegExp = '^Крупногабаритный заказ \\(от (\\d+) кг\\)$';

                        await this.cargoDeliveryItem.isVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'Проверяем, что условия доставки КГЗ отображаются на странице'
                            );

                        await this.cargoDeliveryItem.getTitle()
                            .should.eventually.to.be.match(
                                new RegExp(cargoDeliveryTypeTitleRegExp),
                                `Текст заголовка должен соответсвовать шаблону "${cargoDeliveryTypeTitleRegExp}"`
                            );

                        await this.cargoDeliveryItem.getDescription()
                            .should.eventually.to.be.match(
                                new RegExp(deliveryTypeDescriptionRegExp),
                                `Описание информации о крупногабаритном заказе
                                должно соответсвовать шаблону ${deliveryTypeDescriptionRegExp}`
                            );
                    },
                }),

                'Бесплатная доставка': makeCase({
                    async test() {
                        await this.freeDeliveryItem.isVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'Проверяем, что условие бесплатной доставки отображается на странице'
                            );

                        const titleFreeDelivery = 'Бесплатная доставка';
                        await this.freeDeliveryItem.getTitle()
                            .should.eventually.to.be.equal(
                                titleFreeDelivery,
                                `Текст заголовка должен быть "${titleFreeDelivery}"`
                            );

                        const freeDeliveryDescriptionRegExp = '^при заказе от ([\\d]+ )?[\\d]+ ₽$';
                        await this.freeDeliveryItem.getDescription()
                            .should.eventually.to.be.match(
                                new RegExp(freeDeliveryDescriptionRegExp),
                                `Описание информации о бесплатной доставке
                            должно соответсвовать шаблону ${freeDeliveryDescriptionRegExp}`
                            );
                    },
                }),
            },
        }),

        'Когда доставки в регион нет': {
            async beforeEach() {
                if (this.getMeta('environment') === 'kadavr') {
                    await this.browser.setState(
                        'Tarantino.data.result',
                        [orderConditionsPageCmsMarkup]
                    );
                    await this.browser.yaScenario(this, setReportState, {
                        state: {},
                    });
                }

                await this.browser.yaOpenPage(PAGE_IDS_COMMON.ORDER_CONDITIONS);
            },

            'не отображаются блоки с информацией о доставке, выведено сообщение о том, что доставки нет': makeCase({
                async test() {
                    await this.pickupItem.isVisible()
                        .should.eventually.to.be.equal(
                            false,
                            'Проверяем, что вариант доставки "Самовывоз" не отображается на странице'
                        );

                    await this.deliveryItem.isVisible()
                        .should.eventually.to.be.equal(
                            false,
                            'Проверяем, что вариант доставки курьером не отображается на странице'
                        );

                    await this.postItem.isVisible()
                        .should.eventually.to.be.equal(
                            false,
                            'Проверяем, что вариант доставки почтой не отображается на странице'
                        );

                    await this.cargoDeliveryItem.isVisible()
                        .should.eventually.to.be.equal(
                            false,
                            'Проверяем, что условия доставки КГЗ не отображаются на странице'
                        );

                    await this.freeDeliveryItem.isVisible()
                        .should.eventually.to.be.equal(
                            false,
                            'Проверяем, что условие бесплатной доставки не отображается на странице'
                        );

                    await this.tariffsNotFoundForRegion.isVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Проверяем, что на странице есть сообщение о том, что регион не входит в зону доставки'
                        );

                    const text = 'К сожалению, Москва пока не входит в зону доставки';
                    await this.expect(this.tariffsNotFoundForRegion.getDescription())
                        .to.be.equal(text, `Текст сообщения должен быть "${text}"`);
                },
            }),
        },

        'При переключении региона': {
            async beforeEach() {
                if (this.getMeta('environment') === 'kadavr') {
                    await this.browser.setState(
                        'Tarantino.data.result',
                        [orderConditionsPageCmsMarkup]
                    );
                    await this.browser.yaScenario(this, setReportState, {
                        state: createDeliveryGroup(onlyDeliveryDeliveryGroup),
                    });
                }

                await this.browser.yaOpenPage(PAGE_IDS_COMMON.ORDER_CONDITIONS);
            },

            'изменяется информация об условиях доставки': makeCase({
                async test() {
                    await this.pickupItem.isVisible()
                        .should.eventually.to.be.equal(
                            false,
                            'Проверяем, что вариант доставки "Самовывоз" не отображается на странице'
                        );

                    await this.deliveryItem.isVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Проверяем, что вариант доставки курьером отображается на странице'
                        );

                    const region1 = 'Москва';
                    await this.expect(this.deliveryCalculator.getSuggestInputValue())
                        .to.be.equal(region1, `Должен быть выбран регион "${region1}"`);

                    // Добавляем в стейт Самовывоз, меняем город и проверяем что он повился, а курьерка исчезла
                    await this.browser.yaScenario(this, setReportState, {
                        state: createDeliveryGroup(onlyPickupDeliveryGroup),
                    });

                    const region2 = 'Санкт-Петербург';
                    await this.deliveryCalculator.setSuggestInputValue(region2);
                    await this.deliveryCalculator.selectSuggestion(region2);
                    await this.expect(this.deliveryCalculator.getSuggestInputValue())
                        .to.be.equal(region2, `Должен быть выбран регион "${region2}"`);

                    await this.pickupItem.isVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Проверяем, что вариант доставки "Самовывоз" отображается на странице'
                        );

                    await this.deliveryItem.isVisible()
                        .should.eventually.to.be.equal(
                            false,
                            'Проверяем, что вариант доставки курьером не отображается на странице'
                        );
                },
            }),
        },
    },
});
