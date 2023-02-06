import {Component} from 'components/Component';
import {Button} from 'components/Button';
import {TestLink} from 'components/TestLink';

export default class TestDescriptionAndActions extends Component {
    readonly busDescription: Component;
    readonly carrierName: Component;
    readonly downloadButton: TestLink;
    readonly printButton: Button;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.busDescription = new Component(this.browser, {
            parent: this.qa,
            current: 'busDescription',
        });
        this.carrierName = new Component(this.browser, {
            parent: this.qa,
            current: 'carrierName',
        });
        this.downloadButton = new TestLink(this.browser, {
            parent: this.qa,
            current: 'downloadButton',
        });
        this.printButton = new Button(this.browser, {
            parent: this.qa,
            current: 'printButton',
        });
    }
}
