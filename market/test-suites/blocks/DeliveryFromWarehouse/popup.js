import {makeCase, makeSuite} from 'ginny';
import DeliveryFromWarehousePO from '@self/platform/components/DeliveryFromWarehouse/__pageObject/index';

/**
 * Тесты на блок DeliveryFromWarehouse
 * @param {PageObject.DeliveryFromWarehouse} deliveryFromWarehouse
 */
export default makeSuite('Попап "Со склада Яндекса".', {
    story: {
        'При клике на иконку информации': {
            'попап отображается и содержит ожидаемый заголовок': makeCase({
                async test() {
                    const {expectedPopupHeaderText} = this.params;
                    const popupSelector = DeliveryFromWarehousePO.popupSelector;

                    await this.deliveryFromWarehouse.clickOnIcon();
                    await this.browser.waitForVisible(popupSelector);

                    const popupHeaderText = this.deliveryFromWarehouse.getPopupHeaderText();
                    return this.expect(popupHeaderText).to.be.equal(
                        expectedPopupHeaderText,
                        'текст в попапе ожидаемый'
                    );
                },
            }),
        },
    },
});
