import {Component} from 'components/Component';

export default class TestOrderHotelsError extends Component {
    title: Component;
    orderId: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.title = new Component(this.browser, {
            parent: this.qa,
            current: 'title',
        });
        this.orderId = new Component(this.browser, {
            parent: this.qa,
            current: 'orderId',
        });
    }
}
