import {assert} from 'chai';

import {ITestOrderPageData} from '../types/ITestOrderPageData';

import TestOrderHotels from 'helpers/project/account/pages/OrderPage/TestOrderHotels';

export async function testOrderPage(
    orderPage: TestOrderHotels,
    data: ITestOrderPageData,
): Promise<void> {
    const {deferredFullPriceAfterApplyPromo, paymentEndsAtDeferredPayment} =
        data;

    assert.equal(
        deferredFullPriceAfterApplyPromo,
        await orderPage.deferredPayment.partialPrice.totalPrice.getPriceValue(),
        'Цена доплаты на странице бронирования и на странице заказа должна совпадать',
    );

    assert.equal(
        paymentEndsAtDeferredPayment,
        await orderPage.deferredPayment.paymentEndsAt.getText(),
        'Крайняя дата доплаты на странице бронирования и на странице заказа должна совпадать',
    );

    assert.isTrue(
        await orderPage.deferredPayment.nextPaymentLink.isVisible(),
        'Должна отображаться ссылка на доплату',
    );
}
