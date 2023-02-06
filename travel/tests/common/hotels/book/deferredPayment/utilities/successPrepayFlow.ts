import moment from 'moment';
import {assert} from 'chai';

import {MOCK_PAYMENT_URL} from 'helpers/constants/testContext';
import {TEST_SUCCESS_PROMOCODE} from 'helpers/constants/hotels/promocodes';

import {ITestOrderPageData} from '../types/ITestOrderPageData';

import TestOrderHotels from 'helpers/project/account/pages/OrderPage/TestOrderHotels';
import {
    femaleGuests,
    maleGuests,
} from 'helpers/project/hotels/data/adultGuests';
import {contacts} from 'helpers/project/hotels/data/contacts';
import dateFormats from 'helpers/utilities/date/formats';
import {
    TCreateAccountResult,
    TGetAccountResult,
} from 'helpers/project/common/passport/Account';
import {TestHotelsBookApp} from 'helpers/project/hotels/app/TestHotelsBookApp/TestHotelsBookApp';
import getTestOfferParams from 'helpers/project/hotels/app/TestHotelsBookApp/utilities/getTestOfferParams';

export async function successPrepayFlow(
    browser: WebdriverIO.Browser,
    retryCount: number,
    account: TCreateAccountResult | TGetAccountResult,
    orderPage: TestOrderHotels,
): Promise<ITestOrderPageData> {
    const {login, password} = account;

    const app = new TestHotelsBookApp(browser);

    const {hotelsBookPage, hotelsHappyPage} = app;

    const hotelPagePromoCodes = hotelsBookPage.priceInfo.promoCodes;
    const deferredPayment = hotelsBookPage.priceInfo.deferredPayment;

    /* Login and fetch offer */
    await browser.login(login, password);

    const testOfferParams = getTestOfferParams({
        originalId: 100,
        occupancy: '2',
        checkinDate: moment()
            .add(1, 'month')
            .add(5, 'days')
            .format(dateFormats.ROBOT),
        checkoutDate: moment()
            .add(1, 'month')
            .add(6, 'days')
            .format(dateFormats.ROBOT),
        priceAmount: 6000,
    });

    await hotelsBookPage.goToPage(testOfferParams);
    await hotelsBookPage.bookStatusProvider.isOfferFetched();

    await deferredPayment.applyDeferredPayment();

    assert.equal(
        await deferredPayment.postpayRadioboxLabel.getText(),
        'Оплатить позже',
        'Чекбокс рассрочки должен содержать текст "Оплатить позже"',
    );

    /* Apply promoCode */
    await hotelPagePromoCodes.checkBox.scrollIntoView();
    await hotelPagePromoCodes.testInitialPromoCodesState();
    await hotelPagePromoCodes.checkBox.click();
    await hotelPagePromoCodes.testActivePromoCodesState();
    await hotelPagePromoCodes.applyPromoCode(TEST_SUCCESS_PROMOCODE.code);
    await hotelPagePromoCodes.discountPrice.waitForVisible();

    const paymentEndsAtDeferredPayment =
        await deferredPayment.paymentEndsAt.getText();
    const deferredFullPriceAfterApplyPromo =
        await deferredPayment.getFullPrice();

    await app.paymentTestContextHelper.setStartPaymentTestContext({
        minUserActionDelay: app.paymentTestContextHelper.getProgressiveDelay(
            10,
            retryCount,
        ),
        paymentOutcome: 'PO_SUCCESS',
        paymentUrl: MOCK_PAYMENT_URL,
    });

    /* Fill form and submit */
    await hotelsBookPage.bookForm.fillForm(
        [...maleGuests, ...femaleGuests],
        contacts,
    );
    await hotelsBookPage.bookForm.submit();
    await hotelsBookPage.waitUntilLoaded();

    /* HappyPage */
    await hotelsHappyPage.loader.waitUntilLoaded();
    await hotelsHappyPage.orderActions.detailsLink.waitForVisible();
    await hotelsHappyPage.orderActions.detailsLink.click();

    /* OrderPage */
    await orderPage.loader.waitUntilLoaded();

    return {
        deferredFullPriceAfterApplyPromo,
        paymentEndsAtDeferredPayment,
    };
}
