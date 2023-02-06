import {
    makeSuite,
    prepareSuite,
} from 'ginny';

// pageObjects
import Checkbox from '@self/root/src/uikit/components/Checkbox/__pageObject';
import Link from '@self/root/src/components/Link/__pageObject';

// scenarios
import {setFormData} from '@self/root/src/spec/hermione/scenarios/checkoutLiftingToFloorPopup';

// suites
import PopupStateSuite from './popupStateSuite';
import CheckboxStateSuite from './checkboxStateSuite';

/**
 * Проверяет состояние попапа выбора подъема на этаж
 * PageObjects:
 * this.liftingToFloor - объект с выбором подъема на этаж
 * this.liftingToFloorPopup - попап с выбором подъема на этаж
 */
const popupSuite = makeSuite('Попап подъема на этаж', {
    params: {
        manualLiftPerFloorCost: 'Стоимость подъема вручную',
        elevatorLiftCost: 'Стоимость подъема на лифте',
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                checkbox: () => this.createPageObject(Checkbox, {
                    parent: this.liftingToFloor,
                }),
                editLink: () => this.createPageObject(Link, {
                    parent: this.liftingToFloor,
                }),
            });
        },

        'При клике на чекбокс открывается попап.': prepareSuite(PopupStateSuite, {
            params: {
                isExist: true,
            },
            hooks: {
                async beforeEach() {
                    await this.checkbox.toggle();
                },
            },
        }),

        'При клике на выбранный чекбокс снимается выбор.': prepareSuite(CheckboxStateSuite, {
            params: {
                isExist: true,
                liftingType: 'NOT_NEEDED',
                checked: false,
                floor: 2,
            },
            hooks: {
                async beforeEach() {
                    await this.browser.yaScenario(this, setFormData, {
                        liftingType: 'MANUAL',
                        floor: 2,
                        comment: 'Comment MANUAL',
                    });
                    await this.checkbox.toggle();
                },
            },
        }),

        'При клике на редактировать открывает попап с ранее введенными данными.': prepareSuite(PopupStateSuite, {
            params: {
                isExist: true,
                liftingType: 'MANUAL',
                comment: 'Comment MANUAL',
            },
            hooks: {
                async beforeEach() {
                    await this.browser.yaScenario(this, setFormData, {
                        liftingType: 'MANUAL',
                        floor: 2,
                        comment: 'Comment MANUAL',
                    });
                    await this.editLink.click();
                },
            },
        }),
    },
});

export default popupSuite;
