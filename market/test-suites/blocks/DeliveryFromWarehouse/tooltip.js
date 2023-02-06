import {makeSuite, makeCase} from 'ginny';

import TooltipContent from '@self/platform/components/DeliveryInfo/DeliveryTooltipContent/__pageObject';

/**
 * Тесты на блок DeliveryFromWarehouse
 * @param {PageObject.DeliveryFromWarehouse} deliveryFromWarehouse
 */
export default makeSuite('Тултип «Со склада Яндекса».', {
    story: {
        'При наведении на кнопку-иконку «Подробнее»': {
            'тултип отображается и содержит ожидаемый текст': makeCase({
                async test() {
                    const {expectedTooltipHeaderText} = this.params;

                    await this.browser.yaReactPageReload();
                    await this.deliveryFromWarehouse.mouseOverInfoButton();
                    await this.browser.waitForVisible(TooltipContent.root);

                    return this.tooltipContent.getTooltipHeaderText()
                        .should.eventually.to.be.equal(expectedTooltipHeaderText,
                            'текст в тултипе ожидаемый'
                        );
                },
            }),
        },
        'При переходе с клавиатуры на кнопку-иконку «Подробнее»': {
            'тултип отображается и содержит ожидаемый текст': makeCase({
                async test() {
                    const {expectedTooltipHeaderText} = this.params;

                    await this.browser.yaReactPageReload();
                    await this.deliveryFromWarehouse.focusInfoButton();
                    await this.browser.waitForVisible(TooltipContent.root);

                    return this.tooltipContent.getTooltipHeaderText()
                        .should.eventually.to.be.equal(expectedTooltipHeaderText,
                            'текст в тултипе ожидаемый'
                        );
                },
            }),
        },
    },
});
