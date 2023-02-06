import {assert} from 'chai';

import {TEST_SUCCESS_PROMOCODE} from 'helpers/constants/hotels/promocodes';

import {IBookOfferRequestParams} from 'helpers/project/hotels/types/IBookOffer';

import getTestOfferParams from './utilities/getTestOfferParams';
import {TestHotelsBookPage} from 'helpers/project/hotels/pages/TestHotelsBookPage/TestHotelsBookPage';
import {TestHotelsPaymentPage} from 'helpers/project/hotels/pages/TestHotelsPaymentPage/TestHotelsPaymentPage';
import {TestHotelsHappyPage} from 'helpers/project/hotels/pages/TestHotelsHappyPage/TestHotelsHappyPage';
import {maleGuests} from 'helpers/project/hotels/data/adultGuests';
import {contacts} from 'helpers/project/hotels/data/contacts';
import {PaymentTestContextHelper} from 'helpers/utilities/paymentTestContext/PaymentTestContextHelper';

interface IBookResult {
    discountedPriceAmount: number;
}

export class TestHotelsBookApp {
    hotelsBookPage: TestHotelsBookPage;
    hotelsPaymentPage: TestHotelsPaymentPage;
    hotelsHappyPage: TestHotelsHappyPage;

    paymentTestContextHelper: PaymentTestContextHelper;

    private readonly browser: WebdriverIO.Browser;

    constructor(browser: WebdriverIO.Browser) {
        this.browser = browser;

        this.hotelsBookPage = new TestHotelsBookPage(this.browser);
        this.hotelsPaymentPage = new TestHotelsPaymentPage(this.browser);
        this.hotelsHappyPage = new TestHotelsHappyPage(this.browser);

        this.paymentTestContextHelper = new PaymentTestContextHelper(
            this.browser,
        );
    }

    bookWithPromoCode(
        offerParams: IBookOfferRequestParams = getTestOfferParams(),
    ): Promise<IBookResult> {
        return this.book(offerParams, TEST_SUCCESS_PROMOCODE.code);
    }

    async book(
        offerParams: IBookOfferRequestParams = getTestOfferParams(),
        promoCode?: string,
    ): Promise<IBookResult> {
        await this.hotelsBookPage.goToPage(offerParams);

        assert(
            await this.hotelsBookPage.bookStatusProvider.isOfferFetched(),
            'Должна открыться страница бронирования отеля',
        );

        await this.hotelsBookPage.priceInfo.deferredPayment.tryApplyFullPayment();

        if (promoCode) {
            await this.hotelsBookPage.priceInfo.promoCodes.checkBox.scrollIntoView();
            await this.hotelsBookPage.priceInfo.promoCodes.testInitialPromoCodesState();
            await this.hotelsBookPage.priceInfo.promoCodes.checkBox.click();
            await this.hotelsBookPage.priceInfo.promoCodes.applyPromoCode(
                promoCode,
            );
            assert.equal(
                await this.hotelsBookPage.priceInfo.promoCodes.discountPrice.getText(),
                '− 200 ₽',
                'В корзинке должна быть указана скидка по промокоду -200₽',
            );
        }

        const discountedPriceAmount =
            await this.hotelsBookPage.priceInfo.totalPrice.getPriceValue();

        await this.hotelsBookPage.bookForm.fillForm(maleGuests, contacts);
        await this.hotelsBookPage.bookForm.submit();
        await this.hotelsBookPage.waitUntilLoaded();

        return {
            discountedPriceAmount,
        };
    }
}
