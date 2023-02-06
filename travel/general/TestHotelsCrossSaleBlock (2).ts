import {Component} from 'components/Component';

export default class TestHotelsCrossSaleBlock extends Component {
    title: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.title = new Component(this.browser, {
            parent: this.qa,
            current: 'title',
        });
    }
}
