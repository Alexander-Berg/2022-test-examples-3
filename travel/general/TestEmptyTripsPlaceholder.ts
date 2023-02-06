import {Component} from 'components/Component';
import {TestLink} from 'components/TestLink';

export default class TestEmptyTripsPlaceholder extends Component {
    readonly illustration: Component;
    readonly noOrdersButton: TestLink;
    readonly noOrdersDescription: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.illustration = new Component(browser, {
            parent: this.qa,
            current: 'illustration',
        });

        this.noOrdersButton = new TestLink(browser, {
            parent: this.qa,
            current: 'noOrdersButton',
        });

        this.noOrdersDescription = new Component(browser, {
            parent: this.qa,
            current: 'noOrdersDescription',
        });
    }
}
