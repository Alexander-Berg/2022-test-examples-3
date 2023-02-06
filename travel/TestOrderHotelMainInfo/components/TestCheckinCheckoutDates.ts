import {Component} from 'components/Component';

export default class TestCheckinCheckoutDates extends Component {
    checkinDate: Component;
    checkoutDate: Component;
    checkinTime: Component;
    checkoutTime: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.checkinDate = new Component(this.browser, {
            parent: this.qa,
            current: 'checkinDate',
        });
        this.checkoutDate = new Component(this.browser, {
            parent: this.qa,
            current: 'checkoutDate',
        });
        this.checkinTime = new Component(this.browser, {
            parent: this.qa,
            current: 'checkinTime',
        });
        this.checkoutTime = new Component(this.browser, {
            parent: this.qa,
            current: 'checkoutTime',
        });
    }
}
