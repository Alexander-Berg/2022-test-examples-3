import {Component} from 'helpers/project/common/components';

export class TestHotelTransportAccessibility extends Component {
    distance: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.distance = new Component(browser, {
            parent: this.qa,
            current: 'distance',
        });
    }
}
