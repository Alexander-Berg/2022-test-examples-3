import {TestHappyPage} from 'components/TestHappyPage/TestHappyPage';
import {TestHotelsOrderInfo} from './components/TestHotelsOrderInfo/TestHotelsOrderInfo';
import {Component} from 'components/Component';
import {TestPrice} from 'components/TestPrice';

const HAPPY_PAGE_SELECTOR = 'happyPage';

export class TestHotelsHappyPage extends TestHappyPage {
    readonly orderInfo: TestHotelsOrderInfo;
    readonly nextPaymentPrice: TestPrice;
    readonly paymentEndsAt: Component;
    readonly nextPaymentLink: Component;

    constructor(browser: WebdriverIO.Browser) {
        super(browser, HAPPY_PAGE_SELECTOR);

        this.orderInfo = new TestHotelsOrderInfo(browser, {
            parent: this.qa,
            current: 'orderInfo',
        });

        this.nextPaymentPrice = new TestPrice(browser, {
            parent: HAPPY_PAGE_SELECTOR,
            current: 'nextPaymentPrice',
        });

        this.paymentEndsAt = new Component(browser, {
            parent: HAPPY_PAGE_SELECTOR,
            current: 'paymentEndsAt',
        });

        this.nextPaymentLink = new Component(browser, {
            parent: HAPPY_PAGE_SELECTOR,
            current: 'nextPaymentLink',
        });
    }
}
