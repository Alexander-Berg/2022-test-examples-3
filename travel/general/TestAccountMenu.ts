import {Component} from 'components/Component';
import TestTab from 'components/TestTabs/components/TestTab';

export default class TestAccountMenu extends Component {
    readonly tripsTab: TestTab;
    readonly ordersTab: TestTab;
    readonly passengersTab: TestTab;
    readonly supportPhone: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.tripsTab = new TestTab(this.browser, {
            parent: this.qa,
            current: 'tripsTab',
        });
        this.ordersTab = new TestTab(this.browser, {
            parent: this.qa,
            current: 'ordersTab',
        });
        this.passengersTab = new TestTab(this.browser, {
            parent: this.qa,
            current: 'passengersTab',
        });

        this.supportPhone = new Component(this.browser, {
            parent: this.qa,
            current: 'supportPhone',
        });
    }
}
