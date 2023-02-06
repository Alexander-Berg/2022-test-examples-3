import {Component} from 'helpers/project/common/components';

export default class TestHotelOrderPageError extends Component {
    title: Component;
    orderId: Component;

    constructor(browser: WebdriverIO.Browser, qa = 'orderError') {
        super(browser, qa);

        this.title = new Component(browser, {
            parent: this.qa,
            current: 'title',
        });

        this.orderId = new Component(browser, {
            parent: this.qa,
            current: 'orderId',
        });
    }
}
