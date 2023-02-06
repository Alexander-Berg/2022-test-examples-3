import {TestLink} from 'components/TestLink';
import {Component} from 'components/Component';

export default class TestOrder extends Component {
    link: TestLink;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.link = new TestLink(this.browser, {
            parent: this.qa,
            current: 'orderLink',
        });
    }
}
