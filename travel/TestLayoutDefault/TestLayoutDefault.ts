import {Component} from 'components/Component';
import {TestFooter} from 'components/TestFooter';
import {TestHeader} from 'components/TestHeader';

export default class TestLayoutDefault extends Component {
    footer: TestFooter;
    header: TestHeader;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.footer = new TestFooter(this.browser, {
            parent: this.qa,
            current: 'portalFooter',
        });

        this.header = new TestHeader(this.browser, {
            parent: this.qa,
            current: 'portalHeader',
        });
    }
}
