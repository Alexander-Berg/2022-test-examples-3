import {Component} from 'components/Component';

export class TestStation extends Component {
    city: Component;
    station: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.city = new Component(browser, {
            parent: this.qa,
            current: 'city',
        });
        this.station = new Component(browser, {
            parent: this.qa,
            current: 'station',
        });
    }
}
