import {Component} from 'components/Component';
import {TestPrice} from 'components/TestPrice';

export class TestHotelsPriceInfo extends Component {
    totalPrice: TestPrice;

    promoCodeInfo: TestPrice;

    promoCodeValue: Component;

    constructor(browser: WebdriverIO.Browser, qa: string) {
        super(browser, qa);

        this.totalPrice = new TestPrice(browser, {
            parent: qa,
            current: 'totalPrice',
        });

        this.promoCodeInfo = new TestPrice(browser, {
            parent: qa,
            current: 'promoCodeInfo',
        });

        this.promoCodeValue = new Component(browser, {
            parent: qa,
            current: 'promoCodeValue',
        });
    }
}
