import {Component} from 'components/Component';
import {TestLink} from 'components/TestLink';

export default class TestContacts extends Component {
    email: TestLink;
    phone: Component;
    info: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.email = new TestLink(browser, {parent: this.qa, current: 'email'});
        this.phone = new Component(browser, {
            parent: this.qa,
            current: 'phone',
        });
        this.info = new Component(browser, {parent: this.qa, current: 'info'});
    }
}
