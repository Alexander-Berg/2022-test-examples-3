import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты вкладок на попапе доставки на странице КМ
 * @param {PageObject.Delivery} delivery
 * @param {PageObject.DeliveryPopup} deliveryPopup
 */
export default makeSuite('Вкладка "Самовывоз" или "Доставка"', {
    params: {
        popupTabId: 'ID вкладки в окне доставки',
    },
    story: {
        'По умолчанию': {
            'отсутствует.': makeCase({
                async test() {
                    const {popupTabId} = this.params;

                    await this.delivery.waitForVisible();
                    await this.delivery.clickInfo();

                    await this.deliveryPopup.waitForVisible();

                    return this
                        .deliveryPopupTabs.isTabExisting(popupTabId)
                        .should.eventually.be.equal(false, `Вкладки ${popupTabId} нет.`);
                },
            }),
        },
    },
});
