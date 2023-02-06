import {
    makeSuite,
    makeCase,
} from 'ginny';

// pageObjects
import NotAvailableText from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/LiftingToFloor/NotAvailableText/__pageObject';
import Checkbox from '@self/root/src/uikit/components/Checkbox/__pageObject';

import {getCheckboxText, getPrice} from './helpers';

/**
 * Проверяет состояние чекбокса выбора подъема на этаж
 * PageObjects:
 * this.deliveryInfo - информация о доставке
 * this.liftingToFloor - объект с выбором подъема на этаж
 */
const checkboxStateSuite = makeSuite('Состояние чекбокса', {
    params: {
        manualLiftPerFloorCost: 'Стоимость подъема вручную',
        elevatorLiftCost: 'Стоимость подъема на лифте',
        liftingType: 'Тип подъема на этаж',
        floor: 'Выбранный этаж',
        comment: 'Написанный комент',
        checked: 'Выбран ли чекбокс',
        isExist: 'Показывается ли чекбокс',
        liftingAvailability: 'Доступность подъема на этаж',
    },
    defaultParams: {
        liftingType: 'NOT_NEEDED',
        isExist: true,
        checked: false,
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                checkbox: () => this.createPageObject(Checkbox, {
                    parent: this.liftingToFloor,
                }),
                notAvailableText: () => this.createPageObject(NotAvailableText, {
                    parent: this.deliveryInfo,
                }),
            });
        },
        'не показываем': makeCase({
            async test() {
                const {
                    isExist,
                    liftingAvailability,
                } = this.params;

                if (isExist) {
                    // eslint-disable-next-line market/ginny/no-skip
                    return this.skip('игнорируем тест если чекбокс должен быть показан');
                }

                this.liftingToFloor.isExisting()
                    .should.eventually.to.be.equal(
                        false,
                        'Чекбокс подъема на этаж не должен быть виден'
                    );

                if (!liftingAvailability || liftingAvailability === 'AVAILABLE') {
                    this.notAvailableText.isExisting()
                        .should.eventually.to.be.equal(
                            false,
                            'Не должно быть текста о недоступности или бесплатности подъема'
                        );
                } else {
                    const isNotAvailable = liftingAvailability === 'NOT_AVAILABLE';

                    this.notAvailableText.isExisting()
                        .should.eventually.to.be.equal(
                            true,
                            'Должен быть показан текст о недоступности или бесплатности подъема'
                        );

                    this.notAvailableText.getText()
                        .should.eventually.to.contain(
                            isNotAvailable
                                ? 'подъем на этаж недоступен'
                                : 'бесплатно',
                            `Должен быть показан текст о ${isNotAvailable ? 'недоступности' : 'бесплатности'} подъема`
                        );
                }
            },
        }),
        'показываем с ожидаемыми данными': makeCase({
            async test() {
                const {
                    liftingType,
                    floor,
                    comment,
                    checked,
                    isExist,
                } = this.params;

                if (!isExist) {
                    // eslint-disable-next-line market/ginny/no-skip
                    return this.skip('игнорируем тест если чекбокс не должен быть показан');
                }

                await this.liftingToFloor.isExisting()
                    .should.eventually.to.be.equal(
                        true,
                        'Чекбокс подъема на этаж должен быть виден'
                    );

                if (floor && liftingType !== 'NOT_NEEDED') {
                    await this.liftingToFloor.getSubtext()
                        .should.eventually.to.contain(
                            floor,
                            'Если задан этаж, дополнительный текст должен содержать его'
                        );
                }

                if (comment && liftingType !== 'NOT_NEEDED') {
                    await this.liftingToFloor.getSubtext()
                        .should.eventually.to.contain(
                            comment,
                            'Если задан коментарий, дополнительный текст должен содержать его'
                        );
                }

                await this.checkbox.isChecked()
                    .should.eventually.to.be.equal(
                        checked,
                        `Чекбокс должен быть${checked ? '' : ' не'} выбран`
                    );

                await this.liftingToFloor.getCheckboxText()
                    .should.eventually.to.contain(
                        getCheckboxText(liftingType, floor),
                        `Текст чекбокса должен содержать ${getCheckboxText(liftingType, floor)}`
                    );

                const price = getPrice(this.params);

                if (price && liftingType !== 'NOT_NEEDED') {
                    await this.liftingToFloor.getPriceValue()
                        .should.eventually.to.be.equal(
                            price,
                            'Если задана цена, то она должна быть в тексте чекбокса'
                        );
                }
            },
        }),
    },
});

export default checkboxStateSuite;
