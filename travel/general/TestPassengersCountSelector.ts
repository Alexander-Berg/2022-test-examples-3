import {Component} from 'components/Component';
import {TravellersCount} from 'components/TravellersCount';

export class TestPassengersCountSelector extends Component {
    passengersCount: TravellersCount;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.passengersCount = new TravellersCount(browser, {
            parent: this.qa,
            current: 'control',
        });
    }
}
