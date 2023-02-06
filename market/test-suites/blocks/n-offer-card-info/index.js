import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты попапа доставки на странице КМ
 * @param {PageObject.Delivery} delivery
 * @param {PageObject.DeliveryPopup} deliveryPopup
 */
export default makeSuite('Блок с информацией о цене и доставке.', {
    params: {
        popupTabId: 'ID вкладки в окне доставки',
        deliveryText: 'Текст в окне доставки',
    },
    story: {
        'Текст соответствует ожидаемому.': makeCase({
            async test() {
                const {popupTabId, deliveryText} = this.params;

                await this.delivery.waitForVisible();
                await this.delivery.clickInfo();

                await this.deliveryPopup.waitForVisible();

                if (popupTabId) {
                    await this.deliveryPopupTabs.selectTabById(popupTabId);
                }

                return this.deliveryPopup.getDeliveryInfoText()
                    .should.eventually.be.equal(deliveryText, 'текст соответствует ожидаемому');
            },
        }),
    },
});
