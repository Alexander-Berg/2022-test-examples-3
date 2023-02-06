import checkoutItemIds from '@self/root/src/spec/hermione/configs/checkout/items';

/**
 * Создание заказа для списка заказов
 * @param {string} status код статуса заказа
 * @returns созданный заказ
 */
module.exports = async function createOrder({
    status = 'UNKNOWN',
    delivery = {
        deliveryPartnerType: 'YANDEX_MARKET',
    },
}) {
    const {browser} = this;
    const order = await browser.yaScenario(
        this,
        'checkoutResource.prepareOrder',
        {
            status,
            region: this.params.region,
            orders: [{
                items: [{skuId: checkoutItemIds.asus.skuId}],
                deliveryType: 'DELIVERY',
                delivery,
            }],
            paymentType: 'PREPAID',
            paymentMethod: 'CASH_ON_DELIVERY',
        }
    );

    return order;
};
