import {Component} from 'components/Component';
import {TestOrderActions} from 'components/TestOrderActions';

export default class TestOrderHotelsActions extends Component {
    actions: TestOrderActions;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.actions = new TestOrderActions(this.browser, {
            parent: this.qa,
            current: 'actions',
        });
    }
}
