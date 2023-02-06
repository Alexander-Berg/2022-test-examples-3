import {TestHappyPage} from 'components/TestHappyPage';
import {TestHotelsOrderInfo} from './components/TestHotelsOrderInfo/TestHotelsOrderInfo';
import {Component} from 'components/Component';
import {TestOrderHeader} from 'components/TestOrderHeader';
import {TestPrice} from 'components/TestPrice';

const HAPPY_PAGE_SELECTOR = 'happyPage';

export class TestHotelsHappyPage extends TestHappyPage {
    readonly orderInfo: TestHotelsOrderInfo;
    readonly email: Component;
    readonly phone: Component;
    readonly orderHeader: TestOrderHeader;
    readonly nextPaymentPrice: TestPrice;
    readonly paymentEndsAt: Component;
    readonly nextPaymentLink: Component;

    constructor(browser: WebdriverIO.Browser) {
        super(browser, HAPPY_PAGE_SELECTOR);

        this.orderInfo = new TestHotelsOrderInfo(browser, {
            parent: this.qa,
            current: 'orderInfo',
        });

        this.email = new Component(browser, {
            parent: HAPPY_PAGE_SELECTOR,
            current: 'email',
        });

        this.phone = new Component(browser, {
            parent: HAPPY_PAGE_SELECTOR,
            current: 'phone',
        });

        this.orderHeader = new TestOrderHeader(browser);

        this.nextPaymentPrice = new TestPrice(browser, {
            parent: HAPPY_PAGE_SELECTOR,
            current: 'nextPaymentPrice',
        });

        this.paymentEndsAt = new TestPrice(browser, {
            parent: HAPPY_PAGE_SELECTOR,
            current: 'paymentEndsAt',
        });

        this.nextPaymentLink = new TestPrice(browser, {
            parent: HAPPY_PAGE_SELECTOR,
            current: 'nextPaymentLink',
        });
    }
}
