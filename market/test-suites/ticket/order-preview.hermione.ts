import 'hermione';
import {expect} from 'chai';

import {login} from '../../helpers';
import OrderPreviewPaymentInfo from '../../page-objects/orderPreviewPaymentInfo';

const PAGE_URL = '/entity/ticket@120397885';
const PAYMENT_INFO_SELECTOR = "//div[text()='Оплата']";
const EXPECTED_PAYMENT_INFO = {
    buyerFullName: 'Кот Матроскин',
    buyerItemsTotal: '15 000,00 ₽',
    deliveryPrice: '549,00 ₽',
    liftPrice: '150,00 ₽',
    buyerTotal: '15 699,00 ₽',
    paymentMethod: 'Наличными при получении',
};

/**
 * Проверить, что:
 * На превью заказа в карточке обращения блок "Оплата" отображается только 1 раз.
 * В блоке отображаются все ожидаемые атрибуты.
 */
describe(`ocrm-705: Отображение блока "Оплата" на превью заказа в обращении`, () => {
    beforeEach(function() {
        return login(PAGE_URL, this);
    });

    it(`На превью заказа корректно отображается блок "Оплата"`, async function() {
        const paymentInfoElement = new OrderPreviewPaymentInfo(this.browser);

        await paymentInfoElement.isDisplayed();

        const paymentInfo = {
            buyerFullName: await (await paymentInfoElement.buyerFullName).getText(),
            buyerItemsTotal: await (await paymentInfoElement.buyerItemsTotal).getText(),
            deliveryPrice: await (await paymentInfoElement.deliveryPrice).getText(),
            liftPrice: await (await paymentInfoElement.liftPrice).getText(),
            buyerTotal: await (await paymentInfoElement.buyerTotal).getText(),
            paymentMethod: await (await paymentInfoElement.paymentMethod).getText(),
        };

        const paymentInfoIsDisplayedOnce = (await this.browser.$$(PAYMENT_INFO_SELECTOR)).length === 1;

        expect(paymentInfoIsDisplayedOnce).to.equal(true, 'Блок "Оплата" отображается больше одного раза');
        expect(paymentInfo).to.deep.equal(EXPECTED_PAYMENT_INFO, 'Значения в блоке "Оплата" отличаются от ожидаемых');
    });
});
