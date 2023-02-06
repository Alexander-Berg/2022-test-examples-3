import {Component} from 'components/Component';

export default class TestOrderHotelGuest extends Component {
    name: Component;
    isMain: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.name = new Component(this.browser, {
            parent: this.qa,
            current: 'name',
        });
        this.isMain = new Component(this.browser, {
            parent: this.qa,
            current: 'isMain',
        });
    }
}
